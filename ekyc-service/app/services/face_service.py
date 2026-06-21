"""Face detection, embedding extraction and matching logic."""

import json
from io import BytesIO
from typing import Any

import numpy as np
from deepface import DeepFace
from PIL import Image, UnidentifiedImageError
from retinaface import RetinaFace

from app.core.config import settings

MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024
EXPECTED_EMBEDDING_SIZE = 512


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
) -> list[float]:
    """Extract one embedding without re-encoding the uploaded image as JPEG."""
    try:
        bgr_image = image[:, :, ::-1].copy()
        representations: list[dict[str, Any]] = DeepFace.represent(
            img_path=bgr_image,
            model_name=settings.DEEPFACE_MODEL,
            detector_backend=settings.FACE_RECOGNITION_DETECTOR,
            enforce_detection=True,
            align=True,
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


def detect_liveness(
    image: np.ndarray,
    image_name: str,
    analysis: dict[str, Any] | None = None,
) -> dict[str, float | bool | str]:
    """Detect whether the captured face is a live person instead of a spoof."""
    try:
        face_analysis = analysis or analyze_face_frame(image, image_name)
        x1, y1, x2, y2 = face_analysis["facial_area"]
        is_real, raw_score = DeepFace.build_model(
            "Fasnet",
            task="spoofing",
        ).analyze(
            img=face_analysis["bgr_image"],
            facial_area=(x1, y1, x2 - x1, y2 - y1),
        )
        is_real = bool(is_real)
        score = round(float(raw_score), 6)
        min_score = float(settings.LIVENESS_MIN_SCORE)
        passed = is_real and score >= min_score

        return {
            "liveness_passed": passed,
            "is_real": is_real,
            "liveness_score": score,
            "liveness_threshold": min_score,
            "anti_spoofing_model": "MiniFASNet",
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

    return {
        "active_liveness_passed": (
            center_ok
            and sides_ok
            and opposite_sides
            and range_ok
            and vertical_moves_ok
            and pitch_range_ok
        ),
        "center_yaw": round(center_yaw, 6),
        "first_turn_yaw": round(first_yaw, 6),
        "second_turn_yaw": round(second_yaw, 6),
        "yaw_range": round(yaw_range, 6),
        "center_pitch": round(center_pitch, 6),
        "up_pitch": round(up_pitch, 6),
        "down_pitch": round(down_pitch, 6),
        "pitch_range": round(pitch_range, 6),
    }


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
