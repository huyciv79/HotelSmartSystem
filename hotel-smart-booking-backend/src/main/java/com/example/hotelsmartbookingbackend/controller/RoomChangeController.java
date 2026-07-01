package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.RoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomChangeResponse;
import com.example.hotelsmartbookingbackend.exception.UnauthorizedException;
import com.example.hotelsmartbookingbackend.service.RoomChangeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import com.example.hotelsmartbookingbackend.dto.request.CustomerRoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * REST Controller xử lý các yêu cầu liên quan đến chức năng
 * "Chuyển phòng" (Room Move / Room Change).
 *
 * <p><b>Base URL:</b> {@code /api/room-change}
 *
 * <p><b>Phân quyền:</b> Chỉ nhân viên có role {@code receptionist}
 * hoặc {@code manager} mới được gọi các endpoint trong controller này.
 * Việc kiểm tra chi tiết quyền được thực hiện ở tầng Service.
 *
 * @author HotelSmartBooking Team
 * @see RoomChangeService
 */
@RestController
@RequestMapping("/api/room-change")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room Change", description = "API quản lý chức năng chuyển phòng cho nhân viên lễ tân")
public class RoomChangeController {

    private final RoomChangeService roomChangeService;

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Thực hiện yêu cầu chuyển phòng cho khách đang check-in.
     *
     * <p><b>Request body:</b>
     * <pre>
     * {
     *   "bookingId": 123,
     *   "newRoomId": 45,
     *   "reason": "Khách yêu cầu phòng view biển"
     * }
     * </pre>
     *
     * <p><b>Business Rules:</b>
     * <ul>
     *   <li>Booking phải đang ở trạng thái {@code Checked-in}.</li>
     *   <li>Phòng mới phải đang ở trạng thái {@code Available}.</li>
     *   <li>Nếu phòng mới cùng loại → giữ nguyên giá.</li>
     *   <li>Nếu phòng mới khác loại → tính lại giá và ghi nhận chênh lệch.</li>
     * </ul>
     *
     * <p><b>HTTP Status Codes:</b>
     * <ul>
     *   <li>{@code 200 OK}          – Chuyển phòng thành công.</li>
     *   <li>{@code 400 Bad Request} – Dữ liệu đầu vào không hợp lệ.</li>
     *   <li>{@code 401 Unauthorized} – Chưa đăng nhập.</li>
     *   <li>{@code 404 Not Found}   – Booking hoặc phòng không tồn tại.</li>
     *   <li>{@code 409 Conflict}    – Phòng mới không ở trạng thái Available.</li>
     * </ul>
     *
     * @param request        thông tin yêu cầu chuyển phòng (validated bởi Bean Validation)
     * @param authentication thông tin xác thực JWT của nhân viên
     * @return {@link RoomChangeResponse} chứa đầy đủ thông tin kết quả chuyển phòng
     */
    @PostMapping
    @Operation(
            summary = "Chuyển phòng (Room Move)",
            description = """
                    Thực hiện chuyển phòng cho khách đang check-in.
                    
                    **Quy trình:**
                    1. Kiểm tra booking đang Checked-in
                    2. Kiểm tra phòng mới đang Available
                    3. So sánh loại phòng và tính chênh lệch giá (nếu có)
                    4. Cập nhật trạng thái phòng cũ → Available, phòng mới → Occupied
                    5. Cập nhật quyền truy cập phòng và mật khẩu mới
                    
                    **Phân quyền:** Chỉ Receptionist và Manager.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Chuyển phòng thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Dữ liệu đầu vào không hợp lệ hoặc vi phạm quy tắc nghiệp vụ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Chưa đăng nhập hoặc không có quyền"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Không tìm thấy booking hoặc phòng"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Phòng mới không ở trạng thái Available")
    })
    public ResponseEntity<ApiResponse<RoomChangeResponse>> changeRoom(
            @Valid @RequestBody RoomChangeRequest request,
            Authentication authentication) {

        // Lấy email nhân viên từ JWT token đã được xác thực
        String staffEmail = resolveStaffEmail(authentication);

        log.info("[RoomChangeController] Staff='{}' POST /api/room-change — bookingId={}",
                staffEmail, request.getBookingId());

        RoomChangeResponse response = roomChangeService.changeRoom(request, staffEmail);

        return ResponseEntity.ok(ApiResponse.success(
                "Chuyển phòng thành công", response));
    }

    /**
     * Khách hàng gửi yêu cầu đổi phòng.
     */
    @PostMapping("/customer/request")
    @Operation(summary = "Khách hàng gửi yêu cầu đổi phòng", description = "Khách hàng đang Checked-in gửi yêu cầu đổi sang phòng mới.")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> submitCustomerRequest(
            @Valid @RequestBody CustomerRoomChangeRequest request,
            Authentication authentication) {
        String customerEmail = resolveStaffEmail(authentication); // dùng chung helper lấy email
        log.info("[RoomChangeController] Guest='{}' yêu cầu đổi phòng. BookingId={}", customerEmail, request.getBookingId());

        CustomerRequestResponse response = roomChangeService.submitRoomChangeRequest(request, customerEmail);
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu đổi phòng thành công. Vui lòng chờ duyệt.", response));
    }

    /**
     * Nhân viên lấy danh sách các yêu cầu đổi phòng đang chờ duyệt.
     */
    @GetMapping("/pending")
    @Operation(summary = "Lấy danh sách yêu cầu chuyển phòng đang chờ duyệt", description = "Chỉ lễ tân hoặc quản lý.")
    public ResponseEntity<ApiResponse<List<CustomerRequestResponse>>> getPendingRequests(
            Authentication authentication) {
        String staffEmail = resolveStaffEmail(authentication);
        log.info("[RoomChangeController] Staff='{}' lấy danh sách yêu cầu chuyển phòng đang chờ duyệt", staffEmail);

        List<CustomerRequestResponse> response = roomChangeService.getPendingRoomChangeRequests(staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Tải danh sách yêu cầu thành công", response));
    }

    /**
     * Nhân viên duyệt yêu cầu đổi phòng.
     */
    @PostMapping("/approve/{requestId}")
    @Operation(summary = "Duyệt yêu cầu chuyển phòng của khách", description = "Thực hiện chuyển phòng thực tế và đổi trạng thái yêu cầu sang Approved.")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> approveRequest(
            @PathVariable Integer requestId,
            @RequestParam Integer newRoomId,
            Authentication authentication) {
        String staffEmail = resolveStaffEmail(authentication);
        log.info("[RoomChangeController] Staff='{}' duyệt yêu cầu chuyển phòng ID={} (gán phòng ID={})", staffEmail, newRoomId);

        CustomerRequestResponse response = roomChangeService.approveRoomChangeRequest(requestId, newRoomId, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Đã duyệt yêu cầu chuyển phòng thành công", response));
    }

    /**
     * Nhân viên từ chối yêu cầu đổi phòng.
     */
    @PostMapping("/reject/{requestId}")
    @Operation(summary = "Từ chối yêu cầu chuyển phòng của khách", description = "Đổi trạng thái yêu cầu sang Rejected và lưu lý do.")
    public ResponseEntity<ApiResponse<CustomerRequestResponse>> rejectRequest(
            @PathVariable Integer requestId,
            @RequestBody(required = false) String rejectionReason,
            Authentication authentication) {
        String staffEmail = resolveStaffEmail(authentication);
        log.info("[RoomChangeController] Staff='{}' từ chối yêu cầu chuyển phòng ID={}. Lý do: {}", staffEmail, requestId, rejectionReason);

        // Chuẩn hóa lý do từ chối (xử lý json raw string nếu gửi dưới dạng text)
        String cleanReason = rejectionReason != null ? rejectionReason.replace("\"", "").trim() : "Từ chối bởi lễ tân";
        
        CustomerRequestResponse response = roomChangeService.rejectRoomChangeRequest(requestId, cleanReason, staffEmail);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối yêu cầu chuyển phòng", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy email nhân viên từ đối tượng {@link Authentication}.
     *
     * <p>Nếu người dùng chưa đăng nhập hoặc là {@code anonymousUser},
     * ném ra {@link UnauthorizedException} để GlobalExceptionHandler trả về HTTP 401.
     *
     * @param authentication đối tượng xác thực từ Spring Security
     * @return email của nhân viên đang đăng nhập
     * @throws UnauthorizedException nếu chưa đăng nhập
     */
    private String resolveStaffEmail(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException(
                    "Bạn cần đăng nhập để thực hiện chức năng chuyển phòng");
        }
        return authentication.getName();
    }
}
