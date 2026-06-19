package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.IdempotentRecord;
import com.example.hotelsmartbookingbackend.dto.request.CaptureOrderRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateOrderRequest;
import com.example.hotelsmartbookingbackend.dto.request.BankTransferRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.PaypalCaptureResponse;
import com.example.hotelsmartbookingbackend.dto.response.PaypalOrderResponse;
import com.example.hotelsmartbookingbackend.service.IdempotencyService;
import com.example.hotelsmartbookingbackend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/payments/paypal")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PaymentController.class);

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<PaypalOrderResponse>> createOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String xIdempotencyKey,
            @Valid @RequestBody CreateOrderRequest request) {

        String key = idempotencyKey != null ? idempotencyKey : xIdempotencyKey;
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Idempotency-Key header is required"));
        }

        // 1. Check idempotency record
        Optional<IdempotentRecord> recordOpt = idempotencyService.getRecord(key);
        if (recordOpt.isPresent()) {
            IdempotentRecord record = recordOpt.get();
            if ("PROCESSING".equals(record.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("Your request is currently being processed. Please wait."));
            }
            // If already succeeded, return the cached response
            ApiResponse<PaypalOrderResponse> cachedResponse = deserializeResponse(record.getResponseBody(), PaypalOrderResponse.class);
            return ResponseEntity.status(record.getResponseStatus()).body(cachedResponse);
        }

        // 2. Start atomic operation
        boolean isNew = idempotencyService.startOperation(key);
        if (!isNew) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Conflict: request already in progress"));
        }

        try {
            PaypalOrderResponse orderResponse = paymentService.createOrder(request, key);
            ApiResponse<PaypalOrderResponse> apiResponse = ApiResponse.success("PayPal order created successfully", orderResponse);
            
            idempotencyService.completeOperation(key, HttpStatus.OK.value(), apiResponse);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error creating PayPal order with idempotency key {}: ", key, e);
            idempotencyService.removeKey(key);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/capture-order")
    public ResponseEntity<ApiResponse<PaypalCaptureResponse>> captureOrder(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String xIdempotencyKey,
            @Valid @RequestBody CaptureOrderRequest request) {

        String key = idempotencyKey != null ? idempotencyKey : xIdempotencyKey;
        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Idempotency-Key header is required"));
        }

        // 1. Check idempotency record
        Optional<IdempotentRecord> recordOpt = idempotencyService.getRecord(key);
        if (recordOpt.isPresent()) {
            IdempotentRecord record = recordOpt.get();
            if ("PROCESSING".equals(record.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.error("Your request is currently being processed. Please wait."));
            }
            ApiResponse<PaypalCaptureResponse> cachedResponse = deserializeResponse(record.getResponseBody(), PaypalCaptureResponse.class);
            return ResponseEntity.status(record.getResponseStatus()).body(cachedResponse);
        }

        // 2. Start atomic operation
        boolean isNew = idempotencyService.startOperation(key);
        if (!isNew) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Conflict: request already in progress"));
        }

        try {
            PaypalCaptureResponse captureResponse = paymentService.captureOrder(request, key);
            ApiResponse<PaypalCaptureResponse> apiResponse = ApiResponse.success("PayPal payment captured successfully", captureResponse);

            idempotencyService.completeOperation(key, HttpStatus.OK.value(), apiResponse);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.error("Error capturing PayPal order with idempotency key {}: ", key, e);
            idempotencyService.removeKey(key);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/bank-transfer")
    public ResponseEntity<ApiResponse<String>> processBankTransfer(
            @Valid @RequestBody BankTransferRequest request) {
        try {
            paymentService.processBankTransfer(request);
            return ResponseEntity.ok(ApiResponse.success("Bank transfer payment processed successfully", "SUCCESS"));
        } catch (Exception e) {
            log.error("Error processing bank transfer payment: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private <T> ApiResponse<T> deserializeResponse(Object body, Class<T> dataType) {
        if (body instanceof ApiResponse) {
            return (ApiResponse<T>) body;
        }
        if (body instanceof java.util.Map) {
            try {
                java.util.Map<?, ?> map = (java.util.Map<?, ?>) body;
                boolean success = Boolean.TRUE.equals(map.get("success"));
                String message = (String) map.get("message");
                Object dataObj = map.get("data");
                T data = null;
                if (dataObj != null) {
                    if (dataType.isInstance(dataObj)) {
                        data = (T) dataObj;
                    } else if (dataObj instanceof java.util.Map) {
                        java.util.Map<?, ?> dataMap = (java.util.Map<?, ?>) dataObj;
                        if (dataType == PaypalOrderResponse.class) {
                            data = (T) PaypalOrderResponse.builder()
                                    .paypalOrderId((String) dataMap.get("paypalOrderId"))
                                    .approveUrl((String) dataMap.get("approveUrl"))
                                    .status((String) dataMap.get("status"))
                                    .build();
                        } else if (dataType == PaypalCaptureResponse.class) {
                            Number amtNum = (Number) dataMap.get("amount");
                            java.math.BigDecimal amt = amtNum != null ? new java.math.BigDecimal(amtNum.toString()) : java.math.BigDecimal.ZERO;
                            data = (T) PaypalCaptureResponse.builder()
                                    .paypalOrderId((String) dataMap.get("paypalOrderId"))
                                    .transactionId((String) dataMap.get("transactionId"))
                                    .status((String) dataMap.get("status"))
                                    .amount(amt)
                                    .currency((String) dataMap.get("currency"))
                                    .build();
                        }
                    }
                }
                return new ApiResponse<>(success, message, data);
            } catch (Exception e) {
                log.error("Deserialization fallback error: ", e);
            }
        }
        return ApiResponse.error("Unable to deserialize response");
    }
}
