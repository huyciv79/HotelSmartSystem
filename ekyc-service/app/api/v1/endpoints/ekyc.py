"""CCCD field extraction API using YOLOv11 and VietOCR."""

import logging
from fastapi import APIRouter, HTTPException, status

from app.schemas.ekyc_schema import EKYCRequest, EKYCResponse
from app.services.ekyc_service import extract_id_card_details
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
    summary="Trích xuất thông tin mặt trước CCCD",
    description=(
        "Tải ảnh CCCD, dùng YOLOv11 phát hiện các vùng id_number, full_name và dob, "
        "sau đó dùng VietOCR để đọc tiếng Việt. Giới tính và quê quán/tỉnh thành được suy từ số CCCD, "
        "không đọc từ vùng giới tính/quê quán bằng OCR. API này không nhận selfie, "
        "không tạo embedding và không so sánh khuôn mặt."
    ),
    responses={
        200: {"description": "Trả về số CCCD, họ tên và ngày sinh đọc được"},
        400: {"description": "Ảnh không hợp lệ hoặc không phát hiện được vùng thông tin"},
        422: {"description": "Dữ liệu đầu vào không đúng định dạng"},
        500: {"description": "Lỗi server nội bộ"},
    },
)
async def verify_ekyc(payload: EKYCRequest) -> EKYCResponse:
    """
    Mặt sau hiện được tải để kiểm tra URL/ảnh hợp lệ; OCR thực hiện trên mặt trước.
    """
    front_url = str(payload.front_image_url)
    back_url = str(payload.back_image_url)
    logger.info("Bắt đầu trích xuất thông tin CCCD bằng YOLOv11 + VietOCR")

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
        details = extract_id_card_details(front_img)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc
    except Exception as exc:
        logger.exception("Lỗi pipeline YOLOv11 + VietOCR")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Lỗi trích xuất thông tin CCCD: {exc}",
        ) from exc

    return EKYCResponse(
        id_card_number=details.get("id_card_number"),
        full_name=details.get("full_name"),
        date_of_birth=details.get("date_of_birth"),
        gender=details.get("gender"),
        hometown=details.get("hometown"),
        validation_passed=details.get("validation_passed", False),
        validation_errors=details.get("validation_errors", []),
        validation_warnings=details.get("validation_warnings", []),
        logic_gender=details.get("logic_gender"),
        logic_birth_year=details.get("logic_birth_year"),
        province_code=details.get("province_code"),
        province_name=details.get("province_name"),
        corrected_fields=details.get("corrected_fields", {}),
        raw_fields=details.get("raw_fields", {}),
        detection_confidences=details.get("detection_confidences", {}),
    )
