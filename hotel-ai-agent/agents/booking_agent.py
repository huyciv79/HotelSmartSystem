"""
booking_agent.py
================
Agent hỗ trợ đặt phòng khách sạn qua hội thoại đa bước.
Tích hợp sâu với Spring Boot backend API.
Sử dụng google-genai SDK mới.
"""
from __future__ import annotations

import logging
import os
import re
from dataclasses import dataclass, field
from datetime import date, datetime, timedelta
from enum import Enum
from typing import Any, Optional

import requests
from dotenv import load_dotenv

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
"""

def _get_backend_room_types() -> list[dict]:
    """Lấy danh sách loại phòng từ backend."""
    try:
        resp = requests.get(f"{_BACKEND_URL}/room-types", timeout=5)
        if resp.status_code == 200:
            return resp.json().get("data", {}).get("content", [])
    except Exception as e:
        logger.error("Lỗi khi kết nối Spring Boot backend để lấy loại phòng: %s", e)
    return []

def _get_user_context(access_token: str) -> str:
    """Lấy thông tin cá nhân và lịch sử đặt phòng của user từ backend."""
    if not access_token:
        return "Thông tin người dùng: Khách chưa đăng nhập."
    
    try:
        headers = {"Authorization": f"Bearer {access_token}"}
        # 1. Lấy lịch sử booking
        resp = requests.get(f"{_BACKEND_URL}/bookings/history", headers=headers, timeout=5)
        if resp.status_code == 200:
            bookings = resp.json().get("data", [])
            if not bookings:
                return "Thông tin người dùng: Đã đăng nhập nhưng hiện tại chưa có booking nào."
            
            context_str = "Thông tin người dùng: Khách đã đăng nhập.\nDanh sách các Booking của khách (Ngữ Cảnh Hệ Thống):\n"
            for b in bookings:
                ref = b.get('bookingReference', 'N/A')
                status = b.get('status', 'N/A')
                room = b.get('roomTypeName', 'N/A')
                ci = b.get('checkInDate', 'N/A')
                co = b.get('checkOutDate', 'N/A')
                price = b.get('finalAmount', 0)
                price_str = f"{float(price)/1_000_000:.2f}M ₫"
                method = b.get('checkInMethod', 'N/A')
                context_str += f"- Mã Booking: {ref} | Trạng thái: {status} | Phòng: {room} | Check-in: {ci} | Check-out: {co} | Tổng tiền: {price_str} | Phương thức: {method}\n"
            return context_str
        elif resp.status_code == 401:
            return "Thông tin người dùng: Phiên đăng nhập đã hết hạn, yêu cầu khách đăng nhập lại."
    except Exception as e:
        logger.error("Lỗi khi kết nối Spring Boot backend để lấy lịch sử: %s", e)
        return "Thông tin người dùng: Lỗi kết nối máy chủ khi lấy dữ liệu."
    
    return "Thông tin người dùng: Không thể lấy thông tin."


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
            
            create_booking_summary_tool = types.FunctionDeclaration(
                name="create_booking_summary",
                description="Tạo tóm tắt đặt phòng để khách hàng xác nhận. Gọi hàm này SAU KHI đã thu thập ĐỦ thông tin từ khách.",
                parameters=types.Schema(
                    type="OBJECT",
                    properties={
                        "room_type": types.Schema(type="STRING"),
                        "check_in_date": types.Schema(type="STRING", description="YYYY-MM-DD"),
                        "check_out_date": types.Schema(type="STRING", description="YYYY-MM-DD"),
                        "adults": types.Schema(type="INTEGER"),
                        "children": types.Schema(type="INTEGER"),
                        "checkin_method": types.Schema(type="STRING")
                    },
                    required=["room_type", "check_in_date", "check_out_date", "adults"]
                )
            )

            submit_final_booking_tool = types.FunctionDeclaration(
                name="submit_final_booking",
                description="Gọi hàm này để chốt đơn đặt phòng khi và chỉ khi khách hàng đã đồng ý/xác nhận với tóm tắt đặt phòng.",
                parameters=types.Schema(
                    type="OBJECT",
                    properties={}
                )
            )

            cancel_booking_tool = types.FunctionDeclaration(
                name="cancel_booking",
                description="Gọi hàm này khi khách hàng từ chối tóm tắt đặt phòng, muốn hủy yêu cầu đặt phòng hiện tại, hoặc không muốn tiếp tục đặt phòng nữa.",
                parameters=types.Schema(
                    type="OBJECT",
                    properties={}
                )
            )

            tool = types.Tool(
                function_declarations=[create_booking_summary_tool, submit_final_booking_tool, cancel_booking_tool]
            )
            
            config = types.GenerateContentConfig(
                system_instruction=system_instruction,
                temperature=0.7,
                tools=[tool]
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
                        # Convert args to dict in case it's a proto mapping or object
                        args_dict = dict(function_call.args) if function_call.args else {}
                        return self._process_booking_summary_tool(args_dict, state)
                    elif function_call.name == "submit_final_booking":
                        msg, _ = self._submit_booking(state)
                        return msg
                    elif function_call.name == "cancel_booking":
                        state.step = BookingStep.CANCELLED
                        return "Đã hủy yêu cầu đặt phòng hiện tại. Bạn cần hỗ trợ gì thêm không?"
                        
            text_resp = response.text
            return text_resp.strip() if text_resp else "Xác nhận yêu cầu."
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
                # Merge content
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
