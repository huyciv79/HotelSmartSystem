"""
Cấu hình ứng dụng – đọc từ biến môi trường hoặc file .env
"""

from typing import List, Optional
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # ── Thông tin ứng dụng ──────────────────────────────────────────────────
    APP_NAME: str = "eKYC AI Service"
    APP_VERSION: str = "1.0.0"

    # ── Server ──────────────────────────────────────────────────────────────
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    APP_DEBUG: bool = False

    # ── CORS ────────────────────────────────────────────────────────────────
    ALLOWED_ORIGINS: List[str] = ["*"]

    # ── DeepFace ────────────────────────────────────────────────────────────
    # Model hỗ trợ 512-dim: "Facenet512", "ArcFace", "DeepFace"
    DEEPFACE_MODEL: str = "Facenet512"
    DEEPFACE_DETECTOR: str = "opencv"       # opencv | retinaface | mtcnn
    DEEPFACE_DISTANCE: str = "cosine"       # cosine | euclidean | euclidean_l2
    FACE_MATCH_THRESHOLD: float = 0.30      # ngưỡng Facenet512 cosine được DeepFace hiệu chỉnh
    FACE_ENROLLMENT_CONSISTENCY_THRESHOLD: float = 0.48
    FACE_RECOGNITION_DETECTOR: str = "retinaface"
    LIVENESS_MIN_SCORE: float = 0.80        # độ tin cậy tối thiểu của anti-spoofing
    ACTIVE_LIVENESS_CENTER_MAX_YAW: float = 0.08
    ACTIVE_LIVENESS_SIDE_MIN_YAW: float = 0.20
    ACTIVE_LIVENESS_MIN_YAW_RANGE: float = 0.42
    ACTIVE_LIVENESS_VERTICAL_MIN_DELTA: float = 0.08
    ACTIVE_LIVENESS_MIN_PITCH_RANGE: float = 0.18
    EXPRESS_LIVENESS_CENTER_MAX_YAW: float = 0.12
    EXPRESS_LIVENESS_SIDE_MIN_YAW: float = 0.12
    EXPRESS_LIVENESS_MIN_YAW_DELTA: float = 0.10
    FACE_READINESS_MIN_SIZE: int = 96

    # ── CCCD field detection (YOLOv11 on Hugging Face) ─────────────────────
    HF_TOKEN: Optional[str] = None
    HF_REPO_ID: str = "phamminhanh2004/hotel-cccd-ocr-v11"
    HF_MODEL_FILE: str = "best.pt"
    HF_REVISION: Optional[str] = None
    CCCD_YOLO_IMAGE_SIZE: int = 640
    CCCD_YOLO_CONFIDENCE: float = 0.15
    CCCD_YOLO_IOU: float = 0.50
    CCCD_YOLO_AGNOSTIC_NMS: bool = True

    # ── VietOCR ─────────────────────────────────────────────────────────────
    VIETOCR_CONFIG: str = "vgg_transformer"
    VIETOCR_CONFIG_PATH: str = "config/vietocr-vgg-transformer.yml"
    VIETOCR_WEIGHTS_URL: str = "https://vocr.vn/data/vietocr/vgg_transformer.pth"
    OCR_DEVICE: str = "auto"                # auto | cpu | cuda | cuda:0
    OCR_CROP_HEIGHT: int = 80
    OCR_PADDING_PIXELS: int = 6

    # ── HTTP request ────────────────────────────────────────────────────────
    REQUEST_TIMEOUT: int = 15               # giây

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
    )


settings = Settings()
