# 🛡️ eKYC AI Service

FastAPI microservice xác thực danh tính điện tử (eKYC) tích hợp:
- **DeepFace** – So sánh khuôn mặt & trích xuất vector 512 chiều
- **EasyOCR** – Đọc thông tin CCCD/CMND (hỗ trợ tiếng Việt)

---

## 📁 Cấu trúc thư mục

```
ekyc-service/
├── main.py                          # Entry point FastAPI
├── requirements.txt                 # Danh sách thư viện
├── .env.example                     # Mẫu biến môi trường
│
└── app/
    ├── core/
    │   └── config.py                # Cấu hình toàn ứng dụng
    │
    ├── api/
    │   └── v1/
    │       ├── router.py            # Router tổng hợp
    │       └── endpoints/
    │           └── ekyc.py          # Endpoint POST /api/v1/ai/verify-ekyc
    │
    ├── services/
    │   └── ekyc_service.py          # Business logic: Face + OCR
    │
    ├── schemas/
    │   └── ekyc_schema.py           # Pydantic models (Request/Response)
    │
    └── utils/
        └── image_utils.py           # Tải ảnh & parse thông tin CCCD
```

---

## ⚙️ Cài đặt

### 1. Tạo môi trường ảo

```bash
python -m venv .venv
# Windows
.venv\Scripts\activate
# Linux/macOS
source .venv/bin/activate
```

### 2. Cài thư viện

```bash
pip install -r requirements.txt
```

> **Lưu ý:** Lần đầu chạy, DeepFace sẽ tự động tải model **Facenet512** (~90MB) và EasyOCR sẽ tải model tiếng Việt. Cần kết nối Internet.

### 3. Cấu hình biến môi trường

```bash
cp .env.example .env
# Chỉnh sửa .env theo nhu cầu
```

---

## 🚀 Chạy server

```bash
# Development
python main.py

# Hoặc dùng uvicorn trực tiếp
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

Truy cập **Swagger UI**: http://localhost:8000/docs

---

## 📡 API Reference

### `POST /api/v1/ai/verify-ekyc`

**Request Body:**

```json
{
  "front_image_url": "https://example.com/cccd_front.jpg",
  "back_image_url":  "https://example.com/cccd_back.jpg",
  "face_image_url":  "https://example.com/selfie.jpg"
}
```

**Response thành công (200):**

```json
{
  "success": true,
  "message": "Xác thực thành công – khuôn mặt KHỚP.",
  "face_verification": {
    "is_matched": true,
    "confidence_score": 0.8731,
    "distance": 0.1269,
    "model_used": "Facenet512",
    "detector_used": "opencv"
  },
  "face_embedding": [0.01234567, -0.00987654, ...],  // 512 phần tử
  "ocr_info": {
    "id_number": "012345678901",
    "full_name": "NGUYỄN VĂN A",
    "dob": "01/01/1990",
    "raw_text": "CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM\n..."
  }
}
```

**Response lỗi không tìm thấy khuôn mặt (400):**

```json
{
  "detail": "Không tìm thấy khuôn mặt trong một hoặc cả hai ảnh. Hãy đảm bảo ảnh rõ nét và chứa khuôn mặt."
}
```

---

## 🔧 Cấu hình model

| Biến môi trường       | Mặc định      | Mô tả                                         |
|-----------------------|---------------|-----------------------------------------------|
| `DEEPFACE_MODEL`      | `Facenet512`  | Model face embedding (512-dim)                |
| `LIVENESS_MIN_SCORE`  | `0.80`        | Độ tin cậy tối thiểu để vượt qua anti-spoofing |
| `ACTIVE_LIVENESS_CENTER_MAX_YAW` | `0.12` | Độ lệch tối đa khi nhìn chính diện |
| `ACTIVE_LIVENESS_SIDE_MIN_YAW` | `0.13` | Độ lệch tối thiểu cho mỗi lần quay đầu |
| `ACTIVE_LIVENESS_VERTICAL_MIN_DELTA` | `0.04` | Độ lệch tối thiểu khi nhìn lên hoặc xuống |
| `ACTIVE_LIVENESS_MIN_PITCH_RANGE` | `0.10` | Khoảng cách tối thiểu giữa frame nhìn lên và nhìn xuống |
| `DEEPFACE_DETECTOR`   | `opencv`      | Face detector (`retinaface` chính xác hơn)    |
| `DEEPFACE_DISTANCE`   | `cosine`      | Metric đo khoảng cách vector                  |
| `FACE_MATCH_THRESHOLD`| `0.40`        | Ngưỡng cosine distance để xác nhận khớp       |
| `OCR_LANGUAGES`       | `["vi","en"]` | Ngôn ngữ EasyOCR                              |
| `OCR_GPU`             | `False`       | Bật CUDA cho EasyOCR                          |

### 💡 Model hỗ trợ embedding 512 chiều
- **`Facenet512`** ← *Được khuyến nghị* – Chính xác cao, cân bằng tốc độ/độ chính xác
- **`ArcFace`** – Chính xác cao hơn, chậm hơn
- **`DeepFace`** – Model gốc của Facebook

---

## 📦 Các thư viện chính cần `pip install`

```bash
# Tất cả trong một lệnh:
pip install fastapi uvicorn[standard] deepface easyocr tensorflow \
            Pillow numpy opencv-python-headless requests \
            pydantic-settings python-dotenv
```

---

## 🔌 Tích hợp với Spring Boot Backend

Gọi từ Java (Spring Boot) bằng `RestTemplate` hoặc `WebClient`:

```java
// Ví dụ gọi từ Spring Boot
String eKYCUrl = "http://localhost:8000/api/v1/ai/verify-ekyc";
Map<String, String> body = Map.of(
    "front_image_url", frontUrl,
    "back_image_url",  backUrl,
    "face_image_url",  selfieUrl
);
ResponseEntity<EKYCResponse> response = restTemplate.postForEntity(eKYCUrl, body, EKYCResponse.class);
```
