package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.RoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.request.CustomerRoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.request.CustomerStayExtensionRequest;
import com.example.hotelsmartbookingbackend.dto.request.CustomerEarlyCheckOutRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomChangeResponse;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.service.RoomChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stay-adjustments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stay Adjustment", description = "API quản lý các thay đổi điều chỉnh lưu trú (Chuyển phòng, Gia hạn, Check-out sớm)")
public class StayAdjustmentController {

    private final RoomChangeService roomChangeService;

    @PostMapping("/room-change")
    @Operation(
            summary = "Chuyển phòng trực tiếp (Room Move)",
            description = "Lễ tân hoặc quản lý thực hiện chuyển phòng trực tiếp cho khách đang check-in."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Chuyển phòng thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy booking hoặc phòng")
    })
    public ResponseEntity<ApiResponse<RoomChangeResponse>> changeRoom(
            @Valid @RequestBody RoomChangeRequest request,
            Authentication authentication) {

        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' POST /room-change — bookingId={}", staffEmail, request.getBookingId());

        RoomChangeResponse response = roomChangeService.changeRoom(request, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Chuyển phòng thành công", response));
    }

    @PostMapping("/room-change/request")
    @Operation(summary = "Khách hàng gửi yêu cầu đổi phòng", description = "Khách hàng đang Checked-in gửi yêu cầu đổi sang phòng mới.")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> submitCustomerRoomChangeRequest(
            @Valid @RequestBody CustomerRoomChangeRequest request,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        log.info("[StayAdjustmentController] Guest='{}' yêu cầu đổi phòng. BookingId={}", customerEmail, request.getBookingId());

        CustomerRequestResponse response = roomChangeService.submitRoomChangeRequest(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu đổi phòng thành công. Vui lòng chờ duyệt.", response));
    }

    @GetMapping("/room-change/pending")
    @Operation(summary = "Lấy danh sách yêu cầu chuyển phòng đang chờ duyệt", description = "Chỉ dành cho lễ tân hoặc quản lý.")
    public ResponseEntity<ApiResponse<List<CustomerRequestResponse>>> getPendingRoomChangeRequests(
            Authentication authentication) {
        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' lấy danh sách yêu cầu chuyển phòng đang chờ duyệt", staffEmail);

        List<CustomerRequestResponse> response = roomChangeService.getPendingRoomChangeRequests(staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Tải danh sách yêu cầu thành công", response));
    }

    @PostMapping("/room-change/approve/{requestId}")
    @Operation(summary = "Duyệt yêu cầu chuyển phòng của khách", description = "Thực hiện chuyển phòng thực tế và đổi trạng thái yêu cầu sang Approved.")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> approveRoomChangeRequest(
            @PathVariable Integer requestId,
            @RequestParam Integer newRoomId,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' duyệt yêu cầu chuyển phòng ID={} (gán phòng ID={})", staffEmail, requestId, newRoomId);

        CustomerRequestResponse response = roomChangeService.approveRoomChangeRequest(requestId, newRoomId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt yêu cầu chuyển phòng thành công", response));
    }

    @PostMapping("/room-change/reject/{requestId}")
    @Operation(summary = "Từ chối yêu cầu chuyển phòng của khách", description = "Đổi trạng thái yêu cầu sang Rejected và lưu lý do.")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> rejectRoomChangeRequest(
            @PathVariable Integer requestId,
            @RequestBody(required = false) String rejectionReason,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' từ chối yêu cầu chuyển phòng ID={}. Lý do: {}", staffEmail, requestId, rejectionReason);

        String cleanReason = rejectionReason != null ? rejectionReason.replace("\"", "").trim() : "Từ chối bởi lễ tân";
        CustomerRequestResponse response = roomChangeService.rejectRoomChangeRequest(requestId, cleanReason, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối yêu cầu chuyển phòng", response));
    }


    @PostMapping("/extension/request")
    @Operation(summary = "Khách hàng gửi yêu cầu gia hạn lưu trú")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> submitStayExtensionRequest(
            @Valid @RequestBody CustomerStayExtensionRequest request,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        log.info("[StayAdjustmentController] Guest='{}' yêu cầu gia hạn lưu trú", customerEmail);

        CustomerRequestResponse response = roomChangeService.submitStayExtensionRequest(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu gia hạn lưu trú thành công", response));
    }

    @PostMapping("/extension/approve/{requestId}")
    @Operation(summary = "Nhân viên phê duyệt yêu cầu gia hạn lưu trú")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> approveStayExtensionRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' duyệt gia hạn ID={}", staffEmail, requestId);

        CustomerRequestResponse response = roomChangeService.approveStayExtensionRequest(requestId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt yêu cầu gia hạn lưu trú thành công", response));
    }

    @PostMapping("/extension/reject/{requestId}")
    @Operation(summary = "Nhân viên từ chối yêu cầu gia hạn lưu trú")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> rejectStayExtensionRequest(
            @PathVariable Integer requestId,
            @RequestParam String rejectionReason,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' từ chối gia hạn ID={}. Lý do: {}", staffEmail, requestId, rejectionReason);

        CustomerRequestResponse response = roomChangeService.rejectStayExtensionRequest(requestId, rejectionReason, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Từ chối yêu cầu gia hạn lưu trú thành công", response));
    }

    @GetMapping("/extension/pending")
    @Operation(summary = "Lấy danh sách yêu cầu gia hạn lưu trú đang chờ phê duyệt (Nhân viên)")
    public ResponseEntity<ApiResponse<List<CustomerRequestResponse>>> getPendingStayExtensionRequests(
            Authentication authentication) {
        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' lấy danh sách yêu cầu gia hạn lưu trú đang chờ phê duyệt", staffEmail);

        List<CustomerRequestResponse> response = roomChangeService.getPendingStayExtensionRequests(staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu gia hạn lưu trú thành công", response));
    }


    @PostMapping("/early-checkout/request")
    @Operation(summary = "Khách hàng gửi yêu cầu check-out sớm")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> submitEarlyCheckOutRequest(
            @Valid @RequestBody CustomerEarlyCheckOutRequest request,
            Authentication authentication) {
        String customerEmail = authentication.getName();
        log.info("[StayAdjustmentController] Guest='{}' gửi yêu cầu check-out sớm", customerEmail);

        CustomerRequestResponse response = roomChangeService.submitEarlyCheckOutRequest(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Gửi yêu cầu check-out sớm thành công", response));
    }

    @PostMapping("/early-checkout/approve/{requestId}")
    @Operation(summary = "Nhân viên phê duyệt yêu cầu check-out sớm")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> approveEarlyCheckOutRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' phê duyệt check-out sớm ID={}", staffEmail, requestId);

        CustomerRequestResponse response = roomChangeService.approveEarlyCheckOutRequest(requestId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt yêu cầu check-out sớm thành công", response));
    }

    @PostMapping("/early-checkout/reject/{requestId}")
    @Operation(summary = "Nhân viên từ chối yêu cầu check-out sớm")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> rejectEarlyCheckOutRequest(
            @PathVariable Integer requestId,
            @RequestParam String rejectionReason,
            Authentication authentication) {
        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' từ chối check-out sớm ID={}. Lý do: {}", staffEmail, requestId, rejectionReason);

        CustomerRequestResponse response = roomChangeService.rejectEarlyCheckOutRequest(requestId, rejectionReason, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Từ chối yêu cầu check-out sớm thành công", response));
    }

    @GetMapping("/early-checkout/pending")
    @Operation(summary = "Lấy danh sách yêu cầu check-out sớm đang chờ phê duyệt (Nhân viên)")
    public ResponseEntity<ApiResponse<List<CustomerRequestResponse>>> getPendingEarlyCheckOutRequests(
            Authentication authentication) {
        String staffEmail = authentication.getName();
        log.info("[StayAdjustmentController] Staff='{}' lấy danh sách yêu cầu check-out sớm đang chờ phê duyệt", staffEmail);

        List<CustomerRequestResponse> response = roomChangeService.getPendingEarlyCheckOutRequests(staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu check-out sớm thành công", response));
    }
}
