package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.ManualPaymentRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentManualController {

    private final PaymentService paymentService;

    @PostMapping("/manual")
    public ResponseEntity<ApiResponse<String>> processManualPayment(
            @Valid @RequestBody ManualPaymentRequest request,
            Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                throw new RuntimeException("Bạn cần đăng nhập để thực hiện chức năng này");
            }
            String staffEmail = authentication.getName();
            paymentService.processManualPayment(request, staffEmail);
            return ResponseEntity.ok(ApiResponse.success("Ghi nhận thanh toán tại quầy thành công", "SUCCESS"));
        } catch (Exception e) {
            log.error("Error processing manual counter payment: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }
}
