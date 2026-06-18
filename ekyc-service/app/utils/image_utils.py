"""
Các hàm tiện ích dùng chung
"""

import re
import logging
from io import BytesIO
from typing import Optional, Tuple

import numpy as np
import requests
from PIL import Image

from app.core.config import settings

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Tải ảnh từ URL
# ---------------------------------------------------------------------------
def download_image(url: str) -> np.ndarray:
    """
    Tải ảnh từ URL và chuyển về numpy array (RGB).

    Args:
        url: Đường dẫn URL của ảnh

    Returns:
        numpy array dạng (H, W, 3) – kênh màu RGB

    Raises:
        ValueError: Nếu không tải được ảnh hoặc ảnh không hợp lệ
    """
    try:
        response = requests.get(
            url,
            timeout=settings.REQUEST_TIMEOUT,
            headers={"User-Agent": "eKYC-Service/1.0"},
            stream=True,
        )
        response.raise_for_status()

        content_type = response.headers.get("Content-Type", "")
        if not content_type.startswith("image/"):
            # Cho phép vì nhiều server không set đúng Content-Type
            logger.warning("URL %s trả về Content-Type: %s", url, content_type)

        image = Image.open(BytesIO(response.content)).convert("RGB")
        return np.array(image)

    except requests.exceptions.Timeout:
        raise ValueError(f"Timeout khi tải ảnh từ URL: {url}")
    except requests.exceptions.HTTPError as exc:
        raise ValueError(f"HTTP {exc.response.status_code} khi tải ảnh: {url}")
    except requests.exceptions.RequestException as exc:
        raise ValueError(f"Không thể tải ảnh từ URL ({url}): {exc}")
    except Exception as exc:
        raise ValueError(f"Ảnh không hợp lệ hoặc không đọc được ({url}): {exc}")


# ---------------------------------------------------------------------------
# Phân tích kết quả OCR – trích xuất các trường CCCD
# ---------------------------------------------------------------------------

# Regex các trường thông dụng trên CCCD Việt Nam
_PATTERNS = {
    # Số CCCD: 12 chữ số, hoặc CMND: 9 chữ số
    "id_number": re.compile(
        r"\b(\d{12}|\d{9})\b"
    ),
    # Ngày sinh: DD/MM/YYYY hoặc DD-MM-YYYY
    "dob": re.compile(
        r"\b(\d{1,2}[\/\-]\d{1,2}[\/\-]\d{4})\b"
    ),
}

# Từ khoá tiêu đề trước họ tên trên CCCD
_NAME_KEYWORDS = [
    "họ và tên",
    "ho va ten",
    "full name",
    "họ tên",
    "name",
]


def _normalize(text: str) -> str:
    """Chuẩn hoá text: viết thường, bỏ khoảng trắng thừa."""
    return " ".join(text.lower().split())


def parse_cccd_info(raw_text: str) -> Tuple[Optional[str], Optional[str], Optional[str]]:
    """
    Phân tích văn bản OCR để trích xuất:
      - Số CCCD/CMND
      - Họ tên
      - Ngày sinh

    Args:
        raw_text: Toàn bộ văn bản OCR ghép lại

    Returns:
        Tuple (id_number, full_name, dob)
    """
    id_number: Optional[str] = None
    full_name: Optional[str] = None
    dob: Optional[str] = None

    lines = [ln.strip() for ln in raw_text.splitlines() if ln.strip()]

    # 1. Tìm số CCCD / CMND
    for line in lines:
        m = _PATTERNS["id_number"].search(line)
        if m:
            id_number = m.group(1)
            break

    # 2. Tìm ngày sinh
    for line in lines:
        m = _PATTERNS["dob"].search(line)
        if m:
            dob = m.group(1)
            break

    # 3. Tìm họ tên – lấy dòng ngay sau từ khoá "Họ và tên"
    for idx, line in enumerate(lines):
        norm = _normalize(line)
        if any(kw in norm for kw in _NAME_KEYWORDS):
            # Lấy phần sau dấu ':' trên cùng dòng
            if ":" in line:
                candidate = line.split(":", 1)[1].strip()
                if candidate:
                    full_name = candidate.upper()
                    break
            # Hoặc lấy dòng tiếp theo
            if idx + 1 < len(lines):
                next_line = lines[idx + 1].strip()
                # Lọc các dòng có vẻ là tên (chỉ chữ cái và khoảng trắng)
                if re.match(r"^[\w\sÀ-ỹ]{3,60}$", next_line, re.UNICODE):
                    full_name = next_line.upper()
                    break

    return id_number, full_name, dob
