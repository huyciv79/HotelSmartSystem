"""
hotel_agent_api.py
==================
FastAPI REST API server for Elysian AI Hotel Assistant.
Exposes endpoints for chat, hotel search, booking management, and price comparison.
"""
from __future__ import annotations

import os
import sys
import re
import logging
from datetime import date, datetime
from typing import Any, Optional, List
from dotenv import load_dotenv

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

# Path Setup
sys.path.insert(0, os.path.dirname(__file__))
load_dotenv()

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

from agents.booking_agent import BookingAgent, BookingState, BookingStep

app = FastAPI(
    title="🏨 Elysian AI Hotel Assistant API",
    description="REST API for intelligent hotel search and multi-step booking agent.",
    version="1.0.0"
)

# CORS Configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Allow all origins for dev environment. Adjust for production.
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize agents
booking_agent = BookingAgent()

# ── Pydantic Request/Response Schemas ─────────────────────────────────────────

class BookingStateSchema(BaseModel):
    step: str = "idle"
    hotel: Optional[dict] = None
    room_type_id: Optional[int] = None
    room_type_name: str = ""
    check_in: Optional[str] = None  # Format: YYYY-MM-DD
    check_out: Optional[str] = None # Format: YYYY-MM-DD
    adults: int = 1
    children: int = 0
    check_in_method: str = "Manual"
    special_requests: str = ""
    access_token: str = ""
    booking_result: Optional[dict] = None
    error: str = ""
    chat_history: List[dict] = []

class ChatRequest(BaseModel):
    message: str
    state: BookingStateSchema

class ChatResponse(BaseModel):
    response: str
    state: BookingStateSchema
    search_results: Optional[dict] = None

class StartBookingRequest(BaseModel):
    hotel: dict
    state: BookingStateSchema

class StartBookingResponse(BaseModel):
    response: str
    state: BookingStateSchema

class CancelBookingRequest(BaseModel):
    state: BookingStateSchema

class CancelBookingResponse(BaseModel):
    response: str
    state: BookingStateSchema



# ── Mapping Helpers ──────────────────────────────────────────────────────────

def schema_to_state(s: BookingStateSchema) -> BookingState:
    ci = None
    co = None
    if s.check_in:
        try:
            ci = datetime.strptime(s.check_in, "%Y-%m-%d").date()
        except ValueError:
            pass
    if s.check_out:
        try:
            co = datetime.strptime(s.check_out, "%Y-%m-%d").date()
        except ValueError:
            pass

    # Map step string to enum
    try:
        step_enum = BookingStep(s.step)
    except ValueError:
        step_enum = BookingStep.IDLE

    return BookingState(
        step=step_enum,
        hotel=s.hotel,
        room_type_id=s.room_type_id,
        room_type_name=s.room_type_name,
        check_in=ci,
        check_out=co,
        adults=s.adults,
        children=s.children,
        check_in_method=s.check_in_method,
        special_requests=s.special_requests,
        access_token=s.access_token,
        booking_result=s.booking_result,
        error=s.error,
        chat_history=s.chat_history
    )

def state_to_schema(st: BookingState) -> BookingStateSchema:
    ci_str = st.check_in.isoformat() if st.check_in else None
    co_str = st.check_out.isoformat() if st.check_out else None
    return BookingStateSchema(
        step=st.step.value,
        hotel=st.hotel,
        room_type_id=st.room_type_id,
        room_type_name=st.room_type_name,
        check_in=ci_str,
        check_out=co_str,
        adults=st.adults,
        children=st.children,
        check_in_method=st.check_in_method,
        special_requests=st.special_requests,
        access_token=st.access_token,
        booking_result=st.booking_result,
        error=st.error,
        chat_history=st.chat_history
    )

# ── API Endpoints ─────────────────────────────────────────────────────────────

@app.get("/api/ai/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "ok", "timestamp": datetime.now().isoformat()}

@app.post("/api/ai/chat", response_model=ChatResponse)
async def chat(req: ChatRequest):
    """Processes user input through search or booking agent based on current state."""
    user_input = req.message
    state = schema_to_state(req.state)
    lower = user_input.lower().strip()

    booking_keywords = ["đặt", "book", "thuê", "reserve", "xác nhận", "confirm"]

    search_results = None

    # 1. If currently in booking flow, delegate to booking agent
    if state.step not in (BookingStep.IDLE, BookingStep.CANCELLED, BookingStep.DONE):
        response, new_state = booking_agent.process(user_input, state)
    else:
        # 2. Check intent using word boundaries to prevent matching "book" in "booking"
        booking_pattern = r"\b(đặt|book|thuê|reserve|xác\s+nhận|confirm)\b"
        is_booking = bool(re.search(booking_pattern, lower))
        
        if is_booking:
            # Start booking via chat
            response, new_state = booking_agent.process(user_input, state)
        else:
            # Regular AI assistant chat
            response = booking_agent._chat_reply(user_input, state)
            new_state = state

    # Append to history
    new_state.chat_history.append({"role": "user", "content": user_input})
    new_state.chat_history.append({"role": "assistant", "content": response})

    return ChatResponse(
        response=response,
        state=state_to_schema(new_state),
        search_results=search_results
    )

@app.post("/api/ai/start-booking", response_model=StartBookingResponse)
async def start_booking(req: StartBookingRequest):
    """Directly starts booking flow for a selected hotel."""
    state = schema_to_state(req.state)
    response, new_state = booking_agent.start_booking(req.hotel, state)
    
    # Sync chat history
    new_state.chat_history.append({"role": "assistant", "content": response})
    
    return StartBookingResponse(
        response=response,
        state=state_to_schema(new_state)
    )

@app.post("/api/ai/cancel-booking", response_model=CancelBookingResponse)
async def cancel_booking(req: CancelBookingRequest):
    """Cancels the active booking flow."""
    state = schema_to_state(req.state)
    response, new_state = booking_agent.cancel(state)
    
    new_state.chat_history.append({"role": "assistant", "content": response})
    
    return CancelBookingResponse(
        response=response,
        state=state_to_schema(new_state)
    )



# ── API Status ────────────────────────────────────────────────────────────────

@app.get("/")
async def root_api_status():
    """Returns basic API status metadata."""
    return {
        "status": "active",
        "service": "Elysian AI Hotel Assistant REST API",
        "endpoints": {
            "chat": "/api/ai/chat",
            "start_booking": "/api/ai/start-booking",
            "cancel_booking": "/api/ai/cancel-booking",
            "health": "/api/ai/health"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("hotel_agent_api:app", host="127.0.0.1", port=8000, reload=True)
