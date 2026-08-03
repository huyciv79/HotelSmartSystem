"""
eKYC Service - FastAPI Application
====================================
Dịch vụ xác thực danh tính điện tử (eKYC) với khả năng:
  - Đăng ký embedding khuôn mặt từ selfie bằng DeepFace
  - Xác minh khuôn mặt đã đăng ký khi check-in
  - Trích xuất vector khuôn mặt (Face Embedding) 512 chiều
  - Phát hiện vùng thông tin CCCD bằng YOLOv11
  - Đọc tiếng Việt trong từng vùng bằng VietOCR
"""

from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.v1.router import api_v1_router
from app.core.config import settings
from app.services.ekyc_service import warm_ocr_models
from app.services.face_service import warm_face_models


@asynccontextmanager
async def lifespan(_app: FastAPI):
    """Hiển thị các URL dành cho developer khi ứng dụng khởi động."""
    try:
        warm_face_models()
    except Exception as e:
        print(f"Warning: Failed to warm face models on startup: {e}", flush=True)

    try:
        warm_ocr_models()
    except Exception as e:
        print(f"Warning: Failed to warm OCR models on startup: {e}", flush=True)

    print("\n" + "=" * 60, flush=True)
    print("eKYC AI Service is running", flush=True)
    print(f"Swagger UI : http://localhost:{settings.PORT}/docs", flush=True)
    print(f"Health check: http://localhost:{settings.PORT}/health", flush=True)
    print("=" * 60 + "\n", flush=True)
    yield


# ---------------------------------------------------------------------------
# Khởi tạo ứng dụng FastAPI
# ---------------------------------------------------------------------------
app = FastAPI(
    title=settings.APP_NAME,
    description=(
        "API eKYC – OCR CCCD, đăng ký khuôn mặt selfie và xác minh FaceID khi check-in.\n\n"
        "Đăng ký eKYC không so sánh selfie với chân dung trên CCCD."
    ),
    version=settings.APP_VERSION,
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# ---------------------------------------------------------------------------
# CORS Middleware
# ---------------------------------------------------------------------------
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------------------------------------------------------
# Đăng ký router
# ---------------------------------------------------------------------------
app.include_router(api_v1_router, prefix="/api/v1")


# ---------------------------------------------------------------------------
# Health-check endpoint
# ---------------------------------------------------------------------------
@app.get("/health", tags=["Health"])
async def health_check():
    """Kiểm tra trạng thái dịch vụ."""
    return {"status": "ok", "service": settings.APP_NAME, "version": settings.APP_VERSION}


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.APP_DEBUG,
        log_level="info",
    )
