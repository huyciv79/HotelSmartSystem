"""Pydantic schemas for CCCD field extraction."""

from typing import Dict, List, Optional
from pydantic import BaseModel, HttpUrl, Field

# ---------------------------------------------------------------------------
# Request – chỉ cần URL hai mặt CCCD
# ---------------------------------------------------------------------------
class EKYCRequest(BaseModel):
    """
    Body gửi lên endpoint POST /api/v1/ai/verify-ekyc.
    Client gửi URL hai mặt CCCD; YOLOv11 + VietOCR đọc thông tin mặt trước.
    """
    front_image_url: HttpUrl = Field(
        ...,
        description="URL ảnh mặt trước CCCD/CMND",
        example="https://example.com/cccd_front.jpg",
    )
    back_image_url: HttpUrl = Field(
        ...,
        description="URL ảnh mặt sau CCCD/CMND",
        example="https://example.com/cccd_back.jpg",
    )


# Response – chỉ trả về dữ liệu OCR CCCD
class EKYCResponse(BaseModel):
    """
    Response trả về từ endpoint POST /api/v1/ai/verify-ekyc.

    Face embedding được đăng ký riêng qua POST /api/v1/face/enroll.
    """
    id_card_number: Optional[str] = Field(
        None,
        description="Số CCCD/CMND đọc từ vùng id_number (None nếu không đọc được)",
    )
    full_name: Optional[str] = Field(
        None,
        description="Họ và tên đọc từ vùng full_name (None nếu không đọc được)",
    )
    date_of_birth: Optional[str] = Field(
        None,
        description="Ngày sinh đọc từ vùng dob (None nếu không đọc được, định dạng dd/mm/yyyy)",
    )


# ---------------------------------------------------------------------------
# Response lỗi
# ---------------------------------------------------------------------------
    gender: Optional[str] = Field(None, description="Gioi tinh suy tu chu so thu 4 cua CCCD")
    hometown: Optional[str] = Field(None, description="Que quan/tinh thanh suy tu 3 so dau CCCD")
    validation_passed: bool = Field(False, description="Ket qua cross-field validation")
    validation_errors: List[str] = Field(default_factory=list)
    validation_warnings: List[str] = Field(default_factory=list)
    logic_gender: Optional[str] = None
    logic_birth_year: Optional[int] = None
    province_code: Optional[str] = None
    province_name: Optional[str] = None
    corrected_fields: Dict[str, str] = Field(default_factory=dict)
    raw_fields: Dict[str, Optional[str]] = Field(default_factory=dict)
    detection_confidences: Dict[str, float] = Field(default_factory=dict)


class ErrorResponse(BaseModel):
    success: bool = False
    message: str
    detail: Optional[str] = None
