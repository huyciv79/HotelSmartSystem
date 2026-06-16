"""
eKYC Service – Business Logic Layer
=====================================
Chứa toàn bộ logic xử lý:
  1. OCR bóc tách Số CCCD từ ảnh mặt trước CCCD (EasyOCR)
  2. Face Embedding Extraction (DeepFace.represent – 512 chiều)

Contract với Spring Boot (POST /api/v1/ai/verify-ekyc):
  - Input : { front_image_url, back_image_url, face_image_url }
  - Output: { id_card_number: str | null, embedding: [512 float] }
"""

import logging
import os
import re
import tempfile
from typing import Dict, Any, Optional

import numpy as np
import easyocr
from deepface import DeepFace

from app.core.config import settings
from app.utils.image_utils import download_image

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Khởi tạo EasyOCR reader (singleton – tốn nhiều RAM nếu khởi tạo lại)
# ---------------------------------------------------------------------------
_ocr_reader: easyocr.Reader | None = None


def _get_ocr_reader() -> easyocr.Reader:
    """Trả về instance EasyOCR (lazy init, singleton)."""
    global _ocr_reader
    if _ocr_reader is None:
        logger.info(
            "Khởi tạo EasyOCR với ngôn ngữ: %s, GPU: %s",
            settings.OCR_LANGUAGES,
            settings.OCR_GPU,
        )
        _ocr_reader = easyocr.Reader(
            lang_list=settings.OCR_LANGUAGES,
            gpu=settings.OCR_GPU,
            verbose=False,
        )
    return _ocr_reader


# ---------------------------------------------------------------------------
# Lưu numpy array ra file tạm để DeepFace đọc
# ---------------------------------------------------------------------------
def _save_temp_image(img_array: np.ndarray, suffix: str = ".jpg") -> str:
    """
    Lưu numpy array ra file tạm trên disk.
    Trả về đường dẫn file (caller chịu trách nhiệm xóa sau khi dùng).
    """
    with tempfile.NamedTemporaryFile(
        suffix=suffix, delete=False, prefix="ekyc_"
    ) as tmp:
        from PIL import Image
        Image.fromarray(img_array).save(tmp.name)
        return tmp.name


# ---------------------------------------------------------------------------
# Regex nhận dạng số CCCD / CMND trên ảnh mặt trước CCCD Việt Nam
# ---------------------------------------------------------------------------
# CCCD mới: 12 chữ số  |  CMND cũ: 9 chữ số
_ID_PATTERN = re.compile(r"\b(\d{12}|\d{9})\b")

# Từ khoá chắc chắn KHÔNG phải số CCCD (ngày sinh, mã ZIP, ...)
_EXCLUDE_PATTERN = re.compile(
    r"(\d{1,2}[/\-]\d{1,2}[/\-]\d{2,4})"  # ngày sinh
)


def _is_valid_id_number(candidate: str, raw_line: str) -> bool:
    """
    Trả True nếu chuỗi candidate trông giống số CCCD/CMND thực sự.
    Loại bỏ các chuỗi số xuất hiện ngay cạnh ký tự '/' hoặc '-' (ngày sinh).
    """
    if _EXCLUDE_PATTERN.search(raw_line):
        return False
    return True


# ---------------------------------------------------------------------------
# 1. OCR – Bóc tách Số CCCD từ ảnh mặt trước
# ---------------------------------------------------------------------------
def extract_id_card_number(front_img: np.ndarray) -> Optional[str]:
    """
    Quét toàn bộ văn bản trên ảnh mặt trước CCCD bằng EasyOCR,
    sau đó bóc tách chuỗi Số CCCD (12 chữ số) hoặc số CMND (9 chữ số).

    Args:
        front_img: numpy array ảnh mặt trước CCCD (RGB)

    Returns:
        Chuỗi số CCCD (vd: "012345678901") hoặc None nếu không tìm được.
    """
    reader = _get_ocr_reader()

    results = reader.readtext(
        front_img,
        detail=1,           # [(bbox, text, confidence), ...]
        paragraph=False,
    )

    # Lọc các dòng có confidence >= 0.3
    lines = [
        (text.strip(), conf)
        for (_bbox, text, conf) in results
        if conf >= 0.3 and text.strip()
    ]

    raw_text = "\n".join(t for t, _ in lines)
    logger.info("OCR nhận dạng được %d dòng văn bản", len(lines))
    logger.debug("OCR raw text:\n%s", raw_text)

    # Ưu tiên 12 chữ số trước (CCCD mới), sau đó 9 chữ số (CMND cũ)
    found_12: Optional[str] = None
    found_9: Optional[str] = None

    for text_line, _ in lines:
        for match in _ID_PATTERN.finditer(text_line):
            candidate = match.group(1)
            if not _is_valid_id_number(candidate, text_line):
                continue
            if len(candidate) == 12 and found_12 is None:
                found_12 = candidate
            elif len(candidate) == 9 and found_9 is None:
                found_9 = candidate

    id_number = found_12 or found_9

    if id_number:
        logger.info("✓ Bóc tách Số CCCD thành công: %s", id_number)
    else:
        logger.warning("✗ Không tìm thấy Số CCCD trong ảnh mặt trước")

    return id_number


# ---------------------------------------------------------------------------
# 2. Face Embedding Extraction
# ---------------------------------------------------------------------------
def extract_face_embedding(selfie_img: np.ndarray) -> list[float]:
    """
    Trích xuất vector khuôn mặt 512 chiều từ ảnh selfie bằng DeepFace.represent.

    Args:
        selfie_img: numpy array ảnh selfie (RGB)

    Returns:
        list 512 phần tử float

    Raises:
        ValueError: Nếu không tìm thấy khuôn mặt hoặc embedding không đúng 512 chiều
    """
    tmp_path = None
    try:
        tmp_path = _save_temp_image(selfie_img)

        embedding_objs = DeepFace.represent(
            img_path=tmp_path,
            model_name=settings.DEEPFACE_MODEL,   # Facenet512 → 512 chiều
            detector_backend=settings.DEEPFACE_DETECTOR,
            enforce_detection=True,
            align=True,
        )

        if not embedding_objs:
            raise ValueError("DeepFace.represent() trả về danh sách rỗng.")

        # Lấy khuôn mặt đầu tiên tìm được
        embedding: list[float] = embedding_objs[0]["embedding"]

        if len(embedding) != 512:
            raise ValueError(
                f"Vector khuôn mặt có độ dài {len(embedding)}, "
                f"kỳ vọng 512. Hãy dùng model 'Facenet512' hoặc 'ArcFace'."
            )

        logger.info("Trích xuất embedding thành công: %d chiều", len(embedding))
        return [round(float(v), 8) for v in embedding]

    except ValueError as exc:
        msg = str(exc)
        if "Face could not be detected" in msg or "face" in msg.lower():
            raise ValueError(
                "Không tìm thấy khuôn mặt trong ảnh selfie. "
                "Hãy đảm bảo ảnh rõ nét, chứa khuôn mặt và đủ ánh sáng."
            ) from exc
        raise

    finally:
        if tmp_path and os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except OSError:
                pass
