package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.CustomerEarlyCheckOutRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.service.RoomChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/early-checkout")
@RequiredArgsConstructor
@Tag(name = "Early Check-Out Controller", description = "Quản lý yêu cầu check-out sớm của Khách hàng")
public class EarlyCheckOutController {

    private final RoomChangeService roomChangeService;

    @PostMapping("/request")
    @Operation(summary = "Khách hàng gửi yêu cầu check-out sớm")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> submitRequest(
            @Valid @RequestBody CustomerEarlyCheckOutRequest request,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        CustomerRequestResponse response = roomChangeService.submitEarlyCheckOutRequest(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu check-out sớm thành công", response));
    }

    @PostMapping("/approve/{requestId}")
    @Operation(summary = "Nhân viên phê duyệt yêu cầu check-out sớm")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> approveRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        CustomerRequestResponse response = roomChangeService.approveEarlyCheckOutRequest(requestId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt yêu cầu check-out sớm thành công", response));
    }

    @PostMapping("/reject/{requestId}")
    @Operation(summary = "Nhân viên từ chối yêu cầu check-out sớm")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> rejectRequest(
            @PathVariable Integer requestId,
            @RequestParam String rejectionReason,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        CustomerRequestResponse response = roomChangeService.rejectEarlyCheckOutRequest(requestId, rejectionReason, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Từ chối yêu cầu check-out sớm thành công", response));
    }

    @GetMapping("/pending")
    @Operation(summary = "Lấy danh sách yêu cầu check-out sớm đang chờ phê duyệt (Nhân viên)")
    public ResponseEntity<ApiResponse<List<CustomerRequestResponse>>> getPendingRequests(
            Authentication authentication) {
        String staffEmail = authentication.getName();
        List<CustomerRequestResponse> response = roomChangeService.getPendingEarlyCheckOutRequests(staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu check-out sớm thành công", response));
    }
}
