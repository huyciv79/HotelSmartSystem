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
