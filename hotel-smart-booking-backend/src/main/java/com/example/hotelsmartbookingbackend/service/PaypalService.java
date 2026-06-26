package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.response.PaypalCaptureResponse;
import com.example.hotelsmartbookingbackend.dto.response.PaypalOrderResponse;

import java.math.BigDecimal;

public interface PaypalService {
    String getAccessToken();
    PaypalOrderResponse createPaypalOrder(Integer bookingId, BigDecimal amount, String idempotencyKey);
    PaypalCaptureResponse capturePaypalOrder(String paypalOrderId, String idempotencyKey);
    void refundPaypalCapture(String captureId, BigDecimal usdAmount, String idempotencyKey);
}
