# 🏨 Hotel AI Agent

Trợ lý đặt phòng khách sạn thông minh sử dụng **Google Gemini 2.0 Flash** và **Google ADK**.

## ✨ Tính Năng

| Tính năng | Mô tả |
|-----------|-------|
| 🔍 **Tìm kiếm AI** | Gemini-2.0-Flash tìm và rank tối đa 15 khách sạn theo yêu cầu |
| 💰 **So sánh giá** | Mô phỏng giá từ 8 OTA: Booking.com, Agoda, Trivago, Hotels.com, Expedia, MakeMyTrip, Goibibo, Cleartrip |
| 🛎️ **Đặt phòng hội thoại** | Luồng 5 bước: chọn khách sạn → phòng → ngày → khách → xác nhận |
| 📊 **Dashboard real-time** | Sidebar theo dõi trạng thái đặt phòng, progress bar |
| 🔐 **Tích hợp backend** | Đăng nhập JWT, tạo booking qua Spring Boot REST API |

## 🚀 Cài Đặt

### 1. Yêu cầu
- Python 3.10+
- Spring Boot backend chạy tại `http://localhost:8080`

### 2. Cài đặt dependencies

```bash
cd hotel-ai-agent
pip install -r requirements.txt
```

### 3. Cấu hình

File `.env` đã được tạo sẵn. Kiểm tra API key:

```bash
# .env
GOOGLE_API_KEY=<your_key>
HOTEL_BACKEND_URL=http://localhost:8080/api
GEMINI_MODEL=gemini-2.0-flash
```

### 4. Chạy ứng dụng

```bash
streamlit run hotel_agent_app.py
```

Mở trình duyệt tại: **http://localhost:8501**

## 🧠 Kiến Trúc

```
hotel-ai-agent/
├── hotel_agent_app.py          # Streamlit main (UI, routing)
├── agents/
│   ├── hotel_search_agent.py   # HotelSearchAgent: search + price enrich
│   └── booking_agent.py        # BookingAgent: multi-step conversation + API
└── tools/
    ├── search_tool.py          # Gemini hotel finder (JSON structured output)
    └── price_comparator.py     # 8-platform price simulation
```

### Agents

**HotelSearchAgent**
- Gọi `search_tool.py` → Gemini tạo danh sách khách sạn JSON
- Enrich mỗi khách sạn với `price_comparator.py` (8 nền tảng)
- Generate AI summary bằng Gemini

**BookingAgent**
- State machine 5 bước: `SELECT_HOTEL → SELECT_ROOM → SELECT_DATES → GUEST_INFO → CONFIRM`
- Tích hợp `POST /api/bookings` của Spring Boot
- Hỗ trợ đăng nhập JWT để booking thực tế

## 💬 Ví Dụ Câu Lệnh

```
"Tìm khách sạn 5 sao ở Đà Nẵng dưới 3 triệu"
"Resort Phú Quốc có hồ bơi view biển"
"Khách sạn Hà Nội gần Hồ Hoàn Kiếm cho 2 người lớn"
"Đặt phòng tại The Grand Đà Nẵng 2 đêm từ 25/07"
"Xác nhận đặt phòng"
```

## 🐳 Docker

```bash
# Từ thư mục gốc hotel-booking-system/
docker-compose up ai-agent
```

Service sẽ chạy tại: **http://localhost:8501**
