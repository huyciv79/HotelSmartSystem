"""Independent face enrollment and verification APIs."""

from typing import Annotated

from fastapi import APIRouter, File, Form, HTTPException, UploadFile, status

from app.core.config import settings
from app.schemas.face_schema import (
    FaceEnrollmentResponse,
    FaceVerificationResponse,
)
from app.services.face_service import (
    analyze_face_frame,
    compare_embeddings,
    decode_image,
    detect_liveness,
    extract_single_face_embedding,
    parse_embedding,
    validate_active_liveness,
)

router = APIRouter(
    prefix="/face",
    tags=["Face Recognition"],
)

SUPPORTED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/webp"}


async def _read_image(upload: UploadFile, label: str):
    if upload.content_type and upload.content_type not in SUPPORTED_IMAGE_TYPES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=(
                f"{label} có Content-Type '{upload.content_type}'. "
                "Chỉ hỗ trợ JPG, PNG hoặc WebP."
            ),
        )

    try:
        return decode_image(await upload.read(), label)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc
    finally:
        await upload.close()


def _extract_embedding(image, label: str) -> list[float]:
    try:
        return extract_single_face_embedding(image, label)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Lỗi xử lý khuôn mặt trong {label}: {exc}",
        ) from exc


def _detect_liveness(
    image,
    label: str,
    analysis=None,
) -> dict[str, float | bool | str]:
    try:
        return detect_liveness(image, label, analysis)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Lỗi kiểm tra liveness trong {label}: {exc}",
        ) from exc


@router.post(
    "/enroll",
    response_model=FaceEnrollmentResponse,
    summary="Đăng ký khuôn mặt bằng ảnh CCCD và selfie",
    description=(
        "Phát hiện một khuôn mặt trong ảnh mặt trước CCCD và một khuôn mặt "
        "trong selfie, tạo embedding Facenet512 rồi so sánh bằng cosine distance. "
        "Chỉ trả embedding selfie để lưu khi hai khuôn mặt khớp."
    ),
)
async def enroll_face(
    id_card_image: Annotated[
        UploadFile,
        File(description="Ảnh mặt trước CCCD có chân dung khách"),
    ],
    selfie_image: Annotated[
        UploadFile,
        File(description="Ảnh selfie chính diện của khách"),
    ],
) -> FaceEnrollmentResponse:
    card_image = await _read_image(id_card_image, "ảnh CCCD")
    selfie = await _read_image(selfie_image, "ảnh selfie")

    card_embedding = _extract_embedding(card_image, "ảnh CCCD")
    selfie_embedding = _extract_embedding(selfie, "ảnh selfie")
    result = compare_embeddings(card_embedding, selfie_embedding)
    matched = bool(result["matched"])

    return FaceEnrollmentResponse(
        enrolled=matched,
        matched=matched,
        message=(
            "Đăng ký khuôn mặt thành công."
            if matched
            else "Ảnh CCCD và selfie không phải cùng một người."
        ),
        distance=float(result["distance"]),
        threshold=float(result["threshold"]),
        similarity_percentage=result["similarity_percentage"],
        metric=settings.DEEPFACE_DISTANCE,
        model_used=settings.DEEPFACE_MODEL,
        detector_used=settings.FACE_RECOGNITION_DETECTOR,
        embedding=selfie_embedding if matched else None,
    )


@router.post(
    "/verify",
    response_model=FaceVerificationResponse,
    summary="Xác minh khuôn mặt đã đăng ký",
    description=(
        "Nhận chuỗi frame chính diện, quay trái, quay phải, nhìn lên và nhìn xuống "
        "để kiểm tra active liveness. "
        "Sau đó kiểm tra anti-spoofing và so sánh khuôn mặt thật "
        "với registered_embedding do Spring Boot lưu. "
        "Để dev test trên Swagger, có thể bỏ embedding và tải reference_image "
        "thay thế. Chỉ được cung cấp một trong hai nguồn tham chiếu."
    ),
)
async def verify_face(
    selfie_image: Annotated[
        UploadFile,
        File(description="Ảnh chính diện cần xác minh"),
    ],
    left_image: Annotated[
        UploadFile,
        File(description="Ảnh sau khi khách quay đầu sang trái"),
    ],
    right_image: Annotated[
        UploadFile,
        File(description="Ảnh sau khi khách quay đầu sang phải"),
    ],
    up_image: Annotated[
        UploadFile,
        File(description="Ảnh sau khi khách nhìn lên"),
    ],
    down_image: Annotated[
        UploadFile,
        File(description="Ảnh sau khi khách nhìn xuống"),
    ],
    registered_embedding: Annotated[
        str | None,
        Form(
            description=(
                "JSON array gồm 512 số được trả về từ API /face/enroll. "
                "Để trống nếu dùng reference_image."
            )
        ),
    ] = None,
    reference_image: Annotated[
        UploadFile | None,
        File(
            description=(
                "Ảnh khuôn mặt đã đăng ký, chỉ dành cho dev test. "
                "Để trống nếu dùng registered_embedding."
            )
        ),
    ] = None,
) -> FaceVerificationResponse:
    has_embedding = bool(registered_embedding and registered_embedding.strip())
    has_reference_image = reference_image is not None
    if has_embedding == has_reference_image:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=(
                "Phải cung cấp đúng một nguồn tham chiếu: "
                "registered_embedding hoặc reference_image."
            ),
        )

    if has_embedding:
        try:
            reference_embedding = parse_embedding(registered_embedding or "")
        except ValueError as exc:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=str(exc),
            ) from exc
    else:
        reference = await _read_image(reference_image, "ảnh tham chiếu")
        reference_embedding = _extract_embedding(reference, "ảnh tham chiếu")

    selfie = await _read_image(selfie_image, "ảnh chính diện")
    left = await _read_image(left_image, "ảnh quay trái")
    right = await _read_image(right_image, "ảnh quay phải")
    up = await _read_image(up_image, "ảnh nhìn lên")
    down = await _read_image(down_image, "ảnh nhìn xuống")

    try:
        center_analysis = analyze_face_frame(selfie, "ảnh chính diện")
        left_analysis = analyze_face_frame(left, "ảnh quay trái")
        right_analysis = analyze_face_frame(right, "ảnh quay phải")
        up_analysis = analyze_face_frame(up, "ảnh nhìn lên")
        down_analysis = analyze_face_frame(down, "ảnh nhìn xuống")
        active_liveness = validate_active_liveness(
            center_analysis,
            left_analysis,
            right_analysis,
            up_analysis,
            down_analysis,
        )
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc

    liveness = _detect_liveness(
        selfie,
        "ảnh chính diện",
        center_analysis,
    )
    active_passed = bool(active_liveness["active_liveness_passed"])
    passive_passed = bool(liveness["liveness_passed"])
    if not active_passed or not passive_passed:
        message = (
            "Không vượt qua active liveness. "
            "Hãy nhìn thẳng, quay trái, quay phải, nhìn lên và nhìn xuống theo hướng dẫn."
            if not active_passed
            else (
                "Không vượt qua kiểm tra chống giả mạo. "
                "Vui lòng dùng khuôn mặt thật trước camera, không dùng ảnh hoặc màn hình."
            )
        )
        return FaceVerificationResponse(
            verified=False,
            matched=False,
            liveness_passed=False,
            active_liveness_passed=active_passed,
            is_real=bool(liveness["is_real"]),
            liveness_score=float(liveness["liveness_score"]),
            liveness_threshold=float(liveness["liveness_threshold"]),
            anti_spoofing_model=str(liveness["anti_spoofing_model"]),
            center_yaw=float(active_liveness["center_yaw"]),
            first_turn_yaw=float(active_liveness["first_turn_yaw"]),
            second_turn_yaw=float(active_liveness["second_turn_yaw"]),
            center_pitch=float(active_liveness["center_pitch"]),
            up_pitch=float(active_liveness["up_pitch"]),
            down_pitch=float(active_liveness["down_pitch"]),
            message=message,
            distance=None,
            threshold=float(settings.FACE_MATCH_THRESHOLD),
            similarity_percentage=None,
            metric=settings.DEEPFACE_DISTANCE,
            model_used=settings.DEEPFACE_MODEL,
            detector_used=settings.FACE_RECOGNITION_DETECTOR,
        )

    selfie_embedding = _extract_embedding(selfie, "ảnh chính diện")
    result = compare_embeddings(reference_embedding, selfie_embedding)
    matched = bool(result["matched"])

    return FaceVerificationResponse(
        verified=matched,
        matched=matched,
        liveness_passed=True,
        active_liveness_passed=True,
        is_real=bool(liveness["is_real"]),
        liveness_score=float(liveness["liveness_score"]),
        liveness_threshold=float(liveness["liveness_threshold"]),
        anti_spoofing_model=str(liveness["anti_spoofing_model"]),
        center_yaw=float(active_liveness["center_yaw"]),
        first_turn_yaw=float(active_liveness["first_turn_yaw"]),
        second_turn_yaw=float(active_liveness["second_turn_yaw"]),
        center_pitch=float(active_liveness["center_pitch"]),
        up_pitch=float(active_liveness["up_pitch"]),
        down_pitch=float(active_liveness["down_pitch"]),
        message=(
            "Liveness hợp lệ và khuôn mặt khớp với dữ liệu đã đăng ký."
            if matched
            else "Liveness hợp lệ nhưng khuôn mặt không khớp với dữ liệu đã đăng ký."
        ),
        distance=float(result["distance"]),
        threshold=float(result["threshold"]),
        similarity_percentage=result["similarity_percentage"],
        metric=settings.DEEPFACE_DISTANCE,
        model_used=settings.DEEPFACE_MODEL,
        detector_used=settings.FACE_RECOGNITION_DETECTOR,
    )
