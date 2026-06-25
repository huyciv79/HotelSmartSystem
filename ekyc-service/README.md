# 🛡️ eKYC AI Service

FastAPI microservice xác thực danh tính điện tử (eKYC) tích hợp:
- **DeepFace** – Đăng ký embedding selfie và xác minh khuôn mặt khi check-in
- **YOLOv11** – Phát hiện vùng số CCCD, họ tên và ngày sinh
- **VietOCR** – Đọc tiếng Việt trong từng vùng thông tin

Ảnh CCCD chỉ dùng cho OCR/hồ sơ. Khi đăng ký eKYC, hệ thống không so sánh
chân dung trên CCCD với selfie. Việc đối chiếu khuôn mặt chỉ diễn ra tại check-in.

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

> **Lưu ý:** Lần đầu chạy cần Internet để tải model **Facenet512**, weight YOLO từ Hugging Face và weight VietOCR. Các model được cache cho những lần chạy sau.

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
  "back_image_url":  "https://example.com/cccd_back.jpg"
}
```

**Response thành công (200):**

```json
{
  "id_card_number": "012345678901",
  "full_name": "NGUYỄN VĂN A",
  "date_of_birth": "01/01/1990"
}
```

Pipeline OCR:

1. Tải và kiểm tra ảnh mặt trước/mặt sau.
2. YOLOv11 phát hiện các class `id_number`, `full_name`, `dob`.
3. Chọn box có confidence cao nhất cho từng class.
4. Thêm padding cố định 6 px, chuẩn hóa chiều cao 80 px, sharpening kernel tâm 9 và normalize tương phản.
5. VietOCR đọc từng vùng và hậu xử lý theo kiểu dữ liệu.

Selfie không được gửi tới API này. Đăng ký khuôn mặt thực hiện riêng qua
`POST /api/v1/face/enroll`.

### `POST /api/v1/face/enroll`

Nhận năm multipart field `selfie_image`, `left_image`, `right_image`, `up_image`,
`down_image`. Endpoint kiểm tra active liveness, anti-spoofing, xác nhận năm frame
thuộc cùng một người rồi tổng hợp thành face template 512 chiều để Spring Boot lưu.
Endpoint không nhận và không so sánh ảnh CCCD.

### `POST /api/v1/face/check-readiness`

Nhận một frame preview và trả trạng thái sẵn sàng trước khi quét liveness. Endpoint yêu cầu đúng
một khuôn mặt, khuôn mặt có chiều rộng/chiều cao tối thiểu và đang nhìn thẳng; không tạo embedding
và không lưu ảnh.

### `POST /api/v1/face/verify`

Nhận face template đã đăng ký, một frame nhìn thẳng và một frame thử thách quay trái
hoặc phải. Endpoint kiểm tra express active liveness, anti-spoofing rồi so sánh khuôn
mặt check-in với face template nhiều góc đã đăng ký.

---

## 🔧 Cấu hình model

| Biến môi trường       | Mặc định      | Mô tả                                         |
|-----------------------|---------------|-----------------------------------------------|
| `DEEPFACE_MODEL`      | `Facenet512`  | Model face embedding (512-dim)                |
| `LIVENESS_MIN_SCORE`  | `0.80`        | Độ tin cậy tối thiểu để vượt qua anti-spoofing |
| `ACTIVE_LIVENESS_CENTER_MAX_YAW` | `0.08` | Độ lệch tối đa khi nhìn chính diện |
| `ACTIVE_LIVENESS_SIDE_MIN_YAW` | `0.20` | Độ lệch tối thiểu cho mỗi lần quay đầu |
| `ACTIVE_LIVENESS_MIN_YAW_RANGE` | `0.42` | Khoảng cách tối thiểu giữa hai frame quay trái/phải |
| `ACTIVE_LIVENESS_VERTICAL_MIN_DELTA` | `0.08` | Độ lệch tối thiểu khi nhìn lên hoặc xuống |
| `ACTIVE_LIVENESS_MIN_PITCH_RANGE` | `0.18` | Khoảng cách tối thiểu giữa frame nhìn lên và nhìn xuống |
| `FACE_READINESS_MIN_SIZE` | `96` | Chiều rộng và chiều cao tối thiểu của khuôn mặt trong frame preview |
| `DEEPFACE_DETECTOR`   | `opencv`      | Face detector (`retinaface` chính xác hơn)    |
| `DEEPFACE_DISTANCE`   | `cosine`      | Metric đo khoảng cách vector                  |
| `FACE_MATCH_THRESHOLD`| `0.30`        | Ngưỡng cosine distance để xác nhận khớp       |
| `FACE_ENROLLMENT_CONSISTENCY_THRESHOLD` | `0.48` | Ngưỡng đảm bảo các frame đăng ký thuộc cùng một người |
| `HF_REPO_ID`          | Repo model    | Hugging Face repository chứa weight YOLO      |
| `HF_MODEL_FILE`       | `best.pt`     | Tên file weight YOLO                          |
| `CCCD_YOLO_CONFIDENCE`| `0.15`        | Confidence tối thiểu của detection            |
| `CCCD_YOLO_AGNOSTIC_NMS` | `True`     | Dùng class-agnostic NMS như pipeline Colab     |
| `OCR_PADDING_PIXELS`  | `6`           | Lề cố định quanh vùng YOLO trước khi OCR       |
| `CCCD_YOLO_IOU`       | `0.50`        | IoU threshold cho class-aware NMS             |
| `VIETOCR_CONFIG`      | `vgg_transformer` | Cấu hình VietOCR                          |
| `OCR_DEVICE`          | `auto`        | Tự chọn CUDA nếu có, ngược lại dùng CPU       |

### 💡 Model hỗ trợ embedding 512 chiều
- **`Facenet512`** ← *Được khuyến nghị* – Chính xác cao, cân bằng tốc độ/độ chính xác
- **`ArcFace`** – Chính xác cao hơn, chậm hơn
- **`DeepFace`** – Model gốc của Facebook

---

## 📦 Các thư viện chính cần `pip install`

```bash
# Tất cả trong một lệnh:
pip install fastapi uvicorn[standard] deepface ultralytics vietocr \
            huggingface-hub tensorflow Pillow numpy opencv-python-headless requests \
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
    "back_image_url",  backUrl
);
ResponseEntity<EKYCResponse> response = restTemplate.postForEntity(eKYCUrl, body, EKYCResponse.class);
```
