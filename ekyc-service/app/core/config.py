"""
Cấu hình ứng dụng – đọc từ biến môi trường hoặc file .env
"""

from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    # ── Thông tin ứng dụng ──────────────────────────────────────────────────
    APP_NAME: str = "eKYC AI Service"
    APP_VERSION: str = "1.0.0"

    # ── Server ──────────────────────────────────────────────────────────────
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    DEBUG: bool = False

    # ── CORS ────────────────────────────────────────────────────────────────
    ALLOWED_ORIGINS: List[str] = ["*"]

    # ── DeepFace ────────────────────────────────────────────────────────────
    # Model hỗ trợ 512-dim: "Facenet512", "ArcFace", "DeepFace"
    DEEPFACE_MODEL: str = "Facenet512"
    DEEPFACE_DETECTOR: str = "opencv"       # opencv | retinaface | mtcnn
    DEEPFACE_DISTANCE: str = "cosine"       # cosine | euclidean | euclidean_l2
    FACE_MATCH_THRESHOLD: float = 0.40      # ngưỡng xác nhận khớp (cosine)
    FACE_RECOGNITION_DETECTOR: str = "retinaface"
    LIVENESS_MIN_SCORE: float = 0.80        # độ tin cậy tối thiểu của anti-spoofing
    ACTIVE_LIVENESS_CENTER_MAX_YAW: float = 0.12
    ACTIVE_LIVENESS_SIDE_MIN_YAW: float = 0.13
    ACTIVE_LIVENESS_MIN_YAW_RANGE: float = 0.30
    ACTIVE_LIVENESS_VERTICAL_MIN_DELTA: float = 0.04
    ACTIVE_LIVENESS_MIN_PITCH_RANGE: float = 0.10

    # ── EasyOCR ─────────────────────────────────────────────────────────────
    OCR_LANGUAGES: List[str] = ["vi", "en"]
    OCR_GPU: bool = False                   # Bật True nếu có CUDA

    # ── HTTP request ────────────────────────────────────────────────────────
    REQUEST_TIMEOUT: int = 15               # giây

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
    )


settings = Settings()
