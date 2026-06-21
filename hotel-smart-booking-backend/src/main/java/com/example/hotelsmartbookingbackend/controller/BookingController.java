package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication) {
        String customerEmail = resolveCustomerEmail(authentication);
        BookingResponse response = bookingService.createBooking(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Đặt phòng thành công", response));
    }

    @PostMapping("/group")
    public ResponseEntity<ApiResponse<BookingResponse>> createGroupBooking(
            @Valid @RequestBody CreateGroupBookingRequest request,
            Authentication authentication) {
        String customerEmail = resolveCustomerEmail(authentication);
        BookingResponse response = bookingService.createGroupBooking(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Đặt phòng nhóm thành công", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<BookingHistoryResponse>>> getBookingHistory(Authentication authentication) {
        String customerEmail = resolveCustomerEmail(authentication);
        List<BookingHistoryResponse> response = bookingService.getBookingHistory(customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đặt phòng thành công", response));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingDetail(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String customerEmail = resolveCustomerEmail(authentication);
        BookingResponse response = bookingService.getBookingDetail(bookingId, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đặt phòng thành công", response));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<BookingHistoryResponse>>> getAllBookings(Authentication authentication) {
        String staffEmail = resolveCustomerEmail(authentication);
        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff(staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy toàn bộ lịch sử đặt phòng thành công", response));
    }

    @PostMapping("/{bookingId}/check-in")
    public ResponseEntity<ApiResponse<BookingResponse>> checkIn(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String staffEmail = resolveCustomerEmail(authentication);
        BookingResponse response = bookingService.performCheckIn(bookingId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Nhận phòng thành công", response));
    }

    @PostMapping(
            value = "/{bookingId}/face-check-in",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Liveness detection và FaceID check-in",
            description = "Chỉ Manager được gọi. Camera tự lấy frame khi khách nhìn thẳng, quay trái, quay phải, nhìn lên và nhìn xuống; sau đó hệ thống kiểm tra active liveness, anti-spoofing và hồ sơ eKYC."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> faceCheckIn(
            @PathVariable Integer bookingId,
            @RequestPart("selfieImage") MultipartFile selfieImage,
            @RequestPart("leftImage") MultipartFile leftImage,
            @RequestPart("rightImage") MultipartFile rightImage,
            @RequestPart("upImage") MultipartFile upImage,
            @RequestPart("downImage") MultipartFile downImage,
            Authentication authentication) {
        String actorEmail = resolveCustomerEmail(authentication);
        BookingResponse response = bookingService.performFaceCheckIn(
                bookingId,
                selfieImage,
                leftImage,
                rightImage,
                upImage,
                downImage,
                actorEmail
        );
        return ResponseEntity.ok(ApiResponse.success(
                "Xác minh khuôn mặt và nhận phòng thành công",
                response
        ));
    }

    @PostMapping("/{bookingId}/check-out")
    public ResponseEntity<ApiResponse<BookingResponse>> checkOut(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String staffEmail = resolveCustomerEmail(authentication);
        BookingResponse response = bookingService.performCheckOut(bookingId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Trả phòng thành công", response));
    }

    private String resolveCustomerEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Bạn cần đăng nhập để thực hiện chức năng này");
        }
        return authentication.getName();
    }
}
