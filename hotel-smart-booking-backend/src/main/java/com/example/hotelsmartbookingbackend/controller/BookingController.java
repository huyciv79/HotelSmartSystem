package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.WalkInBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.AddServiceRequest;
import com.example.hotelsmartbookingbackend.dto.request.BookingFilter;
import com.example.hotelsmartbookingbackend.dto.request.UpdateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CancelBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.InvoiceResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomAvailabilityResponse;
import com.example.hotelsmartbookingbackend.service.BookingService;
import com.example.hotelsmartbookingbackend.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.hotelsmartbookingbackend.repository.ServiceRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PdfService pdfService;
    private final ServiceRepository serviceRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        BookingResponse response = bookingService.createBooking(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Đặt phòng thành công", response));
    }

    @PostMapping("/group")
    public ResponseEntity<ApiResponse<BookingResponse>> createGroupBooking(
            @Valid @RequestBody CreateGroupBookingRequest request,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        BookingResponse response = bookingService.createGroupBooking(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Đặt phòng nhóm thành công", response));
    }

    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<RoomAvailabilityResponse>> getRoomAvailability(
            @RequestParam Integer roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        RoomAvailabilityResponse response = bookingService.getRoomAvailability(
                roomTypeId,
                checkInDate,
                checkOutDate);
        return ResponseEntity.ok(ApiResponse.success("Room availability retrieved", response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<BookingHistoryResponse>>> getBookingHistory(Authentication authentication) {
        String customerEmail = authentication.getName();
        List<BookingHistoryResponse> response = bookingService.getBookingHistory(customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đặt phòng thành công", response));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingDetail(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        BookingResponse response = bookingService.getBookingDetail(bookingId, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đặt phòng thành công", response));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<BookingHistoryResponse>>> getAllBookings(Authentication authentication) {
        String staffEmail = authentication.getName();
        List<BookingHistoryResponse> response = bookingService.getAllBookingsForStaff(staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy toàn bộ lịch sử đặt phòng thành công", response));
    }

    @PostMapping("/walk-in")
    public ResponseEntity<ApiResponse<BookingResponse>> createWalkInBooking(
            @Valid @RequestBody WalkInBookingRequest request,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        BookingResponse response = bookingService.createWalkInBooking(request, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Đặt phòng trực tiếp (Walk-in) thành công", response));
    }

    @GetMapping("/{bookingId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        InvoiceResponse response = bookingService.getInvoiceDetails(bookingId, actorEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hóa đơn thành công", response));
    }

    @GetMapping("/{bookingId}/statement/pdf")
    public ResponseEntity<byte[]> getStatementPdf(
            @PathVariable Integer bookingId,
            Authentication authentication) {
        String actorEmail = authentication.getName();
        byte[] pdfBytes = pdfService.generateInvoicePdf(bookingId, actorEmail);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "statement-" + bookingId + ".pdf");
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }

    @PostMapping("/{bookingId}/services")
    public ResponseEntity<ApiResponse<String>> addService(
            @PathVariable Integer bookingId,
            @Valid @RequestBody AddServiceRequest request,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        bookingService.addServiceToBooking(bookingId, request, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Thêm dịch vụ vào đơn đặt phòng thành công", "SUCCESS"));
    }

    @PostMapping("/receptionist/filter")
    @Operation(
            summary = "Filter bookings (Receptionist)",
            description = "Lọc các đơn đặt phòng theo tiêu chí khác nhau cho nhân viên lễ tân"
    )
    public ResponseEntity<ApiResponse<PageResponse<BookingHistoryResponse>>> filterBookings(
            @Valid @RequestBody BookingFilter criteria,
            Authentication authentication) {
        authentication.getName();
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
        String staffEmail = authentication.getName();
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
        String staffEmail = authentication.getName();
        BookingResponse response = bookingService.cancelBooking(bookingId, request, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn đặt phòng thành công", response));
    }

    @DeleteMapping("/{bookingId}/cancel")
    @Operation(
            summary = "Khách hàng tự hủy đơn đặt phòng",
            description = "Cho phép khách hàng tự hủy đặt phòng của mình nếu chưa Check-in"
    )
    public ResponseEntity<ApiResponse<BookingResponse>> customerCancelBooking(
            @PathVariable Integer bookingId,
            @Valid @RequestBody(required = false) CancelBookingRequest request,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        CancelBookingRequest body = request != null ? request : new CancelBookingRequest();
        BookingResponse response = bookingService.customerCancelBooking(bookingId, body, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn đặt phòng thành công", response));
    }

    @GetMapping("/services/all")
    public ResponseEntity<ApiResponse<List<com.example.hotelsmartbookingbackend.entity.Service>>> getAllServices() {
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách dịch vụ thành công", serviceRepository.findAll()));
    }
}
