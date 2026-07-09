package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.QrCheckInRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceReadinessResponse;
import com.example.hotelsmartbookingbackend.dto.response.QrTokenResponse;
import com.example.hotelsmartbookingbackend.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
        String actorEmail = authentication.getName();
        BookingResponse response = bookingService.performQrCheckIn(request.getToken(), actorEmail);
        return ResponseEntity.ok(ApiResponse.success(
                "Check-in bang QR Code thanh cong",
                response
        ));
    }

    @PostMapping("/{bookingId}/manual")
    @Operation(
            summary = "Check-in thủ công tại quầy",
            description = "Lễ tân hoặc quản lý thực hiện check-in thủ công cho khách."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> checkIn(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        BookingResponse response = bookingService.performCheckIn(bookingId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Nhận phòng thành công", response));
    }

    @PostMapping("/{bookingId}/qr-token")
    @Operation(
            summary = "Tao QR token check-in",
            description = "Khach hang da Verified eKYC tao token QR ngan han cho booking QR Code chua check-in."
    )
    public ResponseEntity<ApiResponse<QrTokenResponse>> generateQrCheckInToken(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        QrTokenResponse response = bookingService.generateQrCheckInToken(bookingId, customerEmail);
        return ResponseEntity.ok(ApiResponse.success(
                "Tao ma QR check-in thành công",
                response
        ));
    }

    @PostMapping(
            value = "/{bookingId}/face",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Liveness detection và FaceID check-in",
            description = "Chỉ Manager được gọi. Camera lấy frame chính diện và một hướng quay ngẫu nhiên; hệ thống kiểm tra active liveness nhanh, anti-spoofing và face template nhiều góc đã đăng ký."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> faceCheckIn(
            @PathVariable Integer bookingId,
            @RequestPart("selfieImage") MultipartFile selfieImage,
            @RequestPart("challengeImage") MultipartFile challengeImage,
            @RequestPart("challengeImage2") MultipartFile challengeImage2,
            @RequestPart("challengeImage3") MultipartFile challengeImage3,
            @RequestPart("challengeDirection") String challengeDirection,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        BookingResponse response = bookingService.performFaceCheckIn(
                bookingId,
                selfieImage,
                challengeImage,
                challengeImage2,
                challengeImage3,
                challengeDirection,
                actorEmail
        );
        return ResponseEntity.ok(ApiResponse.success(
                "Xác minh khuôn mặt và nhận phòng thành công",
                response
        ));
    }

    @PostMapping(
            value = "/face-readiness",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Kiểm tra camera trước khi quét FaceID",
            description = "Chỉ Manager được gọi. Kiểm tra đúng một khuôn mặt, đủ gần và nhìn thẳng."
    )
    public ResponseEntity<ApiResponse<AiFaceReadinessResponse>> checkFaceReadiness(
            @RequestPart("selfieImage") MultipartFile selfieImage,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        AiFaceReadinessResponse response = bookingService.checkFaceReadiness(
                selfieImage,
                actorEmail
        );
        return ResponseEntity.ok(ApiResponse.success(
                "Kiểm tra camera FaceID thành công",
                response
        ));
    }

    @PostMapping("/{bookingId}/check-out")
    @Operation(
            summary = "Trả phòng (Check-out)",
            description = "Nhân viên thực hiện trả phòng cho khách."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> checkOut(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        BookingResponse response = bookingService.performCheckOut(bookingId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Trả phòng thành công", response));
    }
}
