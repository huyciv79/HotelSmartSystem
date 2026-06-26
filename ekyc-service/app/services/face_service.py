"""Face detection, embedding extraction and matching logic."""

import json
from io import BytesIO
from typing import Any

import cv2
import numpy as np
from deepface import DeepFace
from PIL import Image, UnidentifiedImageError
from retinaface import RetinaFace

from app.core.config import settings

MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024
EXPECTED_EMBEDDING_SIZE = 512
LIVENESS_TTA_N = 10  # số biến thể TTA cho liveness detection
LIVENESS_FAST_PATH_FRAMES = 5


def warm_face_models() -> None:
    """Load singleton models during service startup instead of the first scan."""
    RetinaFace.build_model()
    DeepFace.build_model(settings.DEEPFACE_MODEL)
    DeepFace.build_model("Fasnet", task="spoofing")


def decode_image(image_bytes: bytes, image_name: str) -> np.ndarray:
    """Validate uploaded bytes and convert them to an RGB numpy array."""
    if not image_bytes:
        raise ValueError(f"{image_name} không có dữ liệu.")
    if len(image_bytes) > MAX_IMAGE_SIZE_BYTES:
        raise ValueError(f"{image_name} vượt quá giới hạn 10 MB.")

    try:
        with Image.open(BytesIO(image_bytes)) as image:
            image.load()
            rgb_image = image.convert("RGB")
    except (UnidentifiedImageError, OSError, ValueError) as exc:
        raise ValueError(
            f"{image_name} không phải file ảnh JPG/PNG/WebP hợp lệ."
        ) from exc

    width, height = rgb_image.size
    if width < 80 or height < 80:
        raise ValueError(
            f"{image_name} quá nhỏ ({width}x{height}). Kích thước tối thiểu là 80x80."
        )

    return np.asarray(rgb_image)


def extract_single_face_embedding(
    image: np.ndarray,
    image_name: str,
    analysis: dict[str, Any] | None = None,
) -> list[float]:
    """Extract one embedding, reusing a prior face detection when available."""
    try:
        if analysis is None:
            bgr_image = image[:, :, ::-1].copy()
            detector_backend = settings.FACE_RECOGNITION_DETECTOR
            enforce_detection = True
            align = True
        else:
            bgr_image = analysis["bgr_image"]
            x1, y1, x2, y2 = analysis["facial_area"]
            height, width = bgr_image.shape[:2]
            face_width = x2 - x1
            face_height = y2 - y1
            margin_x = max(12, int(face_width * 0.20))
            margin_y = max(12, int(face_height * 0.20))
            crop_x1 = max(0, x1 - margin_x)
            crop_y1 = max(0, y1 - margin_y)
            crop_x2 = min(width, x2 + margin_x)
            crop_y2 = min(height, y2 + margin_y)
            bgr_image = bgr_image[crop_y1:crop_y2, crop_x1:crop_x2]
            if bgr_image.size == 0:
                raise ValueError(f"Không thể cắt khuôn mặt trong {image_name}.")
            detector_backend = "skip"
            enforce_detection = False
            align = False

        representations: list[dict[str, Any]] = DeepFace.represent(
            img_path=bgr_image,
            model_name=settings.DEEPFACE_MODEL,
            detector_backend=detector_backend,
            enforce_detection=enforce_detection,
            align=align,
        )
        if not representations:
            raise ValueError(f"Không tìm thấy khuôn mặt trong {image_name}.")
        if len(representations) > 1:
            raise ValueError(
                f"Phát hiện {len(representations)} khuôn mặt trong {image_name}. "
                "Vui lòng chỉ sử dụng ảnh có một người."
            )
        embedding = representations[0].get("embedding")
        if not embedding or len(embedding) != EXPECTED_EMBEDDING_SIZE:
            raise ValueError(
                f"Embedding của {image_name} phải có {EXPECTED_EMBEDDING_SIZE} chiều."
            )
        return [round(float(value), 8) for value in embedding]
    except ValueError as exc:
        if "Face could not be detected" in str(exc):
            raise ValueError(
                f"Không tìm thấy khuôn mặt trong {image_name}. "
                "Hãy đưa khuôn mặt gần camera hơn và bảo đảm đủ ánh sáng."
            ) from exc
        raise


def analyze_face_frame(
    image: np.ndarray,
    image_name: str,
) -> dict[str, Any]:
    """Detect one face once and retain landmarks for pose, spoofing and embedding."""
    bgr_image = image[:, :, ::-1].copy()
    detections = RetinaFace.detect_faces(
        bgr_image,
        model=RetinaFace.build_model(),
        allow_upscaling=False,
    )
    if not isinstance(detections, dict) or not detections:
        raise ValueError(
            f"Không tìm thấy khuôn mặt trong {image_name}. "
            "Hãy đưa khuôn mặt gần camera hơn và bảo đảm đủ ánh sáng."
        )
    if len(detections) > 1:
        raise ValueError(
            f"Phát hiện {len(detections)} khuôn mặt trong {image_name}. "
            "Vui lòng chỉ để một người trước camera."
        )

    detection = next(iter(detections.values()))
    landmarks = detection["landmarks"]
    left_eye = np.asarray(landmarks["left_eye"], dtype=np.float64)
    right_eye = np.asarray(landmarks["right_eye"], dtype=np.float64)
    nose = np.asarray(landmarks["nose"], dtype=np.float64)
    mouth_left = np.asarray(landmarks["mouth_left"], dtype=np.float64)
    mouth_right = np.asarray(landmarks["mouth_right"], dtype=np.float64)
    eye_distance = float(np.linalg.norm(left_eye - right_eye))
    if eye_distance <= 1:
        raise ValueError(f"Không thể xác định hướng khuôn mặt trong {image_name}.")

    eye_midpoint = (left_eye + right_eye) / 2
    mouth_midpoint = (mouth_left + mouth_right) / 2
    vertical_span = float(mouth_midpoint[1] - eye_midpoint[1])
    if vertical_span <= 1:
        raise ValueError(f"Không thể xác định hướng nhìn lên/xuống trong {image_name}.")

    yaw_score = float((nose[0] - eye_midpoint[0]) / eye_distance)
    pitch_score = float((nose[1] - eye_midpoint[1]) / vertical_span)

    return {
        "bgr_image": bgr_image,
        "facial_area": [int(value) for value in detection["facial_area"]],
        "landmarks": landmarks,
        "detection_score": float(detection["score"]),
        "yaw_score": round(yaw_score, 6),
        "pitch_score": round(pitch_score, 6),
    }


def check_face_readiness(
    image: np.ndarray,
    image_name: str = "ảnh kiểm tra",
) -> dict[str, Any]:
    """Check whether one sufficiently large, centered face is ready to scan."""
    bgr_image = image[:, :, ::-1].copy()
    detections = RetinaFace.detect_faces(
        bgr_image,
        model=RetinaFace.build_model(),
        allow_upscaling=False,
    )
    if not isinstance(detections, dict) or not detections:
        return {
            "ready": False,
            "reason": "Không tìm thấy khuôn mặt. Hãy đứng gần camera và bảo đảm đủ ánh sáng.",
            "code": "NO_FACE",
            "face_count": 0,
        }

    face_count = len(detections)
    if face_count > 1:
        return {
            "ready": False,
            "reason": (
                f"Phát hiện {face_count} khuôn mặt. "
                "Vui lòng chỉ để một người đứng trước camera."
            ),
            "code": "MULTIPLE_FACES",
            "face_count": face_count,
        }

    detection = next(iter(detections.values()))
    x1, y1, x2, y2 = [int(value) for value in detection["facial_area"]]
    face_width = max(0, x2 - x1)
    face_height = max(0, y2 - y1)
    min_size = int(settings.FACE_READINESS_MIN_SIZE)
    if face_width < min_size or face_height < min_size:
        return {
            "ready": False,
            "reason": "Khuôn mặt đang quá nhỏ. Hãy tiến gần camera hơn.",
            "code": "FACE_TOO_SMALL",
            "face_count": 1,
            "face_width": face_width,
            "face_height": face_height,
        }

    landmarks = detection["landmarks"]
    left_eye = np.asarray(landmarks["left_eye"], dtype=np.float64)
    right_eye = np.asarray(landmarks["right_eye"], dtype=np.float64)
    nose = np.asarray(landmarks["nose"], dtype=np.float64)
    eye_distance = float(np.linalg.norm(left_eye - right_eye))
    if eye_distance <= 1:
        return {
            "ready": False,
            "reason": f"Không thể xác định hướng khuôn mặt trong {image_name}.",
            "code": "INVALID_FACE_GEOMETRY",
            "face_count": 1,
            "face_width": face_width,
            "face_height": face_height,
        }

    eye_midpoint = (left_eye + right_eye) / 2
    yaw = float((nose[0] - eye_midpoint[0]) / eye_distance)
    if abs(yaw) > float(settings.ACTIVE_LIVENESS_CENTER_MAX_YAW):
        return {
            "ready": False,
            "reason": "Hãy nhìn thẳng vào camera trước khi bắt đầu quét.",
            "code": "NOT_CENTERED",
            "face_count": 1,
            "face_width": face_width,
            "face_height": face_height,
            "yaw": round(yaw, 6),
        }

    return {
        "ready": True,
        "reason": "Đã nhận diện đúng một khuôn mặt ở vị trí phù hợp.",
        "code": "READY",
        "face_count": 1,
        "face_width": face_width,
        "face_height": face_height,
        "yaw": round(yaw, 6),
    }


def _augment_for_liveness_bgr(bgr: np.ndarray) -> list[np.ndarray]:
    """Create LIVENESS_TTA_N photometric/geometric BGR variants for TTA.

    All variants keep the spatial layout nearly identical so that the
    pre-detected facial_area coordinates remain valid without re-running
    RetinaFace, making inference fast.
    """
    h, w = bgr.shape[:2]
    variants: list[np.ndarray] = []

    # 1. Original
    variants.append(bgr)

    # 2. Brightness +18 %
    variants.append(np.clip(bgr.astype(np.float32) * 1.18, 0, 255).astype(np.uint8))

    # 3. Brightness -18 %
    variants.append(np.clip(bgr.astype(np.float32) * 0.82, 0, 255).astype(np.uint8))

    # 4. Brightness +35 % (overexposed simulation)
    variants.append(np.clip(bgr.astype(np.float32) * 1.35, 0, 255).astype(np.uint8))

    # 5. Contrast enhanced (stretch around mid-gray)
    mean_val = float(bgr.mean())
    variants.append(
        np.clip((bgr.astype(np.float32) - mean_val) * 1.25 + mean_val, 0, 255).astype(np.uint8)
    )

    # 6. Contrast reduced
    variants.append(
        np.clip((bgr.astype(np.float32) - mean_val) * 0.75 + mean_val, 0, 255).astype(np.uint8)
    )

    # 7. Gamma correction 0.75 (darker mid-tones)
    lut_dark = np.array(
        [int(((i / 255.0) ** 0.75) * 255) for i in range(256)], dtype=np.uint8
    )
    variants.append(cv2.LUT(bgr, lut_dark))

    # 8. Gamma correction 1.30 (lighter mid-tones)
    lut_light = np.array(
        [int(((i / 255.0) ** 1.30) * 255) for i in range(256)], dtype=np.uint8
    )
    variants.append(cv2.LUT(bgr, lut_light))

    # 9. Gaussian blur (simulate slight de-focus / compression artefacts)
    variants.append(cv2.GaussianBlur(bgr, (3, 3), 0))

    # 10. Center crop 92 % → resize back (simulate slight zoom)
    cx, cy = w // 2, h // 2
    cw, ch = int(w * 0.92), int(h * 0.92)
    x1c, y1c = max(0, cx - cw // 2), max(0, cy - ch // 2)
    crop = bgr[y1c : y1c + ch, x1c : x1c + cw]
    variants.append(cv2.resize(crop, (w, h), interpolation=cv2.INTER_LINEAR))

    return variants[:LIVENESS_TTA_N]


def detect_liveness(
    image: np.ndarray,
    image_name: str,
    analysis: dict[str, Any] | None = None,
) -> dict[str, float | bool | str]:
    """Detect whether the captured face is a live person using TTA (10 variants).

    Strategy
    --------
    * RetinaFace runs **once** on the original frame.
    * MiniFASNet runs on all LIVENESS_TTA_N augmented BGR variants, reusing
      the same facial_area coordinates (augmentations are spatially conservative).
    * Liveness passes when strict majority of variants vote "real" (> N/2) AND
      the average confidence score meets LIVENESS_MIN_SCORE.
    """
    try:
        face_analysis = analysis or analyze_face_frame(image, image_name)
        x1, y1, x2, y2 = face_analysis["facial_area"]
        facial_area_wh = (x1, y1, x2 - x1, y2 - y1)

        spoof_model = DeepFace.build_model("Fasnet", task="spoofing")
        bgr_variants = _augment_for_liveness_bgr(face_analysis["bgr_image"])

        real_votes: int = 0
        score_sum: float = 0.0
        evaluated_variants = bgr_variants[:LIVENESS_FAST_PATH_FRAMES]
        for bgr_aug in evaluated_variants:
            is_real_v, raw_score_v = spoof_model.analyze(
                img=bgr_aug,
                facial_area=facial_area_wh,
            )
            if bool(is_real_v):
                real_votes += 1
            score_sum += float(raw_score_v)

        min_score = float(settings.LIVENESS_MIN_SCORE)
        fast_count = len(evaluated_variants)
        fast_avg_score = score_sum / fast_count
        fast_passed = (
            real_votes == fast_count
            and fast_avg_score >= min_score
        )

        if not fast_passed:
            for bgr_aug in bgr_variants[fast_count:]:
                is_real_v, raw_score_v = spoof_model.analyze(
                    img=bgr_aug,
                    facial_area=facial_area_wh,
                )
                if bool(is_real_v):
                    real_votes += 1
                score_sum += float(raw_score_v)
            evaluated_variants = bgr_variants

        n = len(evaluated_variants)
        avg_score = round(score_sum / n, 6)
        majority_real = real_votes > n / 2
        passed = majority_real and avg_score >= min_score

        return {
            "liveness_passed": passed,
            "is_real": majority_real,
            "liveness_score": avg_score,
            "liveness_threshold": min_score,
            "anti_spoofing_model": f"MiniFASNet-TTA{n}",
            "tta_real_votes": real_votes,
            "tta_total_frames": n,
            "fast_path": fast_passed,
        }
    except ValueError:
        raise
    except Exception as exc:
        raise RuntimeError(
            f"Lỗi kiểm tra liveness trong {image_name}: {exc}"
        ) from exc


def validate_active_liveness(
    center: dict[str, Any],
    first_turn: dict[str, Any],
    second_turn: dict[str, Any],
    look_up: dict[str, Any],
    look_down: dict[str, Any],
) -> dict[str, float | bool]:
    """Require center, opposite horizontal turns and opposite vertical movements."""
    center_yaw = float(center["yaw_score"])
    first_yaw = float(first_turn["yaw_score"])
    second_yaw = float(second_turn["yaw_score"])
    center_pitch = float(center["pitch_score"])
    up_pitch = float(look_up["pitch_score"])
    down_pitch = float(look_down["pitch_score"])

    center_ok = abs(center_yaw) <= float(settings.ACTIVE_LIVENESS_CENTER_MAX_YAW)
    sides_ok = (
        abs(first_yaw) >= float(settings.ACTIVE_LIVENESS_SIDE_MIN_YAW)
        and abs(second_yaw) >= float(settings.ACTIVE_LIVENESS_SIDE_MIN_YAW)
    )
    opposite_sides = first_yaw * second_yaw < 0
    yaw_range = abs(first_yaw - second_yaw)
    range_ok = yaw_range >= float(settings.ACTIVE_LIVENESS_MIN_YAW_RANGE)

    up_delta = up_pitch - center_pitch
    down_delta = down_pitch - center_pitch
    vertical_min_delta = float(settings.ACTIVE_LIVENESS_VERTICAL_MIN_DELTA)
    vertical_moves_ok = (
        abs(up_delta) >= vertical_min_delta
        and abs(down_delta) >= vertical_min_delta
        and up_delta * down_delta < 0
    )
    pitch_range = abs(up_pitch - down_pitch)
    pitch_range_ok = pitch_range >= float(settings.ACTIVE_LIVENESS_MIN_PITCH_RANGE)

    passed = (
        center_ok
        and sides_ok
        and opposite_sides
        and range_ok
        and vertical_moves_ok
        and pitch_range_ok
    )

    # Build a human-readable diagnostic message so Spring Boot can surface
    # the exact failure reason to the frontend instead of a generic message.
    if not passed:
        if not center_ok:
            fail_reason = (
                f"Ảnh chính diện chưa đủ thẳng (yaw={center_yaw:.3f}, "
                f"tối đa cho phép={settings.ACTIVE_LIVENESS_CENTER_MAX_YAW}). "
                "Hãy nhìn thẳng vào camera trước khi bắt đầu."
            )
        elif not sides_ok:
            fail_reason = (
                f"Góc quay trái/phải chưa đủ rõ "
                f"(trái={first_yaw:.3f}, phải={second_yaw:.3f}, "
                f"yêu cầu >={settings.ACTIVE_LIVENESS_SIDE_MIN_YAW}). "
                "Hãy quay cả đầu sang mỗi bên rõ hơn."
            )
        elif not opposite_sides:
            fail_reason = (
                f"Hai ảnh quay không ngược chiều nhau "
                f"(trái={first_yaw:.3f}, phải={second_yaw:.3f}). "
                "Hãy chắc chắn một ảnh quay trái và một ảnh quay phải."
            )
        elif not range_ok:
            fail_reason = (
                f"Biên độ quay trái/phải quá nhỏ (range={yaw_range:.3f}, "
                f"yêu cầu>={settings.ACTIVE_LIVENESS_MIN_YAW_RANGE}). "
                "Hãy quay đầu rộng hơn sang mỗi bên."
            )
        elif not vertical_moves_ok:
            fail_reason = (
                f"Chuyển động nhìn lên/xuống chưa đủ rõ "
                f"(up_delta={up_pitch - center_pitch:.3f}, down_delta={down_pitch - center_pitch:.3f}, "
                f"yêu cầu>={settings.ACTIVE_LIVENESS_VERTICAL_MIN_DELTA}). "
                "Hãy ngẩng đầu lên và cúi đầu xuống rõ hơn."
            )
        else:
            fail_reason = (
                f"Biên độ nhìn lên/xuống quá nhỏ (pitch_range={pitch_range:.3f}, "
                f"yêu cầu>={settings.ACTIVE_LIVENESS_MIN_PITCH_RANGE})."
            )
    else:
        fail_reason = None

    return {
        "active_liveness_passed": passed,
        "center_yaw": round(center_yaw, 6),
        "first_turn_yaw": round(first_yaw, 6),
        "second_turn_yaw": round(second_yaw, 6),
        "yaw_range": round(yaw_range, 6),
        "center_pitch": round(center_pitch, 6),
        "up_pitch": round(up_pitch, 6),
        "down_pitch": round(down_pitch, 6),
        "pitch_range": round(pitch_range, 6),
        # Diagnostic fields
        "center_ok": center_ok,
        "sides_ok": sides_ok,
        "opposite_sides": opposite_sides,
        "range_ok": range_ok,
        "vertical_moves_ok": vertical_moves_ok,
        "pitch_range_ok": pitch_range_ok,
        "fail_reason": fail_reason,
    }


def validate_quick_active_liveness(
    center: dict[str, Any],
    challenge: dict[str, Any],
    challenge_direction: str,
) -> dict[str, float | bool]:
    """Validate a short center-to-side movement for express check-in."""
    center_yaw = float(center["yaw_score"])
    challenge_yaw = float(challenge["yaw_score"])
    center_max_yaw = float(settings.EXPRESS_LIVENESS_CENTER_MAX_YAW)
    side_min_yaw = float(settings.EXPRESS_LIVENESS_SIDE_MIN_YAW)
    min_yaw_delta = float(settings.EXPRESS_LIVENESS_MIN_YAW_DELTA)
    center_ok = abs(center_yaw) <= center_max_yaw
    side_ok = abs(challenge_yaw) >= side_min_yaw
    yaw_delta = abs(challenge_yaw - center_yaw)
    delta_ok = yaw_delta >= min_yaw_delta
    direction_ok = (
        challenge_yaw > 0
        if challenge_direction == "left"
        else challenge_yaw < 0
    )

    return {
        # Camera selfie previews and raw canvas frames may use opposite horizontal
        # conventions. Require a clear turn, but do not reject a genuine movement
        # solely because its yaw sign is mirrored.
        "active_liveness_passed": center_ok and side_ok and delta_ok,
        "center_ok": center_ok,
        "side_ok": side_ok,
        "delta_ok": delta_ok,
        "center_yaw": round(center_yaw, 6),
        "challenge_yaw": round(challenge_yaw, 6),
        "yaw_delta": round(yaw_delta, 6),
        "direction_ok": direction_ok,
        "center_max_yaw": center_max_yaw,
        "side_min_yaw": side_min_yaw,
        "min_yaw_delta": min_yaw_delta,
    }


def build_multi_angle_face_template(
    embeddings: list[list[float]],
) -> list[float]:
    """Build one normalized 512-d template from consistent multi-angle samples."""
    if len(embeddings) < 2:
        raise ValueError("Cần ít nhất hai mẫu khuôn mặt để tạo face template.")

    vectors = np.asarray(embeddings, dtype=np.float64)
    if vectors.ndim != 2 or vectors.shape[1] != EXPECTED_EMBEDDING_SIZE:
        raise ValueError(
            f"Mỗi embedding phải có đúng {EXPECTED_EMBEDDING_SIZE} chiều."
        )
    if not np.isfinite(vectors).all():
        raise ValueError("Embedding khuôn mặt chứa giá trị không hợp lệ.")

    norms = np.linalg.norm(vectors, axis=1, keepdims=True)
    if np.any(norms == 0):
        raise ValueError("Không thể chuẩn hóa embedding có độ dài bằng 0.")
    normalized = vectors / norms

    reference = normalized[0]
    consistency_threshold = float(
        settings.FACE_ENROLLMENT_CONSISTENCY_THRESHOLD
    )
    for index, candidate in enumerate(normalized[1:], start=2):
        distance = 1 - float(np.dot(reference, candidate))
        if distance > consistency_threshold:
            raise ValueError(
                f"Khuôn mặt ở frame số {index} không đồng nhất với ảnh chính diện. "
                "Vui lòng chỉ để một người thực hiện toàn bộ quá trình."
            )

    template = normalized.mean(axis=0)
    template_norm = float(np.linalg.norm(template))
    if template_norm == 0:
        raise ValueError("Không thể tạo face template từ các frame đã cung cấp.")
    template /= template_norm
    return [round(float(value), 8) for value in template]


def parse_embedding(raw_embedding: str) -> list[float]:
    """Parse a JSON array sent by Spring Boot through multipart/form-data."""
    try:
        value = json.loads(raw_embedding)
    except json.JSONDecodeError as exc:
        raise ValueError(
            "registered_embedding phải là một JSON array hợp lệ."
        ) from exc

    if not isinstance(value, list):
        raise ValueError("registered_embedding phải là một JSON array.")
    if len(value) != EXPECTED_EMBEDDING_SIZE:
        raise ValueError(
            f"registered_embedding phải có đúng {EXPECTED_EMBEDDING_SIZE} phần tử."
        )

    try:
        embedding = [float(item) for item in value]
    except (TypeError, ValueError) as exc:
        raise ValueError(
            "Mọi phần tử trong registered_embedding phải là số."
        ) from exc

    if not np.isfinite(np.asarray(embedding)).all():
        raise ValueError("registered_embedding chứa giá trị không hợp lệ.")

    return embedding


def compare_embeddings(
    reference_embedding: list[float],
    candidate_embedding: list[float],
    threshold_override: float | None = None,
) -> dict[str, float | bool | None]:
    """Compare two embeddings using the distance metric from application settings."""
    reference = np.asarray(reference_embedding, dtype=np.float64)
    candidate = np.asarray(candidate_embedding, dtype=np.float64)

    if reference.shape != (EXPECTED_EMBEDDING_SIZE,):
        raise ValueError(
            f"Embedding tham chiếu phải có {EXPECTED_EMBEDDING_SIZE} phần tử."
        )
    if candidate.shape != (EXPECTED_EMBEDDING_SIZE,):
        raise ValueError(
            f"Embedding cần kiểm tra phải có {EXPECTED_EMBEDDING_SIZE} phần tử."
        )

    metric = settings.DEEPFACE_DISTANCE
    if metric == "cosine":
        denominator = np.linalg.norm(reference) * np.linalg.norm(candidate)
        if denominator == 0:
            raise ValueError("Không thể so sánh embedding có độ dài bằng 0.")
        distance = 1 - float(np.dot(reference, candidate) / denominator)
        similarity_percentage: float | None = max(
            0.0,
            min(100.0, (1.0 - distance) * 100.0),
        )
    elif metric == "euclidean":
        distance = float(np.linalg.norm(reference - candidate))
        similarity_percentage = None
    elif metric == "euclidean_l2":
        reference_norm = np.linalg.norm(reference)
        candidate_norm = np.linalg.norm(candidate)
        if reference_norm == 0 or candidate_norm == 0:
            raise ValueError("Không thể chuẩn hóa embedding có độ dài bằng 0.")
        distance = float(
            np.linalg.norm(
                (reference / reference_norm) - (candidate / candidate_norm)
            )
        )
        similarity_percentage = None
    else:
        raise ValueError(f"Metric không được hỗ trợ: {metric}.")

    distance = round(distance, 6)
    threshold = (
        float(threshold_override)
        if threshold_override is not None
        else float(settings.FACE_MATCH_THRESHOLD)
    )
    return {
        "matched": distance <= threshold,
        "distance": distance,
        "threshold": threshold,
        "similarity_percentage": (
            round(similarity_percentage, 2)
            if similarity_percentage is not None
            else None
        ),
    }
