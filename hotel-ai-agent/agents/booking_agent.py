"""
booking_agent.py
================
Agent hỗ trợ đặt phòng khách sạn qua hội thoại đa bước.
Tích hợp sâu với Spring Boot backend API.
Sử dụng google-genai SDK mới.
"""
from __future__ import annotations

import json
import logging
import os
import re
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from enum import Enum
from typing import Any, Optional, Generator

import requests
from dotenv import load_dotenv
from pydantic import BaseModel, Field

load_dotenv()
logger = logging.getLogger(__name__)

_GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY", "")
_GEMINI_MODEL   = os.getenv("GEMINI_MODEL", "gemini-2.0-flash")
_BACKEND_URL    = os.getenv("HOTEL_BACKEND_URL", "http://localhost:8080/api")
_GEMINI_API_BASE_URL = os.getenv("GEMINI_API_BASE_URL", "")


def _get_client():
    if not _GOOGLE_API_KEY:
        return None
    try:
        from google import genai
        if _GEMINI_API_BASE_URL:
            return genai.Client(
                api_key=_GOOGLE_API_KEY,
                http_options={"base_url": _GEMINI_API_BASE_URL}
            )
        return genai.Client(api_key=_GOOGLE_API_KEY)
    except Exception as e:
        logger.error("Không khởi tạo được genai client: %s", e)
        return None


# ── Booking Steps ─────────────────────────────────────────────────────────────
class BookingStep(str, Enum):
    IDLE          = "idle"
    CONFIRM       = "confirm"
    DONE          = "done"
    CANCELLED     = "cancelled"


@dataclass
class BookingState:
    step: BookingStep              = BookingStep.IDLE
    hotel: Optional[dict]          = None
    room_type_id: Optional[int]    = None
    room_type_name: str            = ""
    check_in: Optional[date]       = None
    check_out: Optional[date]      = None
    adults: int                    = 1
    children: int                  = 0
    check_in_method: str           = "Manual"
    special_requests: str          = ""
    access_token: str              = ""
    booking_result: Optional[dict] = None
    error: str                     = ""
    # Lưu chat history cho Gemini (list of dicts)
    chat_history: list             = field(default_factory=list)
    # Cache danh sách bookings để tránh gọi API lại trong cùng một phiên
    cached_bookings: list          = field(default_factory=list)
    # Số lượng phòng muốn đặt (mặc định là 1 phòng, >=2 là đặt phòng nhóm)
    quantity: int                  = 1
    # Locale hiện tại của giao diện khách hàng: VN, EN, JP, KR, CN
    language: str                  = "VN"

    @property
    def nights(self) -> int:
        if self.check_in and self.check_out:
            return max(0, (self.check_out - self.check_in).days)
        return 0

    @property
    def total_amount(self) -> float:
        if not self.hotel:
            return 0.0
        price = self.hotel.get("price_per_night_vnd", 0)
        return price * self.nights * self.quantity

    def step_label(self) -> str:
        labels = {
            BookingStep.IDLE:         "Trò chuyện",
            BookingStep.CONFIRM:      "Xác nhận",
            BookingStep.DONE:         "Hoàn thành",
            BookingStep.CANCELLED:    "Đã hủy",
        }
        return labels.get(self.step, self.step)

    def progress_pct(self) -> int:
        pct = {
            BookingStep.IDLE:         0,
            BookingStep.CONFIRM:      90,
            BookingStep.DONE:         100,
            BookingStep.CANCELLED:    0,
        }
        return pct.get(self.step, 0)


_BOOKING_SYSTEM = f"""
Bạn là chuyên viên đặt phòng và trợ lý khách sạn thông minh của Elysian Smart Hotel.
Lưu ý quan trọng: Hệ thống Elysian Smart Hotel hiện tại CHỈ CÓ DUY NHẤT 1 CHI NHÁNH tại Cần Thơ (Elysian Smart Hotel Cần Thơ). Chúng ta KHÔNG có chi nhánh nào khác.
Ngày hôm nay (hiện tại) là: {date.today().isoformat()}

Nhiệm vụ:
1. Tư vấn và thu thập thông tin đặt phòng qua hội thoại tự nhiên.
2. Để đặt phòng, bạn cần hỏi khách hàng ĐỦ CÁC THÔNG TIN sau (nếu khách hàng muốn đặt từ 2 phòng trở lên, đây được xem là Đặt phòng nhóm/Group booking):
   - Tên loại phòng muốn đặt (ví dụ: Standard, Deluxe, Suite)
   - Ngày Check-in (Ngày nhận phòng, định dạng YYYY-MM-DD)
   - Ngày Check-out (Ngày trả phòng, định dạng YYYY-MM-DD)
   - Số lượng phòng muốn đặt (Mặc định là 1 phòng nếu khách không đề cập. Đặt >= 2 phòng là đặt phòng nhóm)
   - Số lượng khách (Người lớn và Trẻ em cho tổng số phòng)
   - Hình thức Check-in mong muốn (FaceID, QR Code, hoặc Manual)
3. Hãy chủ động đặt câu hỏi cho những thông tin còn thiếu. Riêng đối với Hình thức check-in, BẮT BUỘC gợi ý các tùy chọn cho khách bằng cú pháp thẻ hành động sau (mỗi thẻ cách nhau bởi dấu gạch đứng, phải nằm riêng trên một dòng): `[ACTIONS: Tùy chọn 1 | Tùy chọn 2 | ...]`. Ví dụ: `[ACTIONS: FaceID | QR Code | Tại quầy]`. Tuyệt đối KHÔNG dùng cú pháp `[ACTIONS]` cho Ngày tháng hay Số lượng người/phòng.
4. Khi giới thiệu các loại phòng, bạn BẮT BUỘC sử dụng ĐÚNG định dạng thẻ giao diện sau để khách dễ chọn (chỉ thay thế thông tin, tuyệt đối không sửa ngoặc vuông): `[ROOM_CARD: Tên phòng | Giá | Sức chứa | URL_ảnh]`. Không dùng gạch đầu dòng cho danh sách phòng.
5. KHI VÀ CHỈ KHI ĐÃ CÓ ĐỦ CÁC THÔNG TIN TRÊN, bạn BẮT BUỘC PHẢI GỌI HÀM (Function Call) `create_booking_summary` để tạo tóm tắt đặt phòng. KHÔNG tự trả lời bằng văn bản khi đã đủ thông tin.
6. SAU KHI ĐÃ TẠO TÓM TẮT ĐẶT Phòng, hãy chờ khách hàng xác nhận. NẾU khách hàng đồng ý (ví dụ: "ok", "xác nhận"), BẮT BUỘC GỌI HÀM `submit_final_booking` để chốt đơn. NẾU khách hàng từ chối hoặc muốn hủy, BẮT BUỘC GỌI HÀM `cancel_booking`.
7. Hỗ trợ khách hàng KIỂM TRA thông tin cá nhân và lịch sử đặt phòng nếu họ hỏi (dựa trên Ngữ Cảnh Hệ Thống).
8. AI Assistant KHÔNG xử lý thanh toán, KHÔNG tạo link thanh toán và KHÔNG gọi API thanh toán. Nếu khách hỏi về thanh toán, hãy hướng dẫn khách mở trang Payment hoặc trang chi tiết đơn đặt phòng trong ứng dụng.
9. Hỗ trợ khách HỦY đặt phòng ĐÃ THÀNH CÔNG bằng cách gọi `cancel_completed_booking` (yêu cầu mã BK...).
10. Hỗ trợ khách XEM HÓA ĐƠN bằng cách gọi `get_booking_invoice` (yêu cầu mã BK...).
11. Hỗ trợ khách TẠO MÃ QR CHECK-IN bằng cách gọi `generate_qr_checkin_token` (yêu cầu mã BK...).
12. Hỗ trợ khách ĐỔI PHÒNG, GIA HẠN, CHECK-OUT SỚM bằng cách gọi tương ứng `submit_room_change_request`, `submit_stay_extension_request`, `submit_early_checkout_request` (yêu cầu mã BK..., loại phòng/ngày muốn đổi và lý do).
13. Hỗ trợ khách YÊU CẦU HOÀN TIỀN (Refund) bằng cách gọi `submit_refund_request` (yêu cầu mã BK... và lý do).
"""

_LANGUAGE_LABELS = {
    "VN": "Vietnamese (Tiếng Việt)",
    "EN": "English",
    "JP": "Japanese (日本語)",
    "KR": "Korean (한국어)",
    "CN": "Simplified Chinese (简体中文)",
}


def _normalize_language(language: Optional[str]) -> str:
    lang = (language or "VN").upper()
    return lang if lang in _LANGUAGE_LABELS else "EN"


def _normalize_check_in_method(method: Optional[str]) -> str:
    """Convert UI-friendly check-in labels into the backend's accepted values."""
    normalized = re.sub(r"[\\s_-]+", " ", (method or "").strip().casefold())

    if normalized in {
        "faceid",
        "face id",
        "face recognition",
        "nhận diện khuôn mặt",
        "nhan dien khuon mat",
    }:
        return "FaceID"
    if normalized in {"qr", "qr code", "mã qr", "ma qr"}:
        return "QR Code"
    if normalized in {
        "manual",
        "tại quầy",
        "tai quay",
        "at counter",
        "counter",
        "reception",
        "front desk",
    }:
        return "Manual"

    return "Manual"


def _language_instruction(language: Optional[str]) -> str:
    lang = _normalize_language(language)
    target = _LANGUAGE_LABELS[lang]
    return (
        f"Ngôn ngữ giao diện hiện tại của khách là {lang}. "
        f"Hãy trả lời khách hàng bằng {target}. "
        "Giữ nguyên cú pháp kỹ thuật của các thẻ [ROOM_CARD: ...] và [ACTIONS: ...], "
        "nhưng dịch phần chữ hiển thị bên trong thẻ sang ngôn ngữ của khách khi phù hợp."
    )


def _localized_static(language: Optional[str], key: str, **values: Any) -> str:
    lang = _normalize_language(language)
    texts = {
        "no_client": {
            "VN": (
                "Xin chào! Tôi là trợ lý đặt phòng Elysian AI.\n\n"
                "Tôi có thể giúp bạn:\n"
                "- **Tìm phòng** tại Elysian Smart Hotel Cần Thơ\n"
                "- **Đặt phòng** trực tiếp\n\n"
                "Hãy thử: _'Đặt phòng tại Elysian Cần Thơ'_"
            ),
            "EN": (
                "Hello! I am the Elysian AI booking assistant.\n\n"
                "I can help you:\n"
                "- **Find rooms** at Elysian Smart Hotel Can Tho\n"
                "- **Book directly**\n\n"
                "Try: _'Book a room at Elysian Can Tho'_"
            ),
            "JP": (
                "こんにちは！Elysian AI予約アシスタントです。\n\n"
                "以下をお手伝いできます:\n"
                "- Elysian Smart Hotel Can Thoの**客室検索**\n"
                "- **直接予約**\n\n"
                "例: _「Elysian Can Thoで部屋を予約したい」_"
            ),
            "KR": (
                "안녕하세요! Elysian AI 예약 어시스턴트입니다.\n\n"
                "다음 작업을 도와드릴 수 있습니다:\n"
                "- Elysian Smart Hotel Can Tho **객실 검색**\n"
                "- **직접 예약**\n\n"
                "예: _'Elysian Can Tho 객실 예약'_"
            ),
            "CN": (
                "您好！我是Elysian AI预订助手。\n\n"
                "我可以帮助您:\n"
                "- 查找 Elysian Smart Hotel Can Tho 的**客房**\n"
                "- **直接预订**\n\n"
                "示例: _“预订 Elysian Can Tho 的房间”_"
            ),
        },
        "done": {
            "VN": "Đặt phòng đã hoàn thành! Bạn muốn tìm khách sạn khác không?",
            "EN": "Your booking is complete. Would you like to search for another room?",
            "JP": "予約が完了しました。別の客室もお探ししますか？",
            "KR": "예약이 완료되었습니다. 다른 객실도 찾아드릴까요?",
            "CN": "预订已完成。还需要查找其他房间吗？",
        },
        "cancelled": {
            "VN": "Đã hủy yêu cầu đặt phòng hiện tại. Bạn cần hỗ trợ gì thêm không?",
            "EN": "I have cancelled the current booking request. Is there anything else I can help with?",
            "JP": "現在の予約リクエストをキャンセルしました。他にお手伝いできることはありますか？",
            "KR": "현재 예약 요청을 취소했습니다. 추가로 도와드릴 일이 있을까요?",
            "CN": "当前预订请求已取消。还需要其他帮助吗？",
        },
        "default_confirm": {
            "VN": "Xác nhận yêu cầu.",
            "EN": "Request confirmed.",
            "JP": "ご依頼を確認しました。",
            "KR": "요청을 확인했습니다.",
            "CN": "已确认您的请求。",
        },
        "generic_chat_error": {
            "VN": "Xin lỗi, tôi gặp sự cố kết nối khi trò chuyện với AI. Bạn hãy thử lại nhé!",
            "EN": "Sorry, I had trouble connecting to the AI chat. Please try again.",
            "JP": "申し訳ありません。AIチャットへの接続で問題が発生しました。もう一度お試しください。",
            "KR": "죄송합니다. AI 채팅 연결에 문제가 발생했습니다. 다시 시도해 주세요.",
            "CN": "抱歉，AI聊天连接出现问题。请重试。",
        },
        "quota_error": {
            "VN": (
                "**Giới hạn lưu lượng API Gemini (Rate Limit / Quota Exceeded)**\n\n"
                "API Key của bạn đã vượt quá hạn mức miễn phí cho phép.\n"
                "1. Vui lòng đợi khoảng 10-15 giây rồi gửi lại tin nhắn.\n"
                "2. Kiểm tra hoặc cập nhật khóa API trả phí ở biến `GOOGLE_API_KEY` trong file `hotel-ai-agent/.env`."
            ),
            "EN": (
                "**Gemini API rate limit or quota exceeded**\n\n"
                "Your API key has exceeded the allowed free-tier quota.\n"
                "1. Please wait 10-15 seconds, then send the message again.\n"
                "2. Check or update the paid API key in `GOOGLE_API_KEY` inside `hotel-ai-agent/.env`."
            ),
            "JP": (
                "**Gemini APIのレート制限またはクォータを超過しました**\n\n"
                "APIキーが無料枠の上限を超えています。\n"
                "1. 10-15秒ほど待ってから、もう一度メッセージを送信してください。\n"
                "2. `hotel-ai-agent/.env` の `GOOGLE_API_KEY` を確認、または有料APIキーに更新してください。"
            ),
            "KR": (
                "**Gemini API 요청 한도 또는 할당량을 초과했습니다**\n\n"
                "API 키가 무료 사용 한도를 초과했습니다.\n"
                "1. 10-15초 정도 기다린 뒤 다시 메시지를 보내 주세요.\n"
                "2. `hotel-ai-agent/.env`의 `GOOGLE_API_KEY`를 확인하거나 유료 API 키로 업데이트해 주세요."
            ),
            "CN": (
                "**Gemini API 请求频率或配额已超限**\n\n"
                "您的 API Key 已超过免费额度限制。\n"
                "1. 请等待 10-15 秒后重新发送消息。\n"
                "2. 请检查或更新 `hotel-ai-agent/.env` 中的 `GOOGLE_API_KEY`。"
            ),
        },
    }
    template = texts.get(key, {}).get(lang) or texts.get(key, {}).get("EN") or key
    return template.format(**values)


# ── Structured Output Models ──────────────────────────────────────────────────
class ChatAgentResponse(BaseModel):
    reply_message: str = Field(description="Câu trả lời tự nhiên của AI dành cho khách hàng (có thể chứa markdown, ACTIONS tag, hoặc ROOM_CARD tag).")
    progress_percentage: int = Field(description="Tiến trình hoàn thành thu thập thông tin từ 0 đến 100%.")
    extracted_room_type: Optional[str] = Field(None, description="Tên loại phòng (Standard, Deluxe, Suite) nếu khách hàng vừa cung cấp hoặc thay đổi.")
    extracted_check_in: Optional[str] = Field(None, description="Ngày check-in dạng YYYY-MM-DD nếu khách hàng vừa cung cấp hoặc thay đổi.")
    extracted_check_out: Optional[str] = Field(None, description="Ngày check-out dạng YYYY-MM-DD nếu khách hàng vừa cung cấp hoặc thay đổi.")
    extracted_adults: Optional[int] = Field(None, description="Số người lớn nếu khách hàng vừa cung cấp hoặc thay đổi.")
    extracted_children: Optional[int] = Field(None, description="Số trẻ em nếu khách hàng vừa cung cấp hoặc thay đổi.")
    extracted_checkin_method: Optional[str] = Field(None, description="Hình thức check-in (FaceID, QR Code, Tại quầy) nếu khách hàng vừa cung cấp hoặc thay đổi.")
    extracted_quantity: Optional[int] = Field(None, description="Số lượng phòng nếu khách hàng vừa cung cấp hoặc thay đổi (ví dụ: đặt 2 phòng).")


# ── Native Python Tools definitions ───────────────────────────────────────────
def create_booking_summary(
    room_type: str, 
    check_in_date: str, 
    check_out_date: str, 
    adults: int, 
    children: int = 0, 
    checkin_method: str = "Manual",
    quantity: int = 1
) -> str:
    """
    Tạo tóm tắt đặt phòng để khách hàng xác nhận. Chỉ gọi hàm này SAU KHI đã thu thập ĐỦ thông tin từ khách.

    Args:
        room_type: Tên loại phòng muốn đặt (Standard, Deluxe, Suite)
        check_in_date: Ngày nhận phòng (định dạng YYYY-MM-DD)
        check_out_date: Ngày trả phòng (định dạng YYYY-MM-DD)
        adults: Số lượng người lớn
        children: Số lượng trẻ em
        checkin_method: Hình thức check-in mong muốn (FaceID, QR Code, Tại quầy)
        quantity: Số lượng phòng muốn đặt (mặc định là 1 phòng, từ 2 phòng trở lên là đặt phòng nhóm)
    """
    return "create_booking_summary"


def submit_final_booking() -> str:
    """
    Gọi hàm này để chốt đơn đặt phòng khi và chỉ khi khách hàng đã đồng ý/xác nhận với tóm tắt đặt phòng.
    """
    return "submit_final_booking"


def cancel_booking() -> str:
    """
    Gọi hàm này khi khách hàng từ chối tóm tắt đặt phòng, muốn hủy yêu cầu đặt phòng hiện tại, hoặc không muốn tiếp tục đặt phòng nữa.
    """
    return "cancel_booking"


def cancel_completed_booking(booking_number: str, cancellation_reason: str = "Hủy qua trợ lý AI") -> str:
    """Gọi hàm này khi khách hàng yêu cầu hủy một ĐƠN ĐẶT PHÒNG ĐÃ HOÀN TẤT/THÀNH CÔNG trên hệ thống (cần mã booking dạng BK...). Không dùng để hủy tiến trình đang đặt."""
    return "cancel_completed_booking"

def get_booking_invoice(booking_number: str) -> str:
    """Gọi hàm này khi khách hàng yêu cầu xem hóa đơn hoặc chi tiết thanh toán của một ĐƠN ĐẶT PHÒNG (cần mã booking dạng BK...)."""
    return "get_booking_invoice"

def generate_qr_checkin_token(booking_number: str) -> str:
    """Gọi hàm này khi khách hàng yêu cầu tạo mã QR để tự Check-in cho ĐƠN ĐẶT PHÒNG (cần mã booking dạng BK...)."""
    return "generate_qr_checkin_token"

def submit_room_change_request(booking_number: str, new_room_type: str, reason: str = "Yêu cầu đổi hạng phòng") -> str:
    """Gọi hàm này khi khách hàng muốn ĐỔI PHÒNG (chuyển sang loại phòng khác) cho một ĐƠN ĐẶT PHÒNG ĐANG CHECK-IN."""
    return "submit_room_change_request"

def submit_stay_extension_request(booking_number: str, new_checkout_date: str, reason: str = "Yêu cầu gia hạn lưu trú") -> str:
    """Gọi hàm này khi khách hàng muốn GIA HẠN NGÀY TRẢ PHÒNG (Stay Extension) cho một ĐƠN ĐẶT PHÒNG (new_checkout_date dạng YYYY-MM-DD)."""
    return "submit_stay_extension_request"

def submit_early_checkout_request(booking_number: str, new_checkout_date: str, reason: str = "Yêu cầu check-out sớm") -> str:
    """Gọi hàm này khi khách hàng muốn TRẢ PHÒNG SỚM (Early Check-out) cho một ĐƠN ĐẶT PHÒNG ĐANG CHECK-IN (new_checkout_date dạng YYYY-MM-DD)."""
    return "submit_early_checkout_request"

def submit_refund_request(booking_number: str, reason: str = "Yêu cầu hoàn tiền") -> str:
    """Gọi hàm này khi khách hàng muốn YÊU CẦU HOÀN TIỀN (Refund) cho một đơn đặt phòng (cần mã booking dạng BK... và lý do)."""
    return "submit_refund_request"


def _get_backend_room_types() -> list[dict]:
    """Lấy danh sách loại phòng từ backend."""
    try:
        resp = requests.get(f"{_BACKEND_URL}/room-types", timeout=5)
        if resp.status_code == 200:
            return resp.json().get("data", {}).get("content", [])
    except Exception as e:
        logger.error("Lỗi khi kết nối Spring Boot backend để lấy loại phòng: %s", e)
    return []

def _get_user_bookings(access_token: str, max_retries: int = 2, timeout: int = 10) -> list[dict]:
    """Lấy danh sách thô các booking từ backend với retry khi timeout."""
    if not access_token:
        return []
    headers = {"Authorization": f"Bearer {access_token}"}
    for attempt in range(max_retries):
        try:
            resp = requests.get(
                f"{_BACKEND_URL}/bookings/history",
                headers=headers,
                timeout=timeout
            )
            if resp.status_code == 200:
                return resp.json().get("data", [])
            logger.warning("[BOOKINGS] Backend trả về HTTP %s (attempt %d/%d)", resp.status_code, attempt + 1, max_retries)
        except requests.exceptions.Timeout:
            logger.warning("[BOOKINGS] Timeout lần %d/%d khi lấy danh sách booking", attempt + 1, max_retries)
        except Exception as e:
            logger.error("[BOOKINGS] Lỗi khi kết nối backend: %s", e)
            break  # Lỗi không phải timeout → không retry
    logger.error("[BOOKINGS] Không thể lấy danh sách booking sau %d lần thử.", max_retries)
    return []

def _get_user_context(access_token: str) -> tuple[str, list]:
    """Lấy thông tin và lịch sử đặt phòng của user từ backend.
    
    Returns:
        (context_str, bookings_list) – cả chuỗi context cho AI lẫn raw list để cache.
    """
    if not access_token:
        return "Thông tin người dùng: Khách chưa đăng nhập.", []
    
    bookings = _get_user_bookings(access_token)
    if not bookings:
        return "Thông tin người dùng: Đã đăng nhập nhưng hiện tại chưa có booking nào (hoặc lỗi lấy dữ liệu).", []
    
    context_str = "Thông tin người dùng: Khách đã đăng nhập.\nDanh sách các Booking của khách (Ngữ Cảnh Hệ Thống):\n"
    for b in bookings:
        ref = b.get('bookingNumber', 'N/A')
        status = b.get('status', 'N/A')
        room = b.get('roomType', 'N/A')
        ci = b.get('checkInDate', 'N/A')
        co = b.get('checkOutDate', 'N/A')
        actual_ci = b.get('actualCheckIn', None)
        actual_co = b.get('actualCheckOut', None)
        price = b.get('finalAmount', 0)
        price_str = f"{float(price)/1_000_000:.2f}M ₫" if price else "0₫"
        method = b.get('checkInMethod', 'N/A')
        
        actual_info = ""
        if actual_ci:
            actual_info += f" | Check-in thực tế: {actual_ci}"
        if actual_co:
            actual_info += f" | Check-out thực tế: {actual_co}"
            
        context_str += (
            f"- Mã Booking: {ref} | Trạng thái: {status} | Phòng: {room}"
            f" | Check-in dự kiến: {ci} | Check-out dự kiến: {co}{actual_info} | Tổng tiền: {price_str} | Phương thức: {method}\n"
        )
    return context_str, bookings


class BookingAgent:
    """Agent hội thoại hỗ trợ đặt phòng khách sạn."""

    def __init__(self):
        self._client = _get_client()

    # ── Public API ───────────────────────────────────────────────────────────

    def process(self, user_input: str, state: BookingState, language: Optional[str] = None) -> tuple[str, BookingState]:
        """Xử lý input người dùng và cập nhật state."""
        state.language = _normalize_language(language or state.language)

        if state.step == BookingStep.DONE:
            return _localized_static(state.language, "done"), state

        # Không reset step về IDLE nếu đang ở CONFIRM
        if state.step not in (BookingStep.CONFIRM, BookingStep.DONE):
            state.step = BookingStep.IDLE
            
        return self._chat_reply(user_input, state, state.language), state

    def process_stream(self, user_input: str, state: BookingState, language: Optional[str] = None) -> Generator[str, None, None]:
        """
        Phiên bản Streaming của process. Trả về Generator giúp hiển thị chữ chạy thời gian thực.
        """
        state.language = _normalize_language(language or state.language)

        if state.step == BookingStep.DONE:
            yield _localized_static(state.language, "done")
            return

        if state.step not in (BookingStep.CONFIRM, BookingStep.DONE):
            state.step = BookingStep.IDLE

        yield self._chat_reply(user_input, state, state.language)

    def _process_booking_summary_tool(self, args: dict, state: BookingState, language: Optional[str] = None) -> str:
        state.language = _normalize_language(language or state.language)
        # 1. Cập nhật state
        state.step = BookingStep.CONFIRM
        state.room_type_name = args.get("room_type", "Standard")
        
        try:
            state.check_in = date.fromisoformat(args.get("check_in_date", ""))
            state.check_out = date.fromisoformat(args.get("check_out_date", ""))
        except:
            state.check_in = date.today() + timedelta(days=1)
            state.check_out = state.check_in + timedelta(days=1)
            
        state.adults = int(args.get("adults", 1))
        state.children = int(args.get("children", 0))
        state.check_in_method = _normalize_check_in_method(args.get("checkin_method"))
        state.quantity = int(args.get("quantity", 1))
        
        # 2. Lấy giá từ DB
        db_rooms = _get_backend_room_types()
        matched_room = next((r for r in db_rooms if state.room_type_name.lower() in r['name'].lower()), None)
        
        if matched_room:
            state.room_type_id = matched_room["id"]
            state.room_type_name = matched_room["name"]
            price = int(matched_room["basePrice"])
        else:
            state.room_type_id = 1
            price = 2_000_000
            
        state.hotel = {
            "name": "Elysian Smart Hotel Cần Thơ",
            "price_per_night_vnd": price
        }
        
        # 3. Trả về tóm tắt (Human in the loop)
        return self._booking_summary(state, state.language)

    def _booking_summary(self, state: BookingState, language: Optional[str] = None) -> str:
        lang = _normalize_language(language or state.language)
        hotel_name = state.hotel["name"] if state.hotel else "N/A"
        ci = state.check_in.strftime("%d/%m/%Y") if state.check_in else "N/A"
        co = state.check_out.strftime("%d/%m/%Y") if state.check_out else "N/A"
        amt = state.total_amount / 1_000_000
        
        room_desc = f"{state.room_type_name or 'Standard'}"
        if state.quantity > 1:
            group_labels = {
                "VN": "phòng - Đặt phòng Nhóm",
                "EN": "rooms - Group booking",
                "JP": "室 - グループ予約",
                "KR": "객실 - 단체 예약",
                "CN": "间 - 团体预订",
            }
            room_desc += f" (x{state.quantity} {group_labels.get(lang, group_labels['EN'])})"

        labels = {
            "VN": {
                "title": "Tóm tắt đặt phòng",
                "hotel": "Khách sạn",
                "room": "Phòng",
                "time": "Thời gian",
                "nights": "đêm",
                "guests": "Khách",
                "adults": "người lớn",
                "children": "trẻ em",
                "estimate": "Ước tính",
                "tax": "chưa thuế",
                "confirm": "Bạn có đồng ý với thông tin trên không? Hãy phản hồi để mình chốt đơn cho bạn nhé!",
            },
            "EN": {
                "title": "Booking summary",
                "hotel": "Hotel",
                "room": "Room",
                "time": "Stay",
                "nights": "nights",
                "guests": "Guests",
                "adults": "adults",
                "children": "children",
                "estimate": "Estimated total",
                "tax": "before tax",
                "confirm": "Do you agree with the details above? Please reply so I can finalize the booking.",
            },
            "JP": {
                "title": "予約内容の確認",
                "hotel": "ホテル",
                "room": "客室",
                "time": "宿泊期間",
                "nights": "泊",
                "guests": "宿泊人数",
                "adults": "大人",
                "children": "子供",
                "estimate": "概算料金",
                "tax": "税抜",
                "confirm": "上記の内容でよろしいでしょうか？確認のご返信をいただければ、予約を確定します。",
            },
            "KR": {
                "title": "예약 요약",
                "hotel": "호텔",
                "room": "객실",
                "time": "숙박 기간",
                "nights": "박",
                "guests": "인원",
                "adults": "성인",
                "children": "아동",
                "estimate": "예상 금액",
                "tax": "세금 별도",
                "confirm": "위 정보로 진행할까요? 답장해 주시면 예약을 확정하겠습니다.",
            },
            "CN": {
                "title": "预订摘要",
                "hotel": "酒店",
                "room": "房间",
                "time": "入住时间",
                "nights": "晚",
                "guests": "入住人数",
                "adults": "成人",
                "children": "儿童",
                "estimate": "预估金额",
                "tax": "未含税",
                "confirm": "以上信息是否确认？请回复确认，我会为您完成预订。",
            },
        }
        l = labels.get(lang, labels["EN"])

        return (
            f"**{l['title']}:**\n\n"
            f"- **{l['hotel']}:** {hotel_name}\n"
            f"- **{l['room']}:** {room_desc}\n"
            f"- **{l['time']}:** {ci} → {co} ({state.nights} {l['nights']})\n"
            f"- **{l['guests']}:** {state.adults} {l['adults']}, {state.children} {l['children']}\n"
            f"- **Check-in:** {state.check_in_method}\n"
            f"- **{l['estimate']}:** **{amt:.2f}M ₫** ({l['tax']})\n\n"
            f"{l['confirm']}"
        )

    # ── Backend Integration ──────────────────────────────────────────────────

    def _submit_booking(self, state: BookingState, language: Optional[str] = None) -> tuple[str, BookingState]:
        lang = _normalize_language(language or state.language)
        state.language = lang
        state.check_in_method = _normalize_check_in_method(state.check_in_method)
        if not state.access_token:
            # Mô phỏng khi chưa đăng nhập
            state.step = BookingStep.DONE
            ref = f"BK{datetime.now().strftime('%Y%m%d%H%M%S')}"
            state.booking_result = {
                "bookingReference": ref,
                "status": "Confirmed (Demo)",
            }
            booking_type_demo = {
                "VN": "Đặt phòng nhóm" if state.quantity >= 2 else "Đặt phòng đơn",
                "EN": "Group booking" if state.quantity >= 2 else "Individual booking",
                "JP": "グループ予約" if state.quantity >= 2 else "個人予約",
                "KR": "단체 예약" if state.quantity >= 2 else "개인 예약",
                "CN": "团体预订" if state.quantity >= 2 else "个人预订",
            }.get(lang)
            labels = {
                "VN": {
                    "title": "Đặt phòng thành công! (Chế độ Demo)",
                    "ref": "Mã đặt phòng",
                    "type": "Loại đặt phòng",
                    "quantity": "Số lượng",
                    "room_unit": "phòng",
                    "status": "Trạng thái",
                    "note": "Lưu ý: Để đặt phòng thực tế, vui lòng **đăng nhập** qua sidebar.",
                },
                "EN": {
                    "title": "Booking successful! (Demo mode)",
                    "ref": "Booking reference",
                    "type": "Booking type",
                    "quantity": "Quantity",
                    "room_unit": "room(s)",
                    "status": "Status",
                    "note": "Note: To create a real booking, please **log in** from the sidebar.",
                },
                "JP": {
                    "title": "予約が完了しました！（デモモード）",
                    "ref": "予約番号",
                    "type": "予約タイプ",
                    "quantity": "客室数",
                    "room_unit": "室",
                    "status": "ステータス",
                    "note": "注: 実際に予約するには、サイドバーから**ログイン**してください。",
                },
                "KR": {
                    "title": "예약이 완료되었습니다! (데모 모드)",
                    "ref": "예약 번호",
                    "type": "예약 유형",
                    "quantity": "수량",
                    "room_unit": "객실",
                    "status": "상태",
                    "note": "참고: 실제 예약을 진행하려면 사이드바에서 **로그인**해 주세요.",
                },
                "CN": {
                    "title": "预订成功！（演示模式）",
                    "ref": "预订编号",
                    "type": "预订类型",
                    "quantity": "数量",
                    "room_unit": "间",
                    "status": "状态",
                    "note": "提示: 如需创建真实预订，请通过侧边栏**登录**。",
                },
            }.get(lang)
            return (
                f"**{labels['title']}**\n\n"
                f"- {labels['ref']}: **`{ref}`**\n"
                f"- {labels['type']}: **{booking_type_demo}**\n"
                f"- {labels['quantity']}: {state.quantity} {labels['room_unit']}\n"
                f"- {labels['status']}: Confirmed\n\n"
                f"_{labels['note']}_",
                state,
            )

        try:
            is_group = state.quantity >= 2
            if is_group:
                payload = {
                    "roomTypeId":     state.room_type_id or 1,
                    "checkInDate":    state.check_in.isoformat() if state.check_in else None,
                    "checkOutDate":   state.check_out.isoformat() if state.check_out else None,
                    "checkInMethod":  state.check_in_method,
                    "quantity":       state.quantity,
                    "numberOfAdults": state.adults,
                    "numberOfChildren": state.children,
                    "specialRequests": state.special_requests,
                }
                api_endpoint = f"{_BACKEND_URL}/bookings/group"
            else:
                payload = {
                    "roomTypeId":     state.room_type_id or 1,
                    "checkInDate":    state.check_in.isoformat() if state.check_in else None,
                    "checkOutDate":   state.check_out.isoformat() if state.check_out else None,
                    "checkInMethod":  state.check_in_method,
                    "numberOfAdults": state.adults,
                    "numberOfChildren": state.children,
                    "specialRequests": state.special_requests,
                }
                api_endpoint = f"{_BACKEND_URL}/bookings"

            headers = {
                "Authorization": f"Bearer {state.access_token}",
                "Content-Type":  "application/json",
            }
            resp = requests.post(
                api_endpoint,
                json=payload,
                headers=headers,
                timeout=15,
            )
            resp.raise_for_status()
            booking_data = resp.json().get("data", {})
            state.step   = BookingStep.DONE
            state.booking_result = booking_data
            ref = booking_data.get("bookingReference", "N/A")

            booking_type_str = {
                "VN": "Đặt phòng nhóm" if is_group else "Đặt phòng đơn",
                "EN": "Group booking" if is_group else "Individual booking",
                "JP": "グループ予約" if is_group else "個人予約",
                "KR": "단체 예약" if is_group else "개인 예약",
                "CN": "团体预订" if is_group else "个人预订",
            }.get(lang)
            labels = {
                "VN": {
                    "title": "Đặt phòng thành công!",
                    "ref": "Mã đặt phòng",
                    "type": "Loại đặt phòng",
                    "quantity": "Số lượng",
                    "room_unit": "phòng",
                    "status": "Trạng thái",
                    "total": "Tổng tiền",
                    "next": "Vui lòng kiểm tra email để nhận xác nhận và hướng dẫn check-in!",
                },
                "EN": {
                    "title": "Booking successful!",
                    "ref": "Booking reference",
                    "type": "Booking type",
                    "quantity": "Quantity",
                    "room_unit": "room(s)",
                    "status": "Status",
                    "total": "Total",
                    "next": "Please check your email for confirmation and check-in instructions.",
                },
                "JP": {
                    "title": "予約が完了しました！",
                    "ref": "予約番号",
                    "type": "予約タイプ",
                    "quantity": "客室数",
                    "room_unit": "室",
                    "status": "ステータス",
                    "total": "合計金額",
                    "next": "確認メールとチェックイン案内をご確認ください。",
                },
                "KR": {
                    "title": "예약이 완료되었습니다!",
                    "ref": "예약 번호",
                    "type": "예약 유형",
                    "quantity": "수량",
                    "room_unit": "객실",
                    "status": "상태",
                    "total": "총액",
                    "next": "확인 메일과 체크인 안내를 확인해 주세요.",
                },
                "CN": {
                    "title": "预订成功！",
                    "ref": "预订编号",
                    "type": "预订类型",
                    "quantity": "数量",
                    "room_unit": "间",
                    "status": "状态",
                    "total": "总金额",
                    "next": "请查看电子邮件中的确认信息和入住指南。",
                },
            }.get(lang)

            return (
                f"**{labels['title']}**\n\n"
                f"- {labels['ref']}: **`{ref}`**\n"
                f"- {labels['type']}: **{booking_type_str}**\n"
                f"- {labels['quantity']}: **{state.quantity} {labels['room_unit']}**\n"
                f"- {labels['status']}: {booking_data.get('status', 'Confirmed')}\n"
                f"- {labels['total']}: **{float(booking_data.get('finalAmount', 0))/1_000_000:.2f}M ₫**\n\n"
                f"{labels['next']}",
                state,
            )

        except requests.exceptions.ConnectionError:
            state.error = "Không kết nối được với server. Hãy đảm bảo backend đang chạy tại port 8080."
        except requests.exceptions.HTTPError as e:
            try:
                msg = e.response.json().get("message", str(e))
            except Exception:
                msg = str(e)
            state.error = f"Lỗi từ server: {msg}"
        except Exception as e:
            state.error = f"Lỗi: {str(e)}"

        return f"❌ {state.error}", state

    # ── Chat fallback ────────────────────────────────────────────────────────

    def _chat_reply(self, user_input: str, state: BookingState, language: Optional[str] = None) -> str:
        state.language = _normalize_language(language or state.language)
        if not self._client:
            return _localized_static(state.language, "no_client")
        try:
            # Query backend room types database dynamically
            rooms = _get_backend_room_types()
            if rooms:
                rooms_info = "Các loại phòng có sẵn tại Elysian Smart Hotel:\n"
                for r in rooms:
                    img = r.get('primaryImageUrl', '')
                    rooms_info += f"[ROOM_CARD: {r['name']} | {float(r['basePrice'])/1_000_000:.1f}M ₫/đêm | {r['totalCapacity']} người lớn + {r['childCapacity']} trẻ em | {img}]\n"
            else:
                rooms_info = (
                    "Các loại phòng mặc định tại Elysian Smart Hotel:\n"
                    "- Standard Room: 1.5M ₫/đêm (Tối đa: 2 người)\n"
                    "- Deluxe Room: 2.5M ₫/đêm (Tối đa: 3 người)\n"
                    "- Executive Suite: 4.5M ₫/đêm (Tối đa: 4 người)"
                )
            
            from google.genai import types
            
            # Tiêm ngữ cảnh người dùng vào prompt và cache bookings vào state
            user_context, fetched_bookings = _get_user_context(state.access_token)
            if fetched_bookings:
                state.cached_bookings = fetched_bookings  # Cập nhật cache
            system_instruction = f"{_BOOKING_SYSTEM}\n\n{_language_instruction(state.language)}\n\n{rooms_info}\n\n{user_context}"
            
            contents = _build_contents_with_history(user_input, state.chat_history, max_history_len=6)
            
            # 💡 OPTIMIZATION 3: Context Caching Hint
            # Trong thực tế, nếu dữ liệu phòng `rooms_info` + `system_instruction` vượt quá 32,768 tokens,
            # bạn nên tạo context cache như sau để tiết kiệm 90% chi phí và tăng tốc độ xử lý:
            # cache = self._client.caches.create(
            #     model=_GEMINI_MODEL,
            #     config=types.CreateCachedContentConfig(
            #         contents=[types.Content(role="user", parts=[types.Part.from_text(text=system_instruction)])],
            #         ttl="300s"
            #     )
            # )
            # config = types.GenerateContentConfig(cached_content=cache.name, tools=[...], ...)

            config = types.GenerateContentConfig(
                system_instruction=system_instruction,
                temperature=0.7,
                # 💡 Tools for function calling - NOTE: response_schema/response_mime_type
                # cannot be used simultaneously with tools as they conflict in Gemini API.
                # Function calling takes priority when tools are present.
                #
                # ⚠️ IMPORTANT: Disable Automatic Function Calling (AFC).
                # By default the SDK auto-executes tool functions and feeds dummy results
                # back to the model, bypassing our real _process_* handlers entirely.
                # We intercept response.function_calls manually below instead.
                automatic_function_calling=types.AutomaticFunctionCallingConfig(disable=True),
                tools=[create_booking_summary, submit_final_booking, cancel_booking,
                       cancel_completed_booking, get_booking_invoice, generate_qr_checkin_token,
                       submit_room_change_request, submit_stay_extension_request, submit_early_checkout_request,
                       submit_refund_request],
            )
            
            response = self._client.models.generate_content(
                model=_GEMINI_MODEL,
                contents=contents,
                config=config,
            )
            
            # Xử lý Function Calling (Human-in-the-loop interception)
            logger.info(f"[CHAT] function_calls={[fc.name for fc in response.function_calls] if response.function_calls else None}")
            if response.function_calls:
                for function_call in response.function_calls:
                    if function_call.name == "create_booking_summary":
                        # Convert args to dict
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_booking_summary_tool(args_dict, state, state.language)
                    elif function_call.name == "submit_final_booking":
                        msg, _ = self._submit_booking(state, state.language)
                        return msg
                    elif function_call.name == "cancel_booking":
                        state.step = BookingStep.CANCELLED
                        return _localized_static(state.language, "cancelled")
                    elif function_call.name == "cancel_completed_booking":
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_cancel_completed_booking(args_dict, state)
                    elif function_call.name == "get_booking_invoice":
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_get_booking_invoice(args_dict, state)
                    elif function_call.name == "generate_qr_checkin_token":
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_generate_qr_checkin_token(args_dict, state)
                    elif function_call.name == "submit_room_change_request":
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_submit_room_change_request(args_dict, state)
                    elif function_call.name == "submit_stay_extension_request":
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_submit_stay_extension_request(args_dict, state)
                    elif function_call.name == "submit_early_checkout_request":
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_submit_early_checkout_request(args_dict, state)
                    elif function_call.name == "submit_refund_request":
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_submit_refund_request(args_dict, state)
                        
            text_resp = response.text
            if text_resp:
                # Try to parse JSON if Gemini returns structured output
                try:
                    data = json.loads(text_resp)
                    if isinstance(data, dict) and "reply_message" in data:
                        # Sync extracted booking state info
                        if data.get("extracted_room_type"):
                            state.room_type_name = data["extracted_room_type"]
                        if data.get("extracted_check_in"):
                            try:
                                state.check_in = date.fromisoformat(data["extracted_check_in"])
                            except:
                                pass
                        if data.get("extracted_check_out"):
                            try:
                                state.check_out = date.fromisoformat(data["extracted_check_out"])
                            except:
                                pass
                        if data.get("extracted_adults"):
                            state.adults = int(data["extracted_adults"])
                        if data.get("extracted_children"):
                            state.children = int(data["extracted_children"])
                        if data.get("extracted_checkin_method"):
                            state.check_in_method = data["extracted_checkin_method"]
                        if data.get("extracted_quantity"):
                            state.quantity = int(data["extracted_quantity"])
                        return data.get("reply_message", _localized_static(state.language, "default_confirm"))
                except Exception:
                    pass
                # Plain text response (expected when tools are enabled)
                return text_resp.strip()
                    
            return _localized_static(state.language, "default_confirm")
        except Exception as e:
            logger.error("Chat reply error: %s", e)
            err_msg = str(e).lower()
            if "429" in err_msg or "quota" in err_msg or "resource_exhausted" in err_msg or "exhausted" in err_msg:
                return _localized_static(state.language, "quota_error")
            return _localized_static(state.language, "generic_chat_error")

    # ── Customer Self-Service Handlers ───────────────────────────────────────

    def _get_booking_id(self, booking_number: str, access_token: str, state: Optional["BookingState"] = None) -> Optional[int]:
        """Tra cứu booking_id từ cache trước, nếu không có thì gọi API (có retry)."""
        # 1. Thử cache trước (không tốn network)
        if state and state.cached_bookings:
            for b in state.cached_bookings:
                if b.get("bookingNumber") == booking_number:
                    logger.info("[BOOKING_ID] Tìm thấy %s trong cache.", booking_number)
                    return b.get("bookingId")
            logger.info("[BOOKING_ID] %s không có trong cache, thử gọi API.", booking_number)

        # 2. Fallback: gọi API với retry
        bookings = _get_user_bookings(access_token)
        for b in bookings:
            if b.get("bookingNumber") == booking_number:
                # Cập nhật cache nếu có state
                if state:
                    state.cached_bookings = bookings
                return b.get("bookingId")
        return None

    def _process_cancel_completed_booking(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập để thực hiện chức năng hủy đặt phòng."
        b_num = args.get("booking_number")
        reason = args.get("cancellation_reason", "Hủy qua trợ lý AI")
        if not b_num:
            return "Vui lòng cung cấp mã đặt phòng (dạng BK...) để tiến hành hủy."
        # Ensure reason is at least 5 chars (backend validation)
        if not reason or len(reason.strip()) < 5:
            reason = "Hủy qua trợ lý AI - không có lý do"
        b_id = self._get_booking_id(b_num, state.access_token, state)
        logger.info(f"[CANCEL] booking_number={b_num}, booking_id={b_id}, reason={reason}")
        if not b_id:
            return f"Không tìm thấy đơn đặt phòng nào của bạn có mã `{b_num}`. Vui lòng kiểm tra lại mã đặt phòng."
        try:
            resp = requests.delete(
                f"{_BACKEND_URL}/bookings/{b_id}/cancel",
                json={"cancellationReason": reason},
                headers={
                    "Authorization": f"Bearer {state.access_token}",
                    "Content-Type": "application/json"
                },
                timeout=10
            )
            logger.info(f"[CANCEL] Response status={resp.status_code}, body={resp.text[:300]}")
            if resp.status_code == 200:
                data = resp.json().get("data", {})
                status_val = data.get("status", "Cancelled")
                return (
                    f"✅ Đã hủy thành công đơn đặt phòng **`{b_num}`**.\n"
                    f"- Trạng thái: **{status_val}**\n"
                    f"- Lý do hủy: {reason}\n\n"
                    f"Nếu đơn có thanh toán, vui lòng liên hệ nhân viên khách sạn để được hoàn tiền theo chính sách."
                )
            # Non-200 response
            try:
                err_body = resp.json()
                err_msg = err_body.get("message") or err_body.get("error") or str(err_body)
            except Exception:
                err_msg = resp.text[:200] if resp.text else f"HTTP {resp.status_code}"
            logger.warning(f"[CANCEL] Backend error for booking {b_num}: {err_msg}")
            return f"❌ Không thể hủy đặt phòng `{b_num}`: {err_msg}"
        except requests.exceptions.ConnectionError:
            return "❌ Không kết nối được với hệ thống. Vui lòng thử lại sau hoặc liên hệ nhân viên khách sạn."
        except requests.exceptions.Timeout:
            return "❌ Hệ thống phản hồi chậm. Vui lòng thử lại sau."
        except Exception as e:
            logger.error(f"[CANCEL] Unexpected error for booking {b_num}: {e}", exc_info=True)
            return f"❌ Đã xảy ra lỗi khi hủy phòng: {e}"

    def _process_get_booking_invoice(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập để xem hóa đơn."
        b_num = args.get("booking_number")
        b_id = self._get_booking_id(b_num, state.access_token, state)
        if not b_id:
            return f"Không tìm thấy đơn đặt phòng nào của bạn có mã `{b_num}`."
        try:
            resp = requests.get(f"{_BACKEND_URL}/bookings/{b_id}/invoice",
                                headers={"Authorization": f"Bearer {state.access_token}"}, timeout=10)
            if resp.status_code == 200:
                inv = resp.json().get("data", {})
                return (f"**Hóa đơn phòng `{b_num}`:**\n"
                        f"- Tiền phòng: {float(inv.get('roomTotal', 0))/1_000_000:.2f}M ₫\n"
                        f"- Tiền dịch vụ: {float(inv.get('serviceTotal', 0))/1_000_000:.2f}M ₫\n"
                        f"- Thuế: {float(inv.get('taxAmount', 0))/1_000_000:.2f}M ₫\n"
                        f"- Tổng tiền: **{float(inv.get('finalAmount', 0))/1_000_000:.2f}M ₫**\n"
                        f"- Đã thanh toán: {float(inv.get('paidAmount', 0))/1_000_000:.2f}M ₫\n"
                        f"- Còn nợ: **{float(inv.get('dueAmount', 0))/1_000_000:.2f}M ₫**")
            return f"Lỗi lấy hóa đơn: {resp.json().get('message', 'Không xác định')}."
        except Exception as e:
            return f"Đã xảy ra lỗi khi lấy hóa đơn: {e}"

    def _process_generate_qr_checkin_token(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập để tạo mã QR."
        b_num = args.get("booking_number")
        b_id = self._get_booking_id(b_num, state.access_token, state)
        if not b_id:
            return f"Không tìm thấy đơn đặt phòng nào có mã `{b_num}`."
        try:
            resp = requests.post(f"{_BACKEND_URL}/checkin/{b_id}/qr-token",
                                 headers={"Authorization": f"Bearer {state.access_token}"}, timeout=10)
            if resp.status_code == 200:
                data = resp.json().get("data", {})
                token = data.get("token")
                return f"Tạo mã QR thành công! Mã của bạn là: **`{token}`**.\nVui lòng xuất trình mã này tại Kiosk Check-in."
            return f"Lỗi tạo QR: {resp.json().get('message', 'Không xác định')}."
        except Exception as e:
            return f"Đã xảy ra lỗi khi tạo QR: {e}"

    def _process_submit_room_change_request(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập để yêu cầu đổi phòng."
        b_num = args.get("booking_number")
        new_type = args.get("new_room_type", "")
        reason = args.get("reason", "Yêu cầu đổi hạng phòng")
        b_id = self._get_booking_id(b_num, state.access_token, state)
        if not b_id:
            return f"Không tìm thấy đơn đặt phòng mã `{b_num}`."
        
        # Map new_type to roomTypeId
        db_rooms = _get_backend_room_types()
        matched_room = next((r for r in db_rooms if new_type.lower() in r['name'].lower()), None)
        if not matched_room:
            return f"Không tìm thấy loại phòng '{new_type}'. Các loại phòng: " + ", ".join([r['name'] for r in db_rooms])
        
        payload = {"bookingId": b_id, "newRoomId": matched_room["id"], "reason": reason}
        try:
            resp = requests.post(f"{_BACKEND_URL}/stay-adjustments/room-change/request",
                                 json=payload, headers={"Authorization": f"Bearer {state.access_token}"}, timeout=10)
            if resp.status_code == 200:
                return f"Đã gửi yêu cầu đổi phòng sang {matched_room['name']} cho đơn `{b_num}` thành công. Vui lòng chờ lễ tân duyệt."
            return f"Lỗi yêu cầu đổi phòng: {resp.json().get('message', 'Không xác định')}."
        except Exception as e:
            return f"Lỗi kết nối: {e}"

    def _process_submit_stay_extension_request(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập."
        b_num = args.get("booking_number")
        new_date = args.get("new_checkout_date")
        reason = args.get("reason", "Yêu cầu gia hạn lưu trú")
        b_id = self._get_booking_id(b_num, state.access_token, state)
        if not b_id:
            return f"Không tìm thấy mã `{b_num}`."
        payload = {"bookingId": b_id, "newCheckOutDate": new_date, "description": reason}
        try:
            resp = requests.post(f"{_BACKEND_URL}/stay-adjustments/extension/request",
                                 json=payload, headers={"Authorization": f"Bearer {state.access_token}"}, timeout=10)
            if resp.status_code == 200:
                return f"Đã gửi yêu cầu gia hạn trả phòng đến ngày {new_date} thành công. Vui lòng chờ duyệt."
            return f"Lỗi yêu cầu gia hạn: {resp.json().get('message')}."
        except Exception as e:
            return f"Lỗi kết nối: {e}"

    def _process_submit_early_checkout_request(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập."
        b_num = args.get("booking_number")
        new_date = args.get("new_checkout_date")
        reason = args.get("reason", "Yêu cầu check-out sớm")
        b_id = self._get_booking_id(b_num, state.access_token, state)
        if not b_id:
            return f"Không tìm thấy mã `{b_num}`."
        payload = {"bookingId": b_id, "newCheckOutDate": new_date, "description": reason}
        try:
            resp = requests.post(f"{_BACKEND_URL}/stay-adjustments/early-checkout/request",
                                 json=payload, headers={"Authorization": f"Bearer {state.access_token}"}, timeout=10)
            if resp.status_code == 200:
                return f"Đã gửi yêu cầu check-out sớm vào ngày {new_date} thành công. Vui lòng chờ duyệt."
            return f"Lỗi yêu cầu check-out sớm: {resp.json().get('message')}."
        except Exception as e:
            return f"Lỗi kết nối: {e}"

    def _process_submit_refund_request(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập để yêu cầu hoàn tiền."
        b_num = args.get("booking_number")
        reason = args.get("reason", "Yêu cầu hoàn tiền")
        b_id = self._get_booking_id(b_num, state.access_token, state)
        if not b_id:
            return f"Không tìm thấy mã `{b_num}`."
        
        payload = {"reason": reason}
        try:
            resp = requests.post(f"{_BACKEND_URL}/bookings/{b_id}/refund-request",
                                 json=payload, headers={"Authorization": f"Bearer {state.access_token}"}, timeout=10)
            if resp.status_code == 200:
                return f"Đã gửi yêu cầu hoàn tiền cho mã `{b_num}` thành công. Vui lòng chờ bộ phận kế toán duyệt."
            return f"Lỗi yêu cầu hoàn tiền: {resp.json().get('message', 'Lỗi không xác định')}."
        except Exception as e:
            return f"Lỗi kết nối: {e}"


def _build_contents_with_history(user_input: str, chat_history: list, max_history_len: int = 6) -> list:
    """
    Format chat history and current input into Gemini API contents structure.
    Ensures that turns strictly alternate between 'user' and 'model' as required by Gemini API.
    Limits the history length to max_history_len to prevent token bloat and rate limits.
    """
    raw_turns = []
    
    # Process and normalize all history messages
    for msg in chat_history:
        role = "user" if msg.get("role") == "user" else "model"
        content = msg.get("content", "")
        if len(content) > 1000:
            content = content[:1000] + "... [nội dung đã rút gọn]"
        
        if content.strip():
            raw_turns.append({"role": role, "text": content})
            
    # Merge consecutive turns of the same role
    merged_turns = []
    for turn in raw_turns:
        if not merged_turns:
            merged_turns.append(turn)
        else:
            last_turn = merged_turns[-1]
            if last_turn["role"] == turn["role"]:
                last_turn["text"] += "\n" + turn["text"]
            else:
                merged_turns.append(turn)
                 
    # Limit to the last max_history_len merged turns
    recent_turns = merged_turns[-max_history_len:] if merged_turns else []
    
    # Build the final contents array
    contents = []
    for turn in recent_turns:
        contents.append({"role": turn["role"], "parts": [{"text": turn["text"]}]})
        
    # Append the current user query. 
    # If the last turn in recent_turns was also 'user', we merge it, otherwise append.
    if contents and contents[-1]["role"] == "user":
        contents[-1]["parts"][0]["text"] += "\n" + user_input
    else:
        contents.append({"role": "user", "parts": [{"text": user_input}]})
        
    return contents


# --- END OF FILE ---
