package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.BookingFilter;
import com.example.hotelsmartbookingbackend.dto.request.UpdateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CancelBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceReadinessResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.service.BookingService;
import com.example.hotelsmartbookingbackend.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PdfService pdfService;

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
        String actorEmail = resolveCustomerEmail(authentication);
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
        String actorEmail = resolveCustomerEmail(authentication);
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
    public ResponseEntity<ApiResponse<BookingResponse>> checkOut(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String staffEmail = resolveCustomerEmail(authentication);
        BookingResponse response = bookingService.performCheckOut(bookingId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Trả phòng thành công", response));
    }



    @GetMapping("/{bookingId}/statement/pdf")
    public ResponseEntity<byte[]> getStatementPdf(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String actorEmail = resolveCustomerEmail(authentication);
        byte[] pdfBytes = pdfService.generateInvoicePdf(bookingId, actorEmail);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "statement-" + bookingId + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }


    @PostMapping("/receptionist/filter")
    @Operation(
            summary = "Filter bookings (Receptionist)",
            description = "Lọc các đơn đặt phòng theo tiêu chí khác nhau cho nhân viên lễ tân"
    )
    public ResponseEntity<ApiResponse<PageResponse<BookingHistoryResponse>>> filterBookings(
            @Valid @RequestBody BookingFilter criteria,
            Authentication authentication) {
        resolveCustomerEmail(authentication);
        PageResponse<BookingHistoryResponse> response = bookingService.filterBookings(criteria);
        return ResponseEntity.ok(ApiResponse.success("Lọc đơn đặt phòng thành công", response));
    }

    @PutMapping("/receptionist/{bookingId}")
    @Operation(
            summary = "Update booking (Receptionist)",
            description = "Cập nhật thông tin đơn đặt phòng (yêu cầu đặc biệt, chiết khấu, ghi chú)"
    )
    public ResponseEntity<ApiResponse<BookingResponse>> updateBooking(
            @PathVariable Integer bookingId,
            @Valid @RequestBody UpdateBookingRequest request,
            Authentication authentication) {
        String staffEmail = resolveCustomerEmail(authentication);
        BookingResponse response = bookingService.updateBooking(bookingId, request, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật đơn đặt phòng thành công", response));
    }

    @DeleteMapping("/receptionist/{bookingId}/cancel")
    @Operation(
            summary = "Cancel booking (Receptionist)",
            description = "Hủy đơn đặt phòng với lý do hủy"
    )
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @PathVariable Integer bookingId,
            @Valid @RequestBody CancelBookingRequest request,
            Authentication authentication) {
        String staffEmail = resolveCustomerEmail(authentication);
        BookingResponse response = bookingService.cancelBooking(bookingId, request, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn đặt phòng thành công", response));
    }

    private String resolveCustomerEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("Bạn cần đăng nhập để thực hiện chức năng này");
        }
        return authentication.getName();
    }
}
