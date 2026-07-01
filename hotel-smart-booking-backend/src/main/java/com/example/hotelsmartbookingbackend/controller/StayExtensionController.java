package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.CustomerStayExtensionRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.service.RoomChangeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stay-extension")
@RequiredArgsConstructor
public class StayExtensionController {

    private final RoomChangeService roomChangeService;

    @PostMapping("/request")
    @Operation(summary = "Khách hàng gửi yêu cầu gia hạn lưu trú")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> submitRequest(
            @Valid @RequestBody CustomerStayExtensionRequest request,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        CustomerRequestResponse response = roomChangeService.submitStayExtensionRequest(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu gia hạn lưu trú thành công", response));
    }

    @PostMapping("/approve/{requestId}")
    @Operation(summary = "Nhân viên phê duyệt yêu cầu gia hạn lưu trú")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> approveRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        CustomerRequestResponse response = roomChangeService.approveStayExtensionRequest(requestId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt yêu cầu gia hạn lưu trú thành công", response));
    }

    @PostMapping("/reject/{requestId}")
    @Operation(summary = "Nhân viên từ chối yêu cầu gia hạn lưu trú")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> rejectRequest(
            @PathVariable Integer requestId,
            @RequestParam String rejectionReason,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        CustomerRequestResponse response = roomChangeService.rejectStayExtensionRequest(requestId, rejectionReason, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Từ chối yêu cầu gia hạn lưu trú thành công", response));
    }

    @GetMapping("/pending")
    @Operation(summary = "Lấy danh sách yêu cầu gia hạn lưu trú đang chờ phê duyệt (Nhân viên)")
    public ResponseEntity<ApiResponse<List<CustomerRequestResponse>>> getPendingRequests(
            Authentication authentication) {
        String staffEmail = authentication.getName();
        List<CustomerRequestResponse> response = roomChangeService.getPendingStayExtensionRequests(staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu gia hạn lưu trú thành công", response));
    }
}
