"""Pydantic schemas for the face recognition APIs."""

from typing import List, Optional

from pydantic import BaseModel, Field


class FaceEnrollmentResponse(BaseModel):
    enrolled: bool
    matched: bool
    message: str
    distance: float
    threshold: float
    similarity_percentage: Optional[float] = None
    metric: str
    model_used: str
    detector_used: str
    embedding: Optional[List[float]] = Field(
        default=None,
        description=(
            "Vector selfie 512 chiều. Chỉ trả về khi ảnh CCCD và selfie khớp "
            "để Spring Boot lưu sau khi đăng ký."
        ),
    )


class FaceVerificationResponse(BaseModel):
    verified: bool
    matched: bool
    liveness_passed: bool
    active_liveness_passed: bool
    is_real: bool
    liveness_score: float
    liveness_threshold: float
    anti_spoofing_model: str
    center_yaw: Optional[float] = None
    first_turn_yaw: Optional[float] = None
    second_turn_yaw: Optional[float] = None
    center_pitch: Optional[float] = None
    up_pitch: Optional[float] = None
    down_pitch: Optional[float] = None
    message: str
    distance: Optional[float] = None
    threshold: float
    similarity_percentage: Optional[float] = None
    metric: str
    model_used: str
    detector_used: str
