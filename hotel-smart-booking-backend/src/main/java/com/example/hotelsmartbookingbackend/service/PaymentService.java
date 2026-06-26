package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.CaptureOrderRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateOrderRequest;
import com.example.hotelsmartbookingbackend.dto.request.BankTransferRequest;
import com.example.hotelsmartbookingbackend.dto.response.PaypalCaptureResponse;
import com.example.hotelsmartbookingbackend.dto.response.PaypalOrderResponse;

import com.example.hotelsmartbookingbackend.dto.request.ManualPaymentRequest;

public interface PaymentService {
    PaypalOrderResponse createOrder(CreateOrderRequest request, String idempotencyKey);
    PaypalCaptureResponse captureOrder(CaptureOrderRequest request, String idempotencyKey);
    void processBankTransfer(BankTransferRequest request);
    void processManualPayment(ManualPaymentRequest request, String staffEmail);
}

