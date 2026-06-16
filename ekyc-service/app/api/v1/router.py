"""
API v1 Router – gộp tất cả endpoint của v1
"""

from fastapi import APIRouter
from app.api.v1.endpoints.ekyc import router as ekyc_router

api_v1_router = APIRouter()
api_v1_router.include_router(ekyc_router)
