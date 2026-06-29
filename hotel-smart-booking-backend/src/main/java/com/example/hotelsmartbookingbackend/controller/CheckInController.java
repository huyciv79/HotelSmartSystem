package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.QrCheckInRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final BookingService bookingService;

    @PostMapping("/qr")
    @Operation(
            summary = "Validate QR token va check-in",
            description = "Validate QR token con hieu luc, chi dung cho booking QR Code chua check-in, roi tra ve mat khau phong."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> qrCheckIn(
            @Valid @RequestBody QrCheckInRequest request,
            Authentication authentication) {
        String actorEmail = resolveAuthenticatedEmail(authentication);
        BookingResponse response = bookingService.performQrCheckIn(request.getToken(), actorEmail);
        return ResponseEntity.ok(ApiResponse.success(
                "Check-in bang QR Code thanh cong",
                response
        ));
    }

    private String resolveAuthenticatedEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Ban can dang nhap de thuc hien chuc nang nay");
        }
        return authentication.getName();
    }
}
