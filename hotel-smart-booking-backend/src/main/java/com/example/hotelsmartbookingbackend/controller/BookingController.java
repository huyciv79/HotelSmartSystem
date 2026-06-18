package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
