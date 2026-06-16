"""
eKYC API Controller
=====================
POST /api/v1/ai/verify-ekyc

Luồng tự động hóa:
  1. Tải 3 ảnh từ URL (mặt trước CCCD, mặt sau CCCD, ảnh selfie).
  2. Dùng EasyOCR bóc tách Số CCCD từ ảnh mặt trước.
  3. Dùng DeepFace.represent trích xuất vector embedding 512 chiều từ ảnh selfie.
  4. Trả về { id_card_number, embedding } cho Spring Boot.
"""

import logging
from fastapi import APIRouter, HTTPException, status

from app.schemas.ekyc_schema import EKYCRequest, EKYCResponse
from app.services.ekyc_service import extract_id_card_number, extract_face_embedding
from app.utils.image_utils import download_image

logger = logging.getLogger(__name__)

router = APIRouter(
    prefix="/ai",
    tags=["eKYC – AI Identity Verification"],
)


@router.post(
    "/verify-ekyc",
    response_model=EKYCResponse,
    status_code=status.HTTP_200_OK,
    summary="Xác thực danh tính điện tử (eKYC) – Tự động hoàn toàn",
    description=(
        "Thực hiện xác thực eKYC tự động gồm 2 bước:\n"
        "1. **OCR**: Bóc tách Số CCCD từ ảnh mặt trước CCCD bằng EasyOCR.\n"
        "2. **Face Embedding**: Trích xuất vector 512 chiều từ ảnh selfie bằng DeepFace.\n\n"
        "> Tất cả 3 ảnh phải là URL trỏ đến file ảnh hợp lệ (JPG/PNG).\n"
        "> Client **KHÔNG** cần gửi số CCCD; hệ thống tự đọc từ ảnh."
    ),
    responses={
        200: {"description": "Xử lý thành công – trả về số CCCD và embedding"},
        400: {"description": "Ảnh không hợp lệ hoặc không tìm thấy khuôn mặt"},
        422: {"description": "Dữ liệu đầu vào không đúng định dạng"},
        500: {"description": "Lỗi server nội bộ"},
    },
)
async def verify_ekyc(payload: EKYCRequest) -> EKYCResponse:
    """
    Endpoint eKYC tự động:
    - Nhận 3 URL ảnh: mặt trước CCCD, mặt sau CCCD, ảnh selfie.
    - Trả về { id_card_number (có thể None nếu OCR thất bại), embedding (512 chiều) }.
    """
    front_url = str(payload.front_image_url)
    back_url = str(payload.back_image_url)
    face_url = str(payload.face_image_url)

    # ── Bước 1: Tải ảnh từ URL ───────────────────────────────────────────
    logger.info("Bắt đầu eKYC tự động – tải ảnh từ URL")

    try:
        front_img = download_image(front_url)
        logger.info("✓ Tải ảnh mặt trước CCCD thành công: %s", front_url)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Không thể tải ảnh mặt trước CCCD: {exc}",
        )

    try:
        _back_img = download_image(back_url)  # noqa: F841 – tải để xác nhận URL hợp lệ
        logger.info("✓ Tải ảnh mặt sau CCCD thành công: %s", back_url)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Không thể tải ảnh mặt sau CCCD: {exc}",
        )

    try:
        selfie_img = download_image(face_url)
        logger.info("✓ Tải ảnh selfie thành công: %s", face_url)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Không thể tải ảnh selfie: {exc}",
        )

    # ── Bước 2: OCR – Bóc tách Số CCCD từ ảnh mặt trước ────────────────
    id_card_number: str | None = None
    try:
        id_card_number = extract_id_card_number(front_img)
        if id_card_number:
            logger.info("✓ OCR bóc tách Số CCCD: %s", id_card_number)
        else:
            # Trả về None – Spring Boot sẽ báo lỗi cho client
            logger.warning("✗ OCR không đọc được Số CCCD từ ảnh mặt trước")
    except Exception as exc:
        logger.exception("Lỗi OCR không xác định")
        # Trả về None thay vì crash – Spring Boot xử lý lỗi này
        id_card_number = None

    # ── Bước 3: Face Embedding – Trích xuất vector 512 chiều ─────────────
    try:
        face_embedding = extract_face_embedding(selfie_img=selfie_img)
        logger.info("✓ Trích xuất face embedding: %d chiều", len(face_embedding))
    except ValueError as exc:
        logger.warning("Trích xuất embedding thất bại: %s", exc)
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        )
    except Exception as exc:
        logger.exception("Lỗi không xác định trong face embedding")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Lỗi trích xuất khuôn mặt: {exc}",
        )

    # ── Trả về kết quả ───────────────────────────────────────────────────
    return EKYCResponse(
        id_card_number=id_card_number,
        embedding=face_embedding,
    )
