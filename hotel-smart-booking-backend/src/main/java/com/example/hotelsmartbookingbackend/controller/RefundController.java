package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.ApproveRefundRequest;
import com.example.hotelsmartbookingbackend.dto.request.RefundRequest;
import com.example.hotelsmartbookingbackend.dto.request.RejectRefundRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.service.RefundService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping("/bookings/{bookingId}/refund-request")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> submitRefundRequest(
            @PathVariable Integer bookingId,
            @Valid @RequestBody RefundRequest request,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        CustomerRequestResponse response = refundService.submitRefundRequest(bookingId, request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu hoàn tiền thành công", response));
    }

    @PostMapping("/refund-requests/{requestId}/approve")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> approveRefundRequest(
            @PathVariable Integer requestId,
            @Valid @RequestBody(required = false) ApproveRefundRequest request,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        ApproveRefundRequest body = request != null ? request : new ApproveRefundRequest();
        CustomerRequestResponse response = refundService.approveRefundRequest(requestId, body, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt yêu cầu hoàn tiền thành công", response));
    }

    @PostMapping("/refund-requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> rejectRefundRequest(
            @PathVariable Integer requestId,
            @Valid @RequestBody RejectRefundRequest request,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        CustomerRequestResponse response = refundService.rejectRefundRequest(requestId, request, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Từ chối yêu cầu hoàn tiền thành công", response));
    }

    @GetMapping("/refund-requests/pending")
    public ResponseEntity<ApiResponse<List<CustomerRequestResponse>>> getPendingRefundRequests(Authentication authentication) {
        String staffEmail = authentication.getName();
        List<CustomerRequestResponse> response = refundService.getPendingRefundRequests(staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu hoàn tiền thành công", response));
    }
}
