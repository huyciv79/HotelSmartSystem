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
# 1. OCR – Bóc tách thông tin từ ảnh mặt trước (Số CCCD, Họ tên, Ngày sinh)
# ---------------------------------------------------------------------------
def extract_id_card_details(front_img: np.ndarray) -> tuple[Optional[str], Optional[str], Optional[str]]:
    """
    Quét toàn bộ văn bản trên ảnh mặt trước CCCD bằng EasyOCR,
    sau đó bóc tách chuỗi Số CCCD, Họ tên và Ngày sinh.

    Args:
        front_img: numpy array ảnh mặt trước CCCD (RGB)

    Returns:
        tuple (id_number, full_name, date_of_birth)
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

    # 1. Bóc tách số CCCD/CMND
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

    # 2. Bóc tách Họ tên và Ngày sinh
    full_name: Optional[str] = None
    date_of_birth: Optional[str] = None

    for i, (text_line, _) in enumerate(lines):
        text_lower = text_line.lower()

        # Tìm Họ tên (thường nằm sau hoặc dưới dòng "Họ và tên / Full name")
        if "họ và tên" in text_lower or "full name" in text_lower or "ho va ten" in text_lower:
            if i + 1 < len(lines):
                candidate_name = lines[i + 1][0].strip()
                # Tên trên CCCD luôn viết hoa toàn bộ và thường có từ 2-4 từ
                if candidate_name.isupper() and len(candidate_name.split()) >= 2:
                    full_name = candidate_name

        # Tìm Ngày sinh (regex tìm định dạng dd/mm/yyyy)
        dob_match = re.search(r"(\d{2}/\d{2}/\d{4})", text_line)
        if dob_match and not date_of_birth:
            date_of_birth = dob_match.group(1)

    # Cơ chế dự phòng tìm Họ tên (nếu nhãn Họ và tên không đọc rõ nhưng tên đọc rõ)
    if not full_name:
        for text_line, _ in lines:
            skip_keywords = ["CỘNG HÒA", "ĐỘC LẬP", "TỰ DO", "GIÁ TRỊ", "NƠI ĐK", "THƯỜNG TRÚ", "QUỐC TỊCH", "DÂN TỘC", "QUÊ QUÁN", "NHẬN DẠNG", "NƠI CẤP"]
            if text_line.isupper() and len(text_line.split()) >= 2 and len(text_line.split()) <= 5:
                if not any(kw in text_line for kw in skip_keywords):
                    if not any(char.isdigit() for char in text_line):
                        full_name = text_line
                        break

    if id_number:
        logger.info("✓ Bóc tách Số CCCD thành công: %s", id_number)
    else:
        logger.warning("✗ Không tìm thấy Số CCCD trong ảnh mặt trước")

    if full_name:
        logger.info("✓ Bóc tách Họ tên thành công: %s", full_name)
    else:
        logger.warning("✗ Không tìm thấy Họ tên trong ảnh mặt trước")

    if date_of_birth:
        logger.info("✓ Bóc tách Ngày sinh thành công: %s", date_of_birth)
    else:
        logger.warning("✗ Không tìm thấy Ngày sinh trong ảnh mặt trước")

    return id_number, full_name, date_of_birth


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
