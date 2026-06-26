"""Independent face enrollment and verification APIs."""

import logging
from typing import Annotated

from fastapi import APIRouter, File, Form, HTTPException, UploadFile, status

from app.core.config import settings
from app.schemas.face_schema import (
    FaceEnrollmentResponse,
    FaceReadinessResponse,
    FaceVerificationResponse,
    FrameValidationResponse,
)
from app.services.face_service import (
    analyze_face_frame,
    build_multi_angle_face_template,
    check_face_readiness,
    compare_embeddings,
    decode_image,
    detect_liveness,
    extract_single_face_embedding,
    parse_embedding,
    validate_active_liveness,
    validate_quick_active_liveness,
)

logger = logging.getLogger(__name__)

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


def _extract_embedding(image, label: str, analysis=None) -> list[float]:
    try:
        return extract_single_face_embedding(image, label, analysis)
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
    "/check-readiness",
    response_model=FaceReadinessResponse,
    summary="Kiểm tra khuôn mặt trước khi quét liveness",
    description=(
        "Kiểm tra ảnh preview có đúng một khuôn mặt, khuôn mặt đủ lớn và đang nhìn thẳng. "
        "Endpoint không tạo embedding và không lưu ảnh."
    ),
)
async def check_readiness(
    selfie_image: Annotated[
        UploadFile,
        File(description="Ảnh preview ngay trước khi bắt đầu quét"),
    ],
) -> FaceReadinessResponse:
    image = await _read_image(selfie_image, "ảnh kiểm tra")
    try:
        return FaceReadinessResponse(
            **check_face_readiness(image, "ảnh kiểm tra"),
        )
    except Exception as exc:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Lỗi kiểm tra trạng thái khuôn mặt: {exc}",
        ) from exc


# ── Pose angle requirements for each enrollment step ────────────────────────
_STEP_POSE_RULES: dict[str, dict] = {
    "center": {
        "label": "chính diện",
        "center_max_yaw": None,   # uses ACTIVE_LIVENESS_CENTER_MAX_YAW
        "side_min_yaw": None,
        "side_dir": None,
        "pitch_up": False,
        "pitch_down": False,
    },
    "left": {
        "label": "quay trái",
        "center_max_yaw": None,
        "side_min_yaw": None,
        "side_dir": "left",    # yaw_score > 0 in mirrored camera coords
        "pitch_up": False,
        "pitch_down": False,
    },
    "right": {
        "label": "quay phải",
        "center_max_yaw": None,
        "side_min_yaw": None,
        "side_dir": "right",   # yaw_score < 0 in mirrored camera coords
        "pitch_up": False,
        "pitch_down": False,
    },
    "up": {
        "label": "nhìn lên",
        "center_max_yaw": None,
        "side_min_yaw": None,
        "side_dir": None,
        "pitch_up": True,
        "pitch_down": False,
    },
    "down": {
        "label": "nhìn xuống",
        "center_max_yaw": None,
        "side_min_yaw": None,
        "side_dir": None,
        "pitch_up": False,
        "pitch_down": True,
    },
}

@router.post(
    "/validate-frame",
    response_model=FrameValidationResponse,
    summary="Xác thực realtime từng bước liveness khi đăng ký",
    description=(
        "Nhận một frame ảnh và tên bước (center/left/right/up/down). "
        "Kiểm tra góc tư thế, số khuôn mặt và anti-spoofing ngay lập tức. "
        "Frontend dùng endpoint này để biết bước vừa chụp đạt hay cần chụp lại."
    ),
)
async def validate_frame(
    frame_image: Annotated[
        UploadFile,
        File(description="Frame vừa chụp từ camera liveness"),
    ],
    step: Annotated[
        str,
        Form(description="Bước liveness: center | left | right | up | down"),
    ],
    reference_image: Annotated[
        UploadFile | None,
        File(description="Ảnh chính diện đã đạt, bắt buộc với các bước trái/phải/lên/xuống"),
    ] = None,
    opposite_image: Annotated[
        UploadFile | None,
        File(description="Ảnh quay ngang trước đó, dùng để xác nhận hai hướng quay đối nhau"),
    ] = None,
) -> FrameValidationResponse:
    step = step.strip().lower()
    if step not in _STEP_POSE_RULES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"step phải là một trong: {', '.join(_STEP_POSE_RULES)}.",
        )

    rule = _STEP_POSE_RULES[step]
    label = f"ảnh {rule['label']}"

    # ── 1. Decode & detect face ──────────────────────────────────────────────
    try:
        image = await _read_image(frame_image, label)
    except HTTPException as exc:
        return FrameValidationResponse(
            passed=False,
            step=step,
            code="DECODE_ERROR",
            reason=exc.detail,
        )

    try:
        analysis = analyze_face_frame(image, label)
    except ValueError as exc:
        msg = str(exc)
        code = "MULTIPLE_FACES" if "khuôn mặt" in msg and "nhiều" in msg or len(msg) > 30 and "người" in msg else "NO_FACE"
        return FrameValidationResponse(
            passed=False,
            step=step,
            code=code,
            reason=msg,
        )
    except Exception as exc:
        return FrameValidationResponse(
            passed=False,
            step=step,
            code="SERVER_ERROR",
            reason=f"Lỗi phân tích khuôn mặt: {exc}",
        )

    yaw   = analysis["yaw_score"]
    pitch = analysis["pitch_score"]
    x1, y1, x2, y2 = analysis["facial_area"]
    min_face_size = int(settings.FACE_READINESS_MIN_SIZE)
    if x2 - x1 < min_face_size or y2 - y1 < min_face_size:
        return FrameValidationResponse(
            passed=False,
            step=step,
            code="FACE_TOO_SMALL",
            reason="Khuôn mặt đang quá nhỏ. Hãy tiến gần camera hơn rồi quét lại bước này.",
            yaw_score=yaw,
            pitch_score=pitch,
        )

    reference_analysis = None
    reference_yaw = None
    reference_pitch = None

    if step != "center":
        if reference_image is None:
            return FrameValidationResponse(
                passed=False,
                step=step,
                code="MISSING_REFERENCE",
                reason="Thiếu ảnh chính diện đã đạt. Vui lòng quét lại từ bước chính diện.",
                yaw_score=yaw,
                pitch_score=pitch,
            )
        try:
            reference = await _read_image(reference_image, "ảnh chính diện tham chiếu")
            reference_analysis = analyze_face_frame(
                reference,
                "ảnh chính diện tham chiếu",
            )
        except (HTTPException, ValueError) as exc:
            reason = exc.detail if isinstance(exc, HTTPException) else str(exc)
            return FrameValidationResponse(
                passed=False,
                step=step,
                code="INVALID_REFERENCE",
                reason=f"Ảnh chính diện tham chiếu không hợp lệ: {reason}",
                yaw_score=yaw,
                pitch_score=pitch,
            )
        reference_yaw = float(reference_analysis["yaw_score"])
        reference_pitch = float(reference_analysis["pitch_score"])
        if abs(reference_yaw) > float(settings.ACTIVE_LIVENESS_CENTER_MAX_YAW):
            return FrameValidationResponse(
                passed=False,
                step=step,
                code="INVALID_REFERENCE",
                reason="Ảnh chính diện tham chiếu bị lệch. Vui lòng quét lại từ bước chính diện.",
                yaw_score=yaw,
                pitch_score=pitch,
                reference_yaw=reference_yaw,
                reference_pitch=reference_pitch,
            )

    # ── 2. Pose check ────────────────────────────────────────────────────────
    center_max_yaw = float(settings.ACTIVE_LIVENESS_CENTER_MAX_YAW)
    side_min_yaw   = float(settings.ACTIVE_LIVENESS_SIDE_MIN_YAW)

    if step == "center":
        if abs(yaw) > center_max_yaw:
            return FrameValidationResponse(
                passed=False, step=step, code="WRONG_POSE",
                reason=(
                    f"Hãy nhìn thẳng vào camera. "
                    f"Đầu đang lệch {'trái' if yaw > 0 else 'phải'} quá nhiều "
                    f"(yaw={yaw:.3f}, tối đa={center_max_yaw})."
                ),
                yaw_score=yaw, pitch_score=pitch,
            )

    elif step in ("left", "right"):
        required_side_yaw = max(
            side_min_yaw,
            float(settings.ACTIVE_LIVENESS_MIN_YAW_RANGE) / 2,
        )
        yaw_delta = abs(yaw - float(reference_yaw))
        if abs(yaw) < required_side_yaw or yaw_delta < required_side_yaw:
            dir_label = "trái" if step == "left" else "phải"
            return FrameValidationResponse(
                passed=False, step=step, code="WRONG_POSE",
                reason=(
                    f"Hãy quay đầu sang {dir_label} rõ hơn. "
                    f"Góc hiện tại chưa đủ (yaw={yaw:.3f}, yêu cầu≥{required_side_yaw:.3f})."
                ),
                yaw_score=yaw, pitch_score=pitch,
                reference_yaw=reference_yaw,
                reference_pitch=reference_pitch,
            )
        if step == "right":
            if opposite_image is None:
                return FrameValidationResponse(
                    passed=False,
                    step=step,
                    code="MISSING_OPPOSITE_FRAME",
                    reason="Thiếu ảnh quay ngang trước đó. Vui lòng quét lại từ bước quay trái.",
                    yaw_score=yaw,
                    pitch_score=pitch,
                    reference_yaw=reference_yaw,
                    reference_pitch=reference_pitch,
                )
            try:
                opposite = await _read_image(opposite_image, "ảnh quay ngang trước đó")
                opposite_analysis = analyze_face_frame(
                    opposite,
                    "ảnh quay ngang trước đó",
                )
                opposite_yaw = float(opposite_analysis["yaw_score"])
            except (HTTPException, ValueError) as exc:
                reason = exc.detail if isinstance(exc, HTTPException) else str(exc)
                return FrameValidationResponse(
                    passed=False,
                    step=step,
                    code="INVALID_OPPOSITE_FRAME",
                    reason=f"Ảnh quay ngang trước đó không hợp lệ: {reason}",
                    yaw_score=yaw,
                    pitch_score=pitch,
                    reference_yaw=reference_yaw,
                    reference_pitch=reference_pitch,
                )
            yaw_range = abs(yaw - opposite_yaw)
            if yaw * opposite_yaw >= 0 or yaw_range < float(
                settings.ACTIVE_LIVENESS_MIN_YAW_RANGE
            ):
                return FrameValidationResponse(
                    passed=False,
                    step=step,
                    code="WRONG_POSE",
                    reason=(
                        "Hai bước quay ngang chưa ngược hướng hoặc biên độ chưa đủ. "
                        "Hãy quay đầu sang phía đối diện rõ hơn."
                    ),
                    yaw_score=yaw,
                    pitch_score=pitch,
                    reference_yaw=reference_yaw,
                    reference_pitch=reference_pitch,
                )

    elif step in ("up", "down"):
        delta = pitch - float(reference_pitch)
        required_pitch_delta = max(
            float(settings.ACTIVE_LIVENESS_VERTICAL_MIN_DELTA),
            float(settings.ACTIVE_LIVENESS_MIN_PITCH_RANGE) / 2,
        )
        if step == "up" and delta > -required_pitch_delta:
            return FrameValidationResponse(
                passed=False, step=step, code="WRONG_POSE",
                reason=(
                    f"Hãy ngẩng đầu nhìn lên cao hơn "
                    f"(độ thay đổi={delta:.3f}, yêu cầu≤{-required_pitch_delta:.3f})."
                ),
                yaw_score=yaw, pitch_score=pitch,
                reference_yaw=reference_yaw,
                reference_pitch=reference_pitch,
            )
        if step == "down" and delta < required_pitch_delta:
            return FrameValidationResponse(
                passed=False, step=step, code="WRONG_POSE",
                reason=(
                    f"Hãy cúi đầu nhìn xuống rõ hơn "
                    f"(độ thay đổi={delta:.3f}, yêu cầu≥{required_pitch_delta:.3f})."
                ),
                yaw_score=yaw, pitch_score=pitch,
                reference_yaw=reference_yaw,
                reference_pitch=reference_pitch,
            )

    # ── 3. Anti-spoofing on the center frame ────────────────────────────────
    liveness_score: float | None = None
    if step == "center":
        try:
            liveness_result = detect_liveness(image, label, analysis)
            liveness_score = float(liveness_result["liveness_score"])
            if not bool(liveness_result["liveness_passed"]):
                return FrameValidationResponse(
                    passed=False, step=step, code="SPOOF",
                    reason=(
                        "Không vượt qua kiểm tra người thật. "
                        "Đảm bảo đủ ánh sáng, không dùng ảnh in hoặc màn hình."
                    ),
                    yaw_score=yaw, pitch_score=pitch,
                    liveness_score=liveness_score,
                )
        except Exception as exc:
            return FrameValidationResponse(
                passed=False,
                step=step,
                code="LIVENESS_ERROR",
                reason=f"Không thể kiểm tra người thật ở frame này: {exc}",
                yaw_score=yaw,
                pitch_score=pitch,
            )

    # ── 4. Same-person check against the accepted center frame ──────────────
    face_distance: float | None = None
    face_threshold: float | None = None
    if reference_analysis is not None:
        try:
            reference_embedding = _extract_embedding(
                reference,
                "ảnh chính diện tham chiếu",
                reference_analysis,
            )
            candidate_embedding = _extract_embedding(
                image,
                label,
                analysis,
            )
            face_result = compare_embeddings(
                reference_embedding,
                candidate_embedding,
                threshold_override=float(
                    settings.FACE_ENROLLMENT_CONSISTENCY_THRESHOLD
                ),
            )
            face_distance = float(face_result["distance"])
            face_threshold = float(face_result["threshold"])
            if not bool(face_result["matched"]):
                return FrameValidationResponse(
                    passed=False,
                    step=step,
                    code="DIFFERENT_FACE",
                    reason=(
                        "Khuôn mặt ở bước này không đồng nhất với ảnh chính diện. "
                        "Vui lòng chỉ để một người thực hiện toàn bộ quá trình."
                    ),
                    yaw_score=yaw,
                    pitch_score=pitch,
                    reference_yaw=reference_yaw,
                    reference_pitch=reference_pitch,
                    face_distance=face_distance,
                    face_threshold=face_threshold,
                )
        except HTTPException as exc:
            return FrameValidationResponse(
                passed=False,
                step=step,
                code="FACE_COMPARE_ERROR",
                reason=str(exc.detail),
                yaw_score=yaw,
                pitch_score=pitch,
                reference_yaw=reference_yaw,
                reference_pitch=reference_pitch,
            )

    # ── 5. Pass ──────────────────────────────────────────────────────────────
    dir_labels = {
        "center": "chính diện", "left": "quay trái",
        "right": "quay phải", "up": "nhìn lên", "down": "nhìn xuống",
    }
    return FrameValidationResponse(
        passed=True, step=step, code="PASS",
        reason=f"Tư thế {dir_labels[step]} hợp lệ.",
        yaw_score=yaw, pitch_score=pitch,
        reference_yaw=reference_yaw,
        reference_pitch=reference_pitch,
        liveness_score=liveness_score,
        face_distance=face_distance,
        face_threshold=face_threshold,
    )


@router.post(
    "/enroll",
    response_model=FaceEnrollmentResponse,
    summary="Đăng ký khuôn mặt nhiều góc kèm liveness",
    description=(
        "Kiểm tra active liveness bằng năm tư thế, anti-spoofing trên ảnh chính diện, "
        "xác nhận các frame thuộc cùng một người rồi tạo face template Facenet512 tổng hợp."
    ),
)
async def enroll_face(
    selfie_image: Annotated[
        UploadFile,
        File(description="Ảnh selfie chính diện của khách"),
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
) -> FaceEnrollmentResponse:
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

    logger.info(
        "[enroll] active_liveness result: passed=%s, yaw_range=%.3f, pitch_range=%.3f, "
        "center_yaw=%.3f, left_yaw=%.3f, right_yaw=%.3f, up_pitch=%.3f, down_pitch=%.3f | "
        "center_ok=%s, sides_ok=%s, opposite=%s, range_ok=%s, vertical_ok=%s, pitch_ok=%s | "
        "fail_reason=%s",
        active_liveness["active_liveness_passed"],
        active_liveness["yaw_range"],
        active_liveness["pitch_range"],
        active_liveness["center_yaw"],
        active_liveness["first_turn_yaw"],
        active_liveness["second_turn_yaw"],
        active_liveness["up_pitch"],
        active_liveness["down_pitch"],
        active_liveness.get("center_ok"),
        active_liveness.get("sides_ok"),
        active_liveness.get("opposite_sides"),
        active_liveness.get("range_ok"),
        active_liveness.get("vertical_moves_ok"),
        active_liveness.get("pitch_range_ok"),
        active_liveness.get("fail_reason"),
    )

    if not bool(active_liveness["active_liveness_passed"]):
        fail_reason = active_liveness.get("fail_reason") or (
            "Không vượt qua active liveness khi đăng ký. "
            "Hãy thực hiện rõ từng tư thế nhìn thẳng, trái, phải, lên và xuống."
        )
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=fail_reason,
        )

    passive_liveness = _detect_liveness(
        selfie,
        "ảnh chính diện",
        center_analysis,
    )
    if not bool(passive_liveness["liveness_passed"]):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=(
                "Không vượt qua kiểm tra chống giả mạo khi đăng ký. "
                "Không sử dụng ảnh in, video hoặc màn hình."
            ),
        )

    embeddings = [
        _extract_embedding(selfie, "ảnh chính diện", center_analysis),
        _extract_embedding(left, "ảnh quay trái", left_analysis),
        _extract_embedding(right, "ảnh quay phải", right_analysis),
        _extract_embedding(up, "ảnh nhìn lên", up_analysis),
        _extract_embedding(down, "ảnh nhìn xuống", down_analysis),
    ]
    try:
        face_template = build_multi_angle_face_template(embeddings)
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(exc),
        ) from exc

    angle_embeddings = [
        {
            "pose": "center",
            "embedding": embeddings[0],
            "yaw_score": center_analysis["yaw_score"],
            "pitch_score": center_analysis["pitch_score"],
            "liveness_score": passive_liveness.get("liveness_score"),
            "available": True,
        },
        {
            "pose": "left",
            "embedding": embeddings[1],
            "yaw_score": left_analysis["yaw_score"],
            "pitch_score": left_analysis["pitch_score"],
            "available": True,
        },
        {
            "pose": "right",
            "embedding": embeddings[2],
            "yaw_score": right_analysis["yaw_score"],
            "pitch_score": right_analysis["pitch_score"],
            "available": True,
        },
        {
            "pose": "up",
            "embedding": embeddings[3],
            "yaw_score": up_analysis["yaw_score"],
            "pitch_score": up_analysis["pitch_score"],
            "available": True,
        },
        {
            "pose": "down",
            "embedding": embeddings[4],
            "yaw_score": down_analysis["yaw_score"],
            "pitch_score": down_analysis["pitch_score"],
            "available": True,
        },
    ]

    return FaceEnrollmentResponse(
        enrolled=True,
        liveness_passed=True,
        active_liveness_passed=True,
        message="Đăng ký liveness và khuôn mặt nhiều góc thành công.",
        model_used=settings.DEEPFACE_MODEL,
        detector_used=settings.FACE_RECOGNITION_DETECTOR,
        embedding=face_template,
        angle_embeddings=angle_embeddings,
    )


@router.post(
    "/verify",
    response_model=FaceVerificationResponse,
    summary="Xác minh khuôn mặt đã đăng ký",
    description=(
        "Nhận frame chính diện và một frame thử thách quay trái hoặc phải "
        "để kiểm tra active liveness nhanh. "
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
    challenge_image: Annotated[
        UploadFile,
        File(description="Ảnh sau khi khách thực hiện hướng quay được yêu cầu"),
    ],
    challenge_direction: Annotated[
        str,
        Form(description="Hướng thử thách do giao diện chọn: left hoặc right"),
    ],
    challenge_image_2: Annotated[
        UploadFile | None,
        File(description="Frame bổ sung thứ hai khi khách quay đầu"),
    ] = None,
    challenge_image_3: Annotated[
        UploadFile | None,
        File(description="Frame bổ sung thứ ba khi khách quay đầu"),
    ] = None,
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
    challenge_direction = challenge_direction.strip().lower()
    if challenge_direction not in {"left", "right"}:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="challenge_direction phải là left hoặc right.",
        )
    challenge_frames = [
        await _read_image(
            challenge_image,
            f"ảnh quay {challenge_direction} số 1",
        ),
    ]
    if challenge_image_2 is not None:
        challenge_frames.append(
            await _read_image(
                challenge_image_2,
                f"ảnh quay {challenge_direction} số 2",
            )
        )
    if challenge_image_3 is not None:
        challenge_frames.append(
            await _read_image(
                challenge_image_3,
                f"ảnh quay {challenge_direction} số 3",
            )
        )

    try:
        center_analysis = analyze_face_frame(selfie, "ảnh chính diện")
        challenge_candidates = []
        last_challenge_error = None
        for index, challenge_frame in enumerate(challenge_frames, start=1):
            try:
                challenge_analysis = analyze_face_frame(
                    challenge_frame,
                    f"ảnh quay {challenge_direction} số {index}",
                )
                challenge_candidates.append(
                    {
                        "frame": challenge_frame,
                        "analysis": challenge_analysis,
                        "liveness": validate_quick_active_liveness(
                            center_analysis,
                            challenge_analysis,
                            challenge_direction,
                        ),
                    }
                )
            except ValueError as exc:
                last_challenge_error = exc

        if not challenge_candidates:
            raise last_challenge_error or ValueError(
                "Không phân tích được các frame quay đầu."
            )

        selected_challenge = max(
            challenge_candidates,
            key=lambda candidate: (
                bool(candidate["liveness"]["active_liveness_passed"]),
                float(candidate["liveness"]["yaw_delta"]),
            ),
        )
        active_liveness = selected_challenge["liveness"]
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
        if not active_passed:
            if not bool(active_liveness["center_ok"]):
                message = (
                    "Ảnh đầu tiên chưa đủ chính diện. "
                    "Hãy nhìn thẳng vào camera và giữ đầu cân bằng trước khi hệ thống chụp."
                )
            elif not bool(active_liveness["side_ok"]) or not bool(
                active_liveness["delta_ok"]
            ):
                message = (
                    "Góc quay đầu chưa đủ rõ. "
                    "Hãy quay cả đầu sang một bên, không chỉ liếc mắt, rồi giữ nguyên đến khi chụp."
                )
            else:
                message = (
                    "Không ghi nhận được chuyển động khuôn mặt rõ ràng. "
                    "Vui lòng nhìn thẳng rồi quay cả đầu sang một bên."
                )
        else:
            message = (
                "Không vượt qua kiểm tra chống giả mạo. "
                "Vui lòng dùng khuôn mặt thật trước camera, không dùng ảnh hoặc màn hình."
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
            tta_real_votes=liveness.get("tta_real_votes"),
            tta_total_frames=liveness.get("tta_total_frames"),
            center_yaw=float(active_liveness["center_yaw"]),
            first_turn_yaw=float(active_liveness["challenge_yaw"]),
            second_turn_yaw=None,
            center_pitch=None,
            up_pitch=None,
            down_pitch=None,
            message=message,
            distance=None,
            threshold=float(settings.FACE_MATCH_THRESHOLD),
            similarity_percentage=None,
            metric=settings.DEEPFACE_DISTANCE,
            model_used=settings.DEEPFACE_MODEL,
            detector_used=settings.FACE_RECOGNITION_DETECTOR,
        )

    selfie_embedding = _extract_embedding(
        selfie,
        "ảnh chính diện",
        center_analysis,
    )
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
        tta_real_votes=liveness.get("tta_real_votes"),
        tta_total_frames=liveness.get("tta_total_frames"),
        center_yaw=float(active_liveness["center_yaw"]),
        first_turn_yaw=float(active_liveness["challenge_yaw"]),
        second_turn_yaw=None,
        center_pitch=None,
        up_pitch=None,
        down_pitch=None,
        message=(
            "Liveness hợp lệ và khuôn mặt khớp với dữ liệu đã đăng ký."
            if matched
            else (
                "Check-in bị từ chối: khuôn mặt hiện tại không khớp "
                "với khuôn mặt đã đăng ký eKYC."
            )
        ),
        distance=float(result["distance"]),
        threshold=float(result["threshold"]),
        similarity_percentage=result["similarity_percentage"],
        metric=settings.DEEPFACE_DISTANCE,
        model_used=settings.DEEPFACE_MODEL,
        detector_used=settings.FACE_RECOGNITION_DETECTOR,
    )
