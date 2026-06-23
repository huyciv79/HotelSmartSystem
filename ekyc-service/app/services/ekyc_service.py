"""CCCD field detection with YOLOv11 and Vietnamese text recognition with VietOCR."""

from __future__ import annotations

import logging
import os
import re
import threading
import unicodedata
from datetime import datetime
from pathlib import Path
from typing import Any, Optional

# Keep library-generated config/model caches inside the service workspace.
_SERVICE_ROOT = Path(__file__).resolve().parents[2]
_CACHE_ROOT = _SERVICE_ROOT / ".cache"
(_CACHE_ROOT / "ultralytics" / "Ultralytics").mkdir(parents=True, exist_ok=True)
(_CACHE_ROOT / "huggingface").mkdir(parents=True, exist_ok=True)
(_CACHE_ROOT / "matplotlib").mkdir(parents=True, exist_ok=True)
os.environ.setdefault(
    "YOLO_CONFIG_DIR",
    str(_SERVICE_ROOT / ".cache" / "ultralytics"),
)
os.environ.setdefault(
    "HF_HOME",
    str(_SERVICE_ROOT / ".cache" / "huggingface"),
)
os.environ.setdefault(
    "MPLCONFIGDIR",
    str(_SERVICE_ROOT / ".cache" / "matplotlib"),
)

import cv2
import numpy as np
import requests
import torch
from huggingface_hub import hf_hub_download
from PIL import Image
from ultralytics import YOLO
from vietocr.tool.config import Cfg
from vietocr.tool.predictor import Predictor

from app.core.config import settings

logger = logging.getLogger(__name__)

_yolo_model: YOLO | None = None
_ocr_predictor: Predictor | None = None
_model_init_lock = threading.Lock()
_ocr_predict_lock = threading.Lock()

_FIELD_ALIASES = {
    "id_number": "id_number",
    "idnumber": "id_number",
    "id": "id_number",
    "cccd": "id_number",
    "identity_number": "id_number",
    "full_name": "full_name",
    "fullname": "full_name",
    "name": "full_name",
    "ho_ten": "full_name",
    "dob": "dob",
    "date_of_birth": "dob",
    "birth_date": "dob",
    "birthday": "dob",
}
_CLASS_ID_FALLBACK = {
    0: "id_number",
    1: "full_name",
    2: "dob",
}

_OCR_DIGIT_TRANSLATION = str.maketrans(
    {
        "O": "0",
        "Q": "0",
        "D": "0",
        "I": "1",
        "L": "1",
        "|": "1",
        "Z": "2",
        "S": "5",
        "G": "6",
        "B": "8",
    }
)


def _resolve_device() -> str:
    configured = settings.OCR_DEVICE.strip().lower()
    if configured == "auto":
        return "cuda:0" if torch.cuda.is_available() else "cpu"
    return configured


def _ensure_vietocr_weights() -> Path:
    weights_dir = _CACHE_ROOT / "vietocr"
    weights_dir.mkdir(parents=True, exist_ok=True)
    weights_path = weights_dir / "vgg_transformer.pth"
    if weights_path.exists() and weights_path.stat().st_size > 0:
        return weights_path

    partial_path = weights_path.with_suffix(".pth.part")
    logger.info("Đang tải weight VietOCR từ %s", settings.VIETOCR_WEIGHTS_URL)
    try:
        with requests.get(
            settings.VIETOCR_WEIGHTS_URL,
            stream=True,
            timeout=120,
        ) as response:
            response.raise_for_status()
            with partial_path.open("wb") as output:
                for chunk in response.iter_content(chunk_size=1024 * 1024):
                    if chunk:
                        output.write(chunk)
        partial_path.replace(weights_path)
        return weights_path
    except Exception:
        if partial_path.exists():
            partial_path.unlink()
        raise


def warm_ocr_models() -> None:
    """Download/cache YOLO weights and initialize YOLO + VietOCR once."""
    global _yolo_model, _ocr_predictor

    if _yolo_model is not None and _ocr_predictor is not None:
        return

    with _model_init_lock:
        if _yolo_model is None:
            logger.info(
                "Đang tải model YOLO CCCD từ Hugging Face repo=%s, file=%s",
                settings.HF_REPO_ID,
                settings.HF_MODEL_FILE,
            )
            model_path = hf_hub_download(
                repo_id=settings.HF_REPO_ID,
                filename=settings.HF_MODEL_FILE,
                revision=settings.HF_REVISION or None,
                token=settings.HF_TOKEN or None,
            )
            _yolo_model = YOLO(model_path)

        if _ocr_predictor is None:
            device = _resolve_device()
            logger.info(
                "Đang khởi tạo VietOCR config=%s trên device=%s",
                settings.VIETOCR_CONFIG,
                device,
            )
            config_path = Path(settings.VIETOCR_CONFIG_PATH)
            if not config_path.is_absolute():
                config_path = _SERVICE_ROOT / config_path
            if not config_path.exists():
                raise FileNotFoundError(
                    f"Không tìm thấy cấu hình VietOCR local: {config_path}"
                )

            weights_path = _ensure_vietocr_weights()
            config = Cfg.load_config_from_file(str(config_path))
            config["device"] = device
            config["weights"] = str(weights_path)
            if isinstance(config.get("cnn"), dict):
                config["cnn"]["pretrained"] = False
            if isinstance(config.get("predictor"), dict):
                config["predictor"]["beamsearch"] = False
            _ocr_predictor = Predictor(config)


def _normalize_class_name(value: str) -> str:
    normalized = re.sub(r"[^a-z0-9]+", "_", value.strip().lower()).strip("_")
    return _FIELD_ALIASES.get(normalized, normalized)


def _class_name(names: dict[int, str] | list[str], class_id: int) -> str:
    if isinstance(names, dict):
        return str(names.get(class_id, class_id))
    if 0 <= class_id < len(names):
        return str(names[class_id])
    return str(class_id)


def enhance_for_ocr(crop_bgr: np.ndarray) -> Image.Image:
    """Preprocess a field crop with the sharpening pipeline tested on Colab."""
    if crop_bgr.size == 0:
        raise ValueError("Vùng thông tin CCCD sau khi cắt bị rỗng.")

    gray = cv2.cvtColor(crop_bgr, cv2.COLOR_BGR2GRAY)
    height, width = gray.shape[:2]
    if height <= 0 or width <= 0:
        raise ValueError("Kích thước vùng thông tin CCCD không hợp lệ.")

    target_height = max(32, int(settings.OCR_CROP_HEIGHT))
    target_width = max(1, int(width * (target_height / height)))
    resized = cv2.resize(
        gray,
        (target_width, target_height),
        interpolation=cv2.INTER_LINEAR,
    )

    sharpen_kernel = np.array(
        [
            [-1, -1, -1],
            [-1, 9, -1],
            [-1, -1, -1],
        ],
        dtype=np.float32,
    )
    sharpened = cv2.filter2D(resized, -1, sharpen_kernel)
    normalized = cv2.normalize(sharpened, None, 0, 255, cv2.NORM_MINMAX)
    return Image.fromarray(normalized)


def _predict_text(image: Image.Image) -> str:
    warm_ocr_models()
    if _ocr_predictor is None:
        raise RuntimeError("VietOCR chưa được khởi tạo.")
    with _ocr_predict_lock:
        return str(_ocr_predictor.predict(image)).strip()


def _clean_id_number(raw_text: str) -> Optional[str]:
    normalized = raw_text.upper().translate(_OCR_DIGIT_TRANSLATION)
    digits = re.sub(r"\D", "", normalized)
    if len(digits) == 12:
        return digits
    if len(digits) == 9:
        return digits

    match = re.search(r"\d{12}", digits)
    if match:
        return match.group(0)
    match = re.search(r"\d{9}", digits)
    return match.group(0) if match else None


def _clean_full_name(raw_text: str) -> Optional[str]:
    normalized = unicodedata.normalize("NFC", raw_text).upper()
    letters_only = "".join(
        character if character.isalpha() or character.isspace() else " "
        for character in normalized
    )
    value = " ".join(letters_only.split())
    return value or None


def _clean_date_of_birth(raw_text: str) -> Optional[str]:
    normalized = raw_text.upper().translate(_OCR_DIGIT_TRANSLATION)
    groups = re.findall(r"\d+", normalized)

    candidate: str | None = None
    if len(groups) >= 3:
        day, month, year = groups[0], groups[1], groups[2]
        if len(year) == 2:
            year = "19" + year
        candidate = f"{day.zfill(2)}/{month.zfill(2)}/{year}"
    else:
        digits = "".join(groups)
        if len(digits) == 8:
            candidate = f"{digits[:2]}/{digits[2:4]}/{digits[4:]}"

    if candidate is None:
        return None

    try:
        return datetime.strptime(candidate, "%d/%m/%Y").strftime("%d/%m/%Y")
    except ValueError:
        return None


def _post_process(field_name: str, raw_text: str) -> Optional[str]:
    if field_name == "id_number":
        return _clean_id_number(raw_text)
    if field_name == "full_name":
        return _clean_full_name(raw_text)
    if field_name == "dob":
        return _clean_date_of_birth(raw_text)
    return raw_text.strip() or None


def _detect_best_field_boxes(front_img: np.ndarray) -> dict[str, dict[str, Any]]:
    warm_ocr_models()
    if _yolo_model is None:
        raise RuntimeError("YOLO CCCD chưa được khởi tạo.")

    front_bgr = cv2.cvtColor(front_img, cv2.COLOR_RGB2BGR)
    results = _yolo_model.predict(
        source=front_bgr,
        imgsz=int(settings.CCCD_YOLO_IMAGE_SIZE),
        conf=float(settings.CCCD_YOLO_CONFIDENCE),
        iou=float(settings.CCCD_YOLO_IOU),
        agnostic_nms=bool(settings.CCCD_YOLO_AGNOSTIC_NMS),
        device=_resolve_device(),
        verbose=False,
    )
    if not results:
        return {}

    result = results[0]
    best_boxes: dict[str, dict[str, Any]] = {}

    if result.boxes is None:
        return best_boxes

    for box in result.boxes:
        class_id = int(box.cls[0].item())
        field_name = _CLASS_ID_FALLBACK.get(class_id)
        if field_name is None:
            logger.warning("Bỏ qua class YOLO ngoài phạm vi: id=%s", class_id)
            continue

        confidence = float(box.conf[0].item())
        current = best_boxes.get(field_name)
        if current is None or confidence > current["confidence"]:
            best_boxes[field_name] = {
                "xyxy": box.xyxy[0].detach().cpu().numpy(),
                "confidence": confidence,
            }

    return best_boxes


def extract_id_card_details(
    front_img: np.ndarray,
) -> tuple[Optional[str], Optional[str], Optional[str]]:
    """Detect id number, full name and DOB regions, then recognize with VietOCR."""
    best_boxes = _detect_best_field_boxes(front_img)
    if not best_boxes:
        raise ValueError(
            "Không phát hiện được vùng số CCCD, họ tên hoặc ngày sinh trên ảnh mặt trước."
        )

    front_bgr = cv2.cvtColor(front_img, cv2.COLOR_RGB2BGR)
    image_height, image_width = front_bgr.shape[:2]
    extracted: dict[str, Optional[str]] = {
        "id_number": None,
        "full_name": None,
        "dob": None,
    }

    for field_name, detection in best_boxes.items():
        x1, y1, x2, y2 = [
            int(round(value))
            for value in detection["xyxy"]
        ]
        padding = max(0, int(settings.OCR_PADDING_PIXELS))

        crop = front_bgr[
            max(0, y1 - padding):min(image_height, y2 + padding),
            max(0, x1 - padding):min(image_width, x2 + padding),
        ]
        processed = enhance_for_ocr(crop)
        raw_text = _predict_text(processed)
        cleaned_text = _post_process(field_name, raw_text)
        extracted[field_name] = cleaned_text

        logger.info(
            "CCCD field=%s, detection_confidence=%.4f, raw=%r, cleaned=%r",
            field_name,
            detection["confidence"],
            raw_text,
            cleaned_text,
        )

    return (
        extracted["id_number"],
        extracted["full_name"],
        extracted["dob"],
    )
