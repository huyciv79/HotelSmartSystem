"""Pydantic schemas for CCCD field extraction."""

from typing import Optional
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
class ErrorResponse(BaseModel):
    success: bool = False
    message: str
    detail: Optional[str] = None
