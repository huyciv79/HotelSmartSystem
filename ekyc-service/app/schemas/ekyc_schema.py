"""
Pydantic schemas cho eKYC endpoint – phiên bản tự động hóa
"""

from typing import List, Optional
from pydantic import BaseModel, HttpUrl, Field


# ---------------------------------------------------------------------------
# Request – chỉ cần 3 URL ảnh, KHÔNG cần số CCCD
# ---------------------------------------------------------------------------
class EKYCRequest(BaseModel):
    """
    Body gửi lên endpoint POST /api/v1/ai/verify-ekyc.
    Client chỉ cần gửi 3 URL ảnh; số CCCD sẽ được hệ thống tự bóc tách.
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
    face_image_url: HttpUrl = Field(
        ...,
        description="URL ảnh selfie của người dùng",
        example="https://example.com/selfie.jpg",
    )


# ---------------------------------------------------------------------------
# Response – trả về số CCCD đọc được + vector embedding 512 chiều
# ---------------------------------------------------------------------------
class EKYCResponse(BaseModel):
    """
    Response trả về từ endpoint POST /api/v1/ai/verify-ekyc.

    Spring Boot đọc hai trường này để:
      - Lưu id_card_number vào cột IDCardNumber của bảng eKYC_Profiles
      - Lưu embedding vào bảng FaceEmbeddings
    """
    id_card_number: Optional[str] = Field(
        None,
        description="Số CCCD/CMND bóc tách từ ảnh (None nếu không đọc được)",
    )
    full_name: Optional[str] = Field(
        None,
        description="Họ và tên bóc tách từ ảnh (None nếu không đọc được)",
    )
    date_of_birth: Optional[str] = Field(
        None,
        description="Ngày sinh bóc tách từ ảnh (None nếu không đọc được, định dạng dd/mm/yyyy)",
    )
    embedding: List[float] = Field(
        ...,
        description="Vector khuôn mặt 512 chiều trích xuất từ ảnh selfie",
        min_length=512,
        max_length=512,
    )


# ---------------------------------------------------------------------------
# Response lỗi
# ---------------------------------------------------------------------------
class ErrorResponse(BaseModel):
    success: bool = False
    message: str
    detail: Optional[str] = None
