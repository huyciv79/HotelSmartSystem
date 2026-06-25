"""CCCD field detection with YOLOv11 and Vietnamese text recognition with VietOCR."""

from __future__ import annotations

import logging
import os
import re
import threading
import unicodedata
from datetime import datetime
from functools import lru_cache
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


def _cache_resource(func):
    try:
        import streamlit as st

        return st.cache_resource(show_spinner=False)(func)
    except Exception:
        return lru_cache(maxsize=8)(func)

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
_CCCD_DETECTABLE_FIELD_NAMES = ("id_number", "full_name", "dob")
_CCCD_REQUIRED_FIELD_NAMES = ("id_number", "full_name", "dob")
_NULL_TEXT_TOKENS = {"", "-", "--", "---", "—", "–", "_", "null", "none", "nan", "n/a", "na"}

CCCD_PROVINCES = {
    "001": "Hà Nội",
    "002": "Hà Giang",
    "004": "Cao Bằng",
    "006": "Bắc Kạn",
    "008": "Tuyên Quang",
    "010": "Lào Cai",
    "011": "Điện Biên",
    "012": "Lai Châu",
    "014": "Sơn La",
    "015": "Yên Bái",
    "017": "Hòa Bình",
    "019": "Thái Nguyên",
    "020": "Lạng Sơn",
    "022": "Quảng Ninh",
    "024": "Bắc Giang",
    "025": "Phú Thọ",
    "026": "Vĩnh Phúc",
    "027": "Bắc Ninh",
    "030": "Hải Dương",
    "031": "Hải Phòng",
    "033": "Hưng Yên",
    "034": "Thái Bình",
    "035": "Hà Nam",
    "036": "Nam Định",
    "037": "Ninh Bình",
    "038": "Thanh Hóa",
    "040": "Nghệ An",
    "042": "Hà Tĩnh",
    "044": "Quảng Bình",
    "045": "Quảng Trị",
    "046": "Thừa Thiên Huế",
    "048": "Đà Nẵng",
    "049": "Quảng Nam",
    "051": "Quảng Ngãi",
    "052": "Bình Định",
    "054": "Phú Yên",
    "056": "Khánh Hòa",
    "058": "Ninh Thuận",
    "060": "Bình Thuận",
    "062": "Kon Tum",
    "064": "Gia Lai",
    "066": "Đắk Lắk",
    "067": "Đắk Nông",
    "068": "Lâm Đồng",
    "070": "Bình Phước",
    "072": "Tây Ninh",
    "074": "Bình Dương",
    "075": "Đồng Nai",
    "077": "Bà Rịa - Vũng Tàu",
    "079": "Hồ Chí Minh",
    "080": "Long An",
    "082": "Tiền Giang",
    "083": "Bến Tre",
    "084": "Trà Vinh",
    "086": "Vĩnh Long",
    "087": "Đồng Tháp",
    "089": "An Giang",
    "091": "Kiên Giang",
    "092": "Cần Thơ",
    "093": "Hậu Giang",
    "094": "Sóc Trăng",
    "095": "Bạc Liêu",
    "096": "Cà Mau",
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


def _strip_accents(value: str) -> str:
    decomposed = unicodedata.normalize("NFD", value)
    without_marks = "".join(
        character
        for character in decomposed
        if unicodedata.category(character) != "Mn"
    )
    return without_marks.replace("đ", "d").replace("Đ", "D")


def _normalize_lookup_text(value: str) -> str:
    value = _strip_accents(value).lower()
    return re.sub(r"[^a-z0-9]+", " ", value).strip()


def _nullable_text(value: Any) -> Optional[str]:
    if value is None:
        return None

    text = " ".join(str(value).strip().split())
    if text in _NULL_TEXT_TOKENS:
        return None

    normalized = _normalize_lookup_text(text)
    if normalized in {"", "null", "none", "nan", "n a", "na"}:
        return None

    if all(character in "-–—_/.,:;|\\ " for character in text):
        return None

    return text


def _resolve_device() -> str:
    configured = settings.OCR_DEVICE.strip().lower()
    if configured == "auto":
        return "cuda:0" if torch.cuda.is_available() else "cpu"
    return configured


def _resolve_yolo_device() -> str:
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


def _model_revision(*values: Optional[str]) -> Optional[str]:
    for value in values:
        if value and value.strip():
            return value.strip()
    return None


def _download_hf_file(repo_id: str, filename: str, revision: Optional[str]) -> Path:
    return Path(
        hf_hub_download(
            repo_id=repo_id,
            filename=filename,
            revision=revision,
            token=settings.HF_TOKEN or None,
        )
    )


def _ensure_vietocr_weights() -> Path:
    repo_id = settings.VIETOCR_REPO_ID or settings.HF_REPO_ID
    revision = _model_revision(
        settings.VIETOCR_REVISION,
        settings.OCR_REVISION,
        settings.TROCR_REVISION,
        settings.HF_REVISION,
    )
    logger.info(
        "Loading VietOCR fine-tuned weights from Hugging Face repo=%s, file=%s",
        repo_id,
        settings.VIETOCR_WEIGHTS_FILE,
    )
    try:
        return _download_hf_file(repo_id, settings.VIETOCR_WEIGHTS_FILE, revision)
    except Exception:
        if not settings.VIETOCR_WEIGHTS_URL:
            raise

        weights_dir = _CACHE_ROOT / "vietocr"
        weights_dir.mkdir(parents=True, exist_ok=True)
        weights_path = weights_dir / Path(settings.VIETOCR_WEIGHTS_FILE).name
        if weights_path.exists() and weights_path.stat().st_size > 0:
            return weights_path

        partial_path = weights_path.with_suffix(weights_path.suffix + ".part")
        logger.info("Fallback download VietOCR weights from %s", settings.VIETOCR_WEIGHTS_URL)
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


def _ensure_yolo_model_file() -> Path:
    revision = _model_revision(settings.OCR_REVISION, settings.HF_REVISION)
    logger.info(
        "Loading YOLOv11 CCCD model from Hugging Face repo=%s, file=%s",
        settings.HF_REPO_ID,
        settings.HF_ONNX_MODEL_FILE,
    )
    try:
        return _download_hf_file(settings.HF_REPO_ID, settings.HF_ONNX_MODEL_FILE, revision)
    except Exception as exc:
        raise RuntimeError(
            "Khong tai duoc YOLO model tu Hugging Face. Hay upload file "
            f"{settings.HF_ONNX_MODEL_FILE} vao repo {settings.HF_REPO_ID}."
        ) from exc


@_cache_resource
def _load_yolo_model(model_path: str) -> YOLO:
    return YOLO(model_path)


@_cache_resource
def _load_vietocr_predictor(config_path: str, weights_path: str, device: str) -> Predictor:
    config = Cfg.load_config_from_file(config_path)
    config["device"] = device
    config["weights"] = weights_path
    if isinstance(config.get("cnn"), dict):
        config["cnn"]["pretrained"] = False
    if isinstance(config.get("predictor"), dict):
        config["predictor"]["beamsearch"] = False
    return Predictor(config)


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
                settings.HF_ONNX_MODEL_FILE,
            )
            model_path = _ensure_yolo_model_file()
            _yolo_model = _load_yolo_model(str(model_path))

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
            _ocr_predictor = _load_vietocr_predictor(
                str(config_path),
                str(weights_path),
                device,
            )


def _normalize_class_name(value: str) -> str:
    normalized = _strip_accents(value)
    normalized = re.sub(r"[^a-z0-9]+", "_", normalized.strip().lower()).strip("_")
    return _FIELD_ALIASES.get(normalized, normalized)


def _class_name(names: dict[int, str] | list[str], class_id: int) -> str:
    if isinstance(names, dict):
        return str(names.get(class_id, class_id))
    if 0 <= class_id < len(names):
        return str(names[class_id])
    return str(class_id)


def enhance_for_ocr(crop_bgr: np.ndarray) -> Image.Image:
    """Convert a YOLO field crop to the RGB PIL format expected by VietOCR."""
    if crop_bgr.size == 0:
        raise ValueError("Vùng thông tin CCCD sau khi cắt bị rỗng.")

    height, width = crop_bgr.shape[:2]
    if height <= 0 or width <= 0:
        raise ValueError("Kích thước vùng thông tin CCCD không hợp lệ.")

    target_height = int(settings.OCR_CROP_HEIGHT)
    if target_height > 0 and height != target_height:
        target_width = max(1, int(width * (target_height / height)))
        crop_bgr = cv2.resize(
            crop_bgr,
            (target_width, target_height),
            interpolation=cv2.INTER_CUBIC,
        )

    crop_rgb = cv2.cvtColor(crop_bgr, cv2.COLOR_BGR2RGB)
    return Image.fromarray(crop_rgb)


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

    match = re.search(r"\d{12}", digits)
    if match:
        return match.group(0)
    return None


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


def _clean_gender(raw_text: str) -> Optional[str]:
    normalized = _normalize_lookup_text(raw_text)
    if "nu" in normalized.split() or normalized == "nu":
        return "Nữ"
    if "nam" in normalized.split() or normalized == "nam":
        return "Nam"
    return None


def _logic_gender_from_id(id_number: str) -> Optional[str]:
    if not re.fullmatch(r"\d{12}", id_number):
        return None
    return "Nam" if int(id_number[3]) % 2 == 0 else "Nữ"


def _logic_year_from_id(id_number: str) -> Optional[int]:
    if not re.fullmatch(r"\d{12}", id_number):
        return None
    gender_digit = int(id_number[3])
    return 1900 + (gender_digit // 2) * 100 + int(id_number[4:6])


def _infer_birth_year_from_id(id_number: Optional[str]) -> Optional[str]:
    if id_number is None:
        return None

    logic_year = _logic_year_from_id(id_number)
    return str(logic_year) if logic_year is not None else None


def _align_dob_year_with_id(dob: Optional[str], id_number: Optional[str]) -> Optional[str]:
    if dob is None or id_number is None:
        return dob

    logic_year = _logic_year_from_id(id_number)
    if logic_year is None:
        return dob

    try:
        parsed = datetime.strptime(dob, "%d/%m/%Y")
    except ValueError:
        return dob

    if parsed.year == logic_year:
        return dob

    if parsed.year % 100 != logic_year % 100:
        return dob

    try:
        return parsed.replace(year=logic_year).strftime("%d/%m/%Y")
    except ValueError:
        return dob


def _post_process(field_name: str, raw_text: str) -> Optional[str]:
    raw_text = _nullable_text(raw_text)
    if raw_text is None:
        return None

    if field_name == "id_number":
        return _clean_id_number(raw_text)
    if field_name == "full_name":
        return _clean_full_name(raw_text)
    if field_name == "dob":
        return _clean_date_of_birth(raw_text)
    if field_name == "gender":
        return _clean_gender(raw_text)
    return _nullable_text(raw_text)


def _validate_cccd_logic(extracted: dict[str, Optional[str]]) -> dict[str, Any]:
    id_number = extracted.get("id_number") or ""
    gender = extracted.get("gender")
    dob = extracted.get("dob")
    errors: list[str] = []
    warnings: list[str] = []
    corrected_fields: dict[str, str] = {}
    logic_gender: str | None = None
    logic_year: int | None = None
    province_code: str | None = None
    province_name: str | None = None

    if not re.fullmatch(r"\d{12}", id_number):
        errors.append("Số CCCD phải có đúng 12 chữ số.")
        return {
            "is_valid": False,
            "logic_gender": logic_gender,
            "logic_year": logic_year,
            "province_code": province_code,
            "province_name": province_name,
            "errors": errors,
            "warnings": warnings,
            "corrected_fields": corrected_fields,
        }

    province_code = id_number[:3]
    province_name = CCCD_PROVINCES.get(province_code)
    logic_gender = _logic_gender_from_id(id_number)
    logic_year = _logic_year_from_id(id_number)

    if (
        gender is not None
        and logic_gender is not None
        and _normalize_lookup_text(gender) != _normalize_lookup_text(logic_gender)
    ):
        errors.append("Mã giới tính trong số CCCD không khớp với trường giới tính.")

    if dob is not None:
        try:
            dob_year = datetime.strptime(dob, "%d/%m/%Y").year
            if logic_year != dob_year:
                errors.append("Năm sinh mã hóa trong số CCCD không khớp với ngày sinh.")
        except ValueError:
            errors.append("Ngày sinh không đúng định dạng dd/mm/yyyy.")

    # Quê quán không được kiểm tra trong luồng eKYC.

    return {
        "is_valid": not errors,
        "logic_gender": logic_gender,
        "logic_year": logic_year,
        "province_code": province_code,
        "province_name": province_name,
        "errors": errors,
        "warnings": warnings,
        "corrected_fields": corrected_fields,
    }


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
        device=_resolve_yolo_device(),
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
        raw_class_name = _class_name(result.names, class_id)
        field_name = _normalize_class_name(raw_class_name)
        if field_name not in _CCCD_DETECTABLE_FIELD_NAMES:
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
                "class_id": class_id,
                "class_name": raw_class_name,
            }

    return best_boxes


def _detect_best_field_boxes_with_autoflip(
    front_img: np.ndarray,
) -> tuple[dict[str, dict], bool]:
    """
    Try YOLO detection on the original image first.
    If fewer than 3 core fields are found,
    flip the image horizontally and retry — backend safety net for
    mirrored images coming from laptop front cameras.

    Returns: (best_boxes_dict, was_flipped)
    """
    boxes_original = _detect_best_field_boxes(front_img)

    required = set(_CCCD_REQUIRED_FIELD_NAMES)
    found_required = required.intersection(boxes_original.keys())

    if len(found_required) >= 3:
        # Enough fields detected on original — no flip needed
        return boxes_original, False

    # Try horizontally flipped image
    flipped_img = cv2.flip(front_img, 1)
    boxes_flipped = _detect_best_field_boxes(flipped_img)
    found_required_flipped = required.intersection(boxes_flipped.keys())

    if len(boxes_flipped) > len(boxes_original) or len(found_required_flipped) > len(found_required):
        logger.info(
            "Auto-flip: original found %d fields (%s), flipped found %d fields (%s). Using flipped.",
            len(boxes_original),
            list(boxes_original.keys()),
            len(boxes_flipped),
            list(boxes_flipped.keys()),
        )
        return boxes_flipped, True

    logger.info(
        "Auto-flip: flip did not improve detection (%d vs %d fields). Using original.",
        len(boxes_flipped),
        len(boxes_original),
    )
    return boxes_original, False


def _extract_id_card_details_legacy(
    front_img: np.ndarray,
) -> tuple[Optional[str], Optional[str], Optional[str]]:
    """Detect id number, full name and DOB regions, then recognize with VietOCR."""
    best_boxes = _detect_best_field_boxes(front_img)
    if not best_boxes:
        raise ValueError(
            "Khong phat hien duoc vung so CCCD, ho ten hoac ngay sinh tren anh mat truoc."
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


def _format_extraction_result(
    extracted: dict[str, Optional[str]],
    validation: dict[str, Any],
    raw_fields: dict[str, Optional[str]],
    detection_confidences: dict[str, float],
) -> dict[str, Any]:
    corrected_fields = validation.get("corrected_fields") or {}
    inferred_hometown = _nullable_text(validation.get("province_name"))
    inferred_gender = _nullable_text(extracted.get("gender")) or _nullable_text(
        validation.get("logic_gender")
    )

    return {
        "id_card_number": _nullable_text(extracted.get("id_number")),
        "full_name": _nullable_text(extracted.get("full_name")),
        "date_of_birth": _nullable_text(extracted.get("dob")),
        "gender": inferred_gender,
        "hometown": inferred_hometown,
        "validation_passed": bool(validation["is_valid"]),
        "validation_errors": validation["errors"],
        "validation_warnings": validation["warnings"],
        "logic_gender": _nullable_text(validation.get("logic_gender")),
        "logic_birth_year": validation["logic_year"],
        "province_code": _nullable_text(validation.get("province_code")),
        "province_name": _nullable_text(validation.get("province_name")),
        "corrected_fields": corrected_fields,
        "raw_fields": raw_fields,
        "detection_confidences": detection_confidences,
    }


def extract_id_card_details(front_img: np.ndarray) -> dict[str, Any]:
    """Detect/OCR CCCD id, full name and DOB, then infer gender and province from the ID."""
    extracted: dict[str, Optional[str]] = {
        "id_number": None,
        "full_name": None,
        "dob": None,
        "gender": None,
        "hometown": None,
    }
    raw_fields: dict[str, Optional[str]] = {
        field: None for field in _CCCD_DETECTABLE_FIELD_NAMES
    }
    detection_confidences: dict[str, float] = {}

    best_boxes = _detect_best_field_boxes_with_autoflip(front_img)
    was_flipped = best_boxes[1]
    best_boxes = best_boxes[0]
    if not best_boxes:
        validation = _validate_cccd_logic(extracted)
        validation["errors"].append(
            "Khong phat hien duoc cac vung thong tin CCCD tren anh mat truoc."
        )
        validation["is_valid"] = False
        return _format_extraction_result(
            extracted,
            validation,
            raw_fields,
            detection_confidences,
        )

    # If we flipped the image, use the flipped version for cropping
    work_img = cv2.flip(front_img, 1) if was_flipped else front_img
    front_bgr = cv2.cvtColor(work_img, cv2.COLOR_RGB2BGR)
    image_height, image_width = front_bgr.shape[:2]

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
        raw_fields[field_name] = _nullable_text(raw_text)
        detection_confidences[field_name] = round(float(detection["confidence"]), 6)

        logger.info(
            "CCCD field=%s, class=%s, detection_confidence=%.4f, raw=%r, cleaned=%r",
            field_name,
            detection.get("class_name"),
            detection["confidence"],
            raw_text,
            cleaned_text,
        )

    extracted["dob"] = _align_dob_year_with_id(
        extracted.get("dob"),
        extracted.get("id_number"),
    )
    dob_inferred_from_id = False
    inferred_birth_year = _infer_birth_year_from_id(extracted.get("id_number"))
    if not extracted.get("dob") and inferred_birth_year:
        extracted["dob"] = f"01/01/{inferred_birth_year}"
        dob_inferred_from_id = True
        logger.info(
            "CCCD dob OCR missing; inferred birth year from id_number=%s",
            inferred_birth_year,
        )

    extracted["gender"] = extracted.get("gender") or _logic_gender_from_id(
        extracted.get("id_number") or ""
    )
    validation = _validate_cccd_logic(extracted)

    if dob_inferred_from_id and inferred_birth_year:
        extracted["dob"] = inferred_birth_year
        validation["warnings"].append(
            "Khong doc duoc ngay sinh bang OCR; da suy ra nam sinh tu so CCCD."
        )
        validation["corrected_fields"]["dob"] = inferred_birth_year

    missing_fields = [
        field
        for field in _CCCD_REQUIRED_FIELD_NAMES
        if not extracted.get(field)
    ]
    if missing_fields:
        validation["errors"].extend(
            f"Khong doc duoc truong bat buoc: {field}."
            for field in missing_fields
        )
        validation["is_valid"] = False

    return _format_extraction_result(
        extracted,
        validation,
        raw_fields,
        detection_confidences,
    )
