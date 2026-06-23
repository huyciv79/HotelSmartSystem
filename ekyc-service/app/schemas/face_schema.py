"""Pydantic schemas for the face recognition APIs."""

from typing import List, Optional

from pydantic import BaseModel, Field


class FrameValidationResponse(BaseModel):
    """Result of per-step realtime frame validation during liveness enrollment."""
    passed: bool
    step: str                          # center | left | right | up | down
    code: str                          # PASS | NO_FACE | MULTIPLE_FACES | WRONG_POSE | SPOOF
    reason: str                        # Human-readable Vietnamese message
    yaw_score: Optional[float] = None  # signed yaw: >0 = quay trái (mirror), <0 = phải
    pitch_score: Optional[float] = None
    reference_yaw: Optional[float] = None
    reference_pitch: Optional[float] = None
    liveness_score: Optional[float] = None
    face_distance: Optional[float] = None
    face_threshold: Optional[float] = None


class FaceEnrollmentResponse(BaseModel):
    enrolled: bool
    liveness_passed: bool
    active_liveness_passed: bool
    message: str
    model_used: str
    detector_used: str
    embedding: List[float] = Field(
        description=(
            "Face template 512 chiều tổng hợp từ các góc chính diện, trái, phải, "
            "lên và xuống sau khi vượt qua liveness."
        ),
    )


class FaceReadinessResponse(BaseModel):
    ready: bool
    reason: str
    code: str
    face_count: int
    face_width: Optional[int] = None
    face_height: Optional[int] = None
    yaw: Optional[float] = None


class FaceVerificationResponse(BaseModel):
    verified: bool
    matched: bool
    liveness_passed: bool
    active_liveness_passed: bool
    is_real: bool
    liveness_score: float
    liveness_threshold: float
    anti_spoofing_model: str
    tta_real_votes: Optional[int] = Field(
        default=None,
        description="Số biến thể TTA (trên tổng tta_total_frames) cho kết quả 'thật'.",
    )
    tta_total_frames: Optional[int] = Field(
        default=None,
        description="Tổng số biến thể TTA được chạy qua MiniFASNet.",
    )
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
