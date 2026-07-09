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
import uuid
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
        return price * self.nights

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
2. Để đặt phòng, bạn cần hỏi khách hàng ĐỦ 5 THÔNG TIN sau:
   - Tên loại phòng muốn đặt (ví dụ: Standard, Deluxe, Suite)
   - Ngày Check-in (Ngày nhận phòng, định dạng YYYY-MM-DD)
   - Ngày Check-out (Ngày trả phòng, định dạng YYYY-MM-DD)
   - Số lượng khách (Người lớn và Trẻ em)
   - Hình thức Check-in mong muốn (FaceID, QR Code, hoặc Manual)
3. Hãy chủ động đặt câu hỏi cho những thông tin còn thiếu. Riêng đối với Hình thức check-in, BẮT BUỘC gợi ý các tùy chọn cho khách bằng cú pháp thẻ hành động sau (mỗi thẻ cách nhau bởi dấu gạch đứng, phải nằm riêng trên một dòng): `[ACTIONS: Tùy chọn 1 | Tùy chọn 2 | ...]`. Ví dụ: `[ACTIONS: FaceID | QR Code | Tại quầy]`. Tuyệt đối KHÔNG dùng cú pháp `[ACTIONS]` cho Ngày tháng hay Số lượng người.
4. Khi giới thiệu các loại phòng, bạn BẮT BUỘC sử dụng ĐÚNG định dạng thẻ giao diện sau để khách dễ chọn (chỉ thay thế thông tin, tuyệt đối không sửa ngoặc vuông): `[ROOM_CARD: Tên phòng | Giá | Sức chứa | URL_ảnh]`. Không dùng gạch đầu dòng cho danh sách phòng.
5. KHI VÀ CHỈ KHI ĐÃ CÓ ĐỦ CẢ 5 THÔNG TIN TRÊN, bạn BẮT BUỘC PHẢI GỌI HÀM (Function Call) `create_booking_summary` để tạo tóm tắt đặt phòng. KHÔNG tự trả lời bằng văn bản khi đã đủ thông tin.
6. SAU KHI ĐÃ TẠO TÓM TẮT ĐẶT Phòng, hãy chờ khách hàng xác nhận. NẾU khách hàng đồng ý (ví dụ: "ok", "xác nhận"), BẮT BUỘC GỌI HÀM `submit_final_booking` để chốt đơn. NẾU khách hàng từ chối hoặc muốn hủy, BẮT BUỘC GỌI HÀM `cancel_booking`.
7. Hỗ trợ khách hàng KIỂM TRA thông tin cá nhân và lịch sử đặt phòng nếu họ hỏi (dựa trên Ngữ Cảnh Hệ Thống).
8. Hệ thống CHỈ HỖ TRỢ THANH TOÁN TRỰC TUYẾN QUA CỔNG PAYPAL.
9. Hỗ trợ khách HỦY đặt phòng ĐÃ THÀNH CÔNG bằng cách gọi `cancel_completed_booking` (yêu cầu mã BK...).
10. Hỗ trợ khách XEM HÓA ĐƠN bằng cách gọi `get_booking_invoice` (yêu cầu mã BK...).
11. Hỗ trợ khách TẠO MÃ QR CHECK-IN bằng cách gọi `generate_qr_checkin_token` (yêu cầu mã BK...).
12. Hỗ trợ khách ĐỔI PHÒNG, GIA HẠN, CHECK-OUT SỚM bằng cách gọi tương ứng `submit_room_change_request`, `submit_stay_extension_request`, `submit_early_checkout_request` (yêu cầu mã BK..., loại phòng/ngày muốn đổi và lý do).
13. Hỗ trợ khách YÊU CẦU HOÀN TIỀN (Refund) bằng cách gọi `submit_refund_request` (yêu cầu mã BK... và lý do).
14. Hỗ trợ khách THANH TOÁN (tạo link PayPal) bằng cách gọi `generate_payment_link` (yêu cầu mã BK...).
"""


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


# ── Native Python Tools definitions ───────────────────────────────────────────
def create_booking_summary(
    room_type: str, 
    check_in_date: str, 
    check_out_date: str, 
    adults: int, 
    children: int = 0, 
    checkin_method: str = "Manual"
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

def generate_payment_link(booking_number: str) -> str:
    """Gọi hàm này khi khách hàng muốn THANH TOÁN cho đơn đặt phòng, hàm sẽ tạo liên kết PayPal."""
    return "generate_payment_link"


def _get_backend_room_types() -> list[dict]:
    """Lấy danh sách loại phòng từ backend."""
    try:
        resp = requests.get(f"{_BACKEND_URL}/room-types", timeout=5)
        if resp.status_code == 200:
            return resp.json().get("data", {}).get("content", [])
    except Exception as e:
        logger.error("Lỗi khi kết nối Spring Boot backend để lấy loại phòng: %s", e)
    return []

def _get_user_bookings(access_token: str) -> list[dict]:
    """Lấy danh sách thô các booking từ backend."""
    if not access_token:
        return []
    try:
        headers = {"Authorization": f"Bearer {access_token}"}
        resp = requests.get(f"{_BACKEND_URL}/bookings/history", headers=headers, timeout=5)
        if resp.status_code == 200:
            return resp.json().get("data", [])
    except Exception as e:
        logger.error("Lỗi khi kết nối backend để lấy danh sách booking: %s", e)
    return []

def _get_user_context(access_token: str) -> str:
    """Lấy thông tin cá nhân và lịch sử đặt phòng của user từ backend."""
    if not access_token:
        return "Thông tin người dùng: Khách chưa đăng nhập."
    
    bookings = _get_user_bookings(access_token)
    if not bookings:
        return "Thông tin người dùng: Đã đăng nhập nhưng hiện tại chưa có booking nào (hoặc lỗi lấy dữ liệu)."
    
    context_str = "Thông tin người dùng: Khách đã đăng nhập.\nDanh sách các Booking của khách (Ngữ Cảnh Hệ Thống):\n"
    for b in bookings:
        ref = b.get('bookingNumber', 'N/A')
        status = b.get('status', 'N/A')
        room = b.get('roomType', 'N/A')
        ci = b.get('checkInDate', 'N/A')
        co = b.get('checkOutDate', 'N/A')
        price = b.get('finalAmount', 0)
        price_str = f"{float(price)/1_000_000:.2f}M ₫" if price else "0₫"
        method = b.get('checkInMethod', 'N/A')
        context_str += f"- Mã Booking: {ref} | Trạng thái: {status} | Phòng: {room} | Check-in: {ci} | Check-out: {co} | Tổng tiền: {price_str} | Phương thức: {method}\n"
    return context_str


class BookingAgent:
    """Agent hội thoại hỗ trợ đặt phòng khách sạn."""

    def __init__(self):
        self._client = _get_client()

    # ── Public API ───────────────────────────────────────────────────────────

    def process(self, user_input: str, state: BookingState) -> tuple[str, BookingState]:
        """Xử lý input người dùng và cập nhật state."""

        if state.step == BookingStep.DONE:
            return "Đặt phòng đã hoàn thành! Bạn muốn tìm khách sạn khác không?", state

        # Không reset step về IDLE nếu đang ở CONFIRM
        if state.step not in (BookingStep.CONFIRM, BookingStep.DONE):
            state.step = BookingStep.IDLE
            
        return self._chat_reply(user_input, state), state

    def process_stream(self, user_input: str, state: BookingState) -> Generator[str, None, None]:
        """
        Phiên bản Streaming của process. Trả về Generator giúp hiển thị chữ chạy thời gian thực.
        """
        if state.step == BookingStep.DONE:
            yield "Đặt phòng đã hoàn thành! Bạn muốn tìm khách sạn khác không?"
            return

        if state.step not in (BookingStep.CONFIRM, BookingStep.DONE):
            state.step = BookingStep.IDLE

        yield self._chat_reply(user_input, state)

    def _process_booking_summary_tool(self, args: dict, state: BookingState) -> str:
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
        state.check_in_method = args.get("checkin_method", "Manual")
        
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
        return self._booking_summary(state)

    def _booking_summary(self, state: BookingState) -> str:
        hotel_name = state.hotel["name"] if state.hotel else "N/A"
        ci = state.check_in.strftime("%d/%m/%Y") if state.check_in else "N/A"
        co = state.check_out.strftime("%d/%m/%Y") if state.check_out else "N/A"
        amt = state.total_amount / 1_000_000
        return (
            f"**Tóm tắt đặt phòng:**\n\n"
            f"- **Khách sạn:** {hotel_name}\n"
            f"- **Phòng:** {state.room_type_name or 'Standard'}\n"
            f"- **Thời gian:** {ci} → {co} ({state.nights} đêm)\n"
            f"- **Khách:** {state.adults} người lớn, {state.children} trẻ em\n"
            f"- **Check-in:** {state.check_in_method}\n"
            f"- **Ước tính:** **{amt:.1f}M ₫** (chưa thuế)\n\n"
            f"Bạn có đồng ý với thông tin trên không? Hãy phản hồi để mình chốt đơn cho bạn nhé!"
        )

    # ── Backend Integration ──────────────────────────────────────────────────

    def _submit_booking(self, state: BookingState) -> tuple[str, BookingState]:
        if not state.access_token:
            # Mô phỏng khi chưa đăng nhập
            state.step = BookingStep.DONE
            ref = f"BK{datetime.now().strftime('%Y%m%d%H%M%S')}"
            state.booking_result = {
                "bookingReference": ref,
                "status": "Confirmed (Demo)",
            }
            return (
            f"**Đặt phòng thành công! (Chế độ Demo)**\n\n"
            f"- Mã đặt phòng: **`{ref}`**\n"
            f"- Trạng thái: Confirmed\n\n"
            f"_Lưu ý: Để đặt phòng thực tế, vui lòng **đăng nhập** qua sidebar._",
            state,
        )

        try:
            payload = {
                "roomTypeId":     state.room_type_id or 1,
                "checkInDate":    state.check_in.isoformat() if state.check_in else None,
                "checkOutDate":   state.check_out.isoformat() if state.check_out else None,
                "checkInMethod":  state.check_in_method,
                "numberOfAdults": state.adults,
                "numberOfChildren": state.children,
                "specialRequests": state.special_requests,
            }
            headers = {
                "Authorization": f"Bearer {state.access_token}",
                "Content-Type":  "application/json",
            }
            resp = requests.post(
                f"{_BACKEND_URL}/bookings",
                json=payload,
                headers=headers,
                timeout=15,
            )
            resp.raise_for_status()
            booking_data = resp.json().get("data", {})
            state.step   = BookingStep.DONE
            state.booking_result = booking_data
            ref = booking_data.get("bookingReference", "N/A")

            return (
                f"**Đặt phòng thành công!**\n\n"
                f"- Mã đặt phòng: **`{ref}`**\n"
                f"- Trạng thái: {booking_data.get('status', 'Confirmed')}\n"
                f"- Tổng tiền: **{float(booking_data.get('finalAmount', 0))/1_000_000:.2f}M ₫**\n\n"
                f"Vui lòng kiểm tra email để nhận xác nhận và hướng dẫn check-in!",
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

    def _chat_reply(self, user_input: str, state: BookingState) -> str:
        if not self._client:
            return (
                "Xin chào! Tôi là trợ lý đặt phòng Elysian AI. 🏨\n\n"
                "Tôi có thể giúp bạn:\n"
                "🔍 **Tìm phòng** tại Elysian Smart Hotel Cần Thơ\n"
                "🛎️ **Đặt phòng** trực tiếp\n\n"
                "Hãy thử: _'Đặt phòng tại Elysian Cần Thơ'_"
            )
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
            
            # Tiêm ngữ cảnh người dùng vào prompt
            user_context = _get_user_context(state.access_token)
            system_instruction = f"{_BOOKING_SYSTEM}\n\n{rooms_info}\n\n{user_context}"
            
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
                # 💡 OPTIMIZATION 1: Sử dụng trực tiếp hàm Python làm Tool
                tools=[create_booking_summary, submit_final_booking, cancel_booking,
                       cancel_completed_booking, get_booking_invoice, generate_qr_checkin_token,
                       submit_room_change_request, submit_stay_extension_request, submit_early_checkout_request,
                       submit_refund_request, generate_payment_link],
                # 💡 OPTIMIZATION 2: Trả về Structured Output dưới dạng JSON khớp với Pydantic schema
                response_mime_type="application/json",
                response_schema=ChatAgentResponse
            )
            
            response = self._client.models.generate_content(
                model=_GEMINI_MODEL,
                contents=contents,
                config=config,
            )
            
            # Xử lý Function Calling (Human-in-the-loop interception)
            if response.function_calls:
                for function_call in response.function_calls:
                    if function_call.name == "create_booking_summary":
                        # Convert args to dict
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_booking_summary_tool(args_dict, state)
                    elif function_call.name == "submit_final_booking":
                        msg, _ = self._submit_booking(state)
                        return msg
                    elif function_call.name == "cancel_booking":
                        state.step = BookingStep.CANCELLED
                        return "Đã hủy yêu cầu đặt phòng hiện tại. Bạn cần hỗ trợ gì thêm không?"
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
                    elif function_call.name == "generate_payment_link":
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_generate_payment_link(args_dict, state)
                        
            text_resp = response.text
            if text_resp:
                try:
                    # Parse Structured JSON Output
                    data = json.loads(text_resp)
                    
                    # Tự động đồng bộ thông tin thu thập được vào state biến
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
                        
                    return data.get("reply_message", "Xác nhận yêu cầu.")
                except Exception as json_err:
                    logger.error("Lỗi parse Structured JSON: %s. Nội dung thô: %s", json_err, text_resp)
                    return text_resp.strip()
                    
            return "Xác nhận yêu cầu."
        except Exception as e:
            logger.error("Chat reply error: %s", e)
            err_msg = str(e).lower()
            if "429" in err_msg or "quota" in err_msg or "resource_exhausted" in err_msg or "exhausted" in err_msg:
                return (
                    "⚠️ **Giới hạn lưu lượng API Gemini (Rate Limit / Quota Exceeded)**\n\n"
                    "API Key của bạn đã vượt quá hạn mức miễn phí (Free Tier) cho phép mỗi phút hoặc mỗi ngày.\n"
                    "👉 *Cách khắc phục:*\n"
                    "1. Vui lòng **đợi khoảng 10-15 giây** rồi gửi lại tin nhắn.\n"
                    "2. Kiểm tra hoặc cập nhật khóa API trả phí ở biến `GOOGLE_API_KEY` trong file `hotel-ai-agent/.env`."
                )
            return "Xin lỗi, tôi gặp sự cố kết nối khi trò chuyện với AI. Bạn hãy thử lại nhé!"

    # ── Customer Self-Service Handlers ───────────────────────────────────────

    def _get_booking_id(self, booking_number: str, access_token: str) -> Optional[int]:
        bookings = _get_user_bookings(access_token)
        for b in bookings:
            if b.get("bookingNumber") == booking_number:
                return b.get("bookingId")
        return None

    def _process_cancel_completed_booking(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập để thực hiện chức năng hủy đặt phòng."
        b_num = args.get("booking_number")
        reason = args.get("cancellation_reason", "Hủy qua trợ lý AI")
        b_id = self._get_booking_id(b_num, state.access_token)
        if not b_id:
            return f"Không tìm thấy đơn đặt phòng nào của bạn có mã `{b_num}`."
        try:
            resp = requests.delete(f"{_BACKEND_URL}/bookings/{b_id}/cancel",
                                   json={"cancellationReason": reason},
                                   headers={"Authorization": f"Bearer {state.access_token}"}, timeout=10)
            if resp.status_code == 200:
                data = resp.json().get("data", {})
                return f"Đã hủy thành công đơn đặt phòng `{b_num}`. Trạng thái hiện tại: {data.get('status', 'Cancelled')}."
            return f"Lỗi hủy phòng: {resp.json().get('message', 'Không xác định')}."
        except Exception as e:
            return f"Đã xảy ra lỗi khi hủy phòng: {e}"

    def _process_get_booking_invoice(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập để xem hóa đơn."
        b_num = args.get("booking_number")
        b_id = self._get_booking_id(b_num, state.access_token)
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
        b_id = self._get_booking_id(b_num, state.access_token)
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
        b_id = self._get_booking_id(b_num, state.access_token)
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
        b_id = self._get_booking_id(b_num, state.access_token)
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
        b_id = self._get_booking_id(b_num, state.access_token)
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
        b_id = self._get_booking_id(b_num, state.access_token)
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

    def _process_generate_payment_link(self, args: dict, state: BookingState) -> str:
        if not state.access_token:
            return "Vui lòng đăng nhập để thanh toán."
        b_num = args.get("booking_number")
        b_id = self._get_booking_id(b_num, state.access_token)
        if not b_id:
            return f"Không tìm thấy mã `{b_num}`."
        
        payload = {"bookingId": b_id, "paymentOption": "FULL"}
        idem_key = str(uuid.uuid4())
        headers = {
            "Authorization": f"Bearer {state.access_token}",
            "Idempotency-Key": idem_key
        }
        try:
            resp = requests.post(f"{_BACKEND_URL}/payments/paypal/create-order",
                                 json=payload, headers=headers, timeout=10)
            if resp.status_code == 200:
                data = resp.json().get("data", {})
                approve_url = data.get("approveUrl", "")
                if approve_url:
                    return f"Vui lòng nhấn vào [Đường link này]({approve_url}) để tiến hành thanh toán an toàn qua PayPal cho đơn `{b_num}`."
                return "Lỗi: Không tìm thấy đường link thanh toán trong phản hồi."
            return f"Lỗi tạo link thanh toán: {resp.json().get('message', 'Có thể do đơn đã thanh toán hoặc lỗi hệ thống')}."
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
