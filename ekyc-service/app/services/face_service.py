"""Face detection, embedding extraction and matching logic."""

import json
import logging
import os
import tempfile
from io import BytesIO
from typing import Any

import numpy as np
from deepface import DeepFace
from PIL import Image, UnidentifiedImageError

from app.core.config import settings

logger = logging.getLogger(__name__)

MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024
EXPECTED_EMBEDDING_SIZE = 512


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
    """Extract exactly one 512-dimensional face embedding from an image."""
    temp_path: str | None = None
    try:
        with tempfile.NamedTemporaryFile(
            suffix=".jpg",
            delete=False,
            prefix="face_api_",
        ) as temp_file:
            Image.fromarray(image).save(temp_file.name, format="JPEG", quality=95)
            temp_path = temp_file.name

        representations: list[dict[str, Any]] = DeepFace.represent(
            img_path=temp_path,
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
        if not embedding:
            raise ValueError(f"Không thể tạo embedding từ {image_name}.")
        if len(embedding) != EXPECTED_EMBEDDING_SIZE:
            raise ValueError(
                f"Embedding của {image_name} có {len(embedding)} chiều; "
                f"API yêu cầu {EXPECTED_EMBEDDING_SIZE} chiều."
            )

        return [round(float(value), 8) for value in embedding]
    except ValueError as exc:
        message = str(exc)
        if "Face could not be detected" in message:
            raise ValueError(
                f"Không tìm thấy khuôn mặt trong {image_name}. "
                "Hãy dùng ảnh rõ nét, chính diện và đủ ánh sáng."
            ) from exc
        raise
    finally:
        if temp_path and os.path.exists(temp_path):
            try:
                os.remove(temp_path)
            except OSError:
                logger.warning("Không thể xóa file ảnh tạm: %s", temp_path)


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
    threshold = float(settings.FACE_MATCH_THRESHOLD)
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
