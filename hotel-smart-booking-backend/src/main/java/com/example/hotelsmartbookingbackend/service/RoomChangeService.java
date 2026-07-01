package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.RoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.response.RoomChangeResponse;

/**
 * Service interface định nghĩa contract cho chức năng "Yêu cầu chuyển phòng"
 * (Room Move / Room Change).
 *
 * <p><b>Luồng nghiệp vụ (Business Flow):</b>
 * <ol>
 *   <li>Validate booking tồn tại và đang ở trạng thái Checked-in.</li>
 *   <li>Validate phòng mới tồn tại và đang ở trạng thái Available.</li>
 *   <li>So sánh loại phòng (RoomType) cũ và mới:
 *       <ul>
 *         <li>Cùng loại → giữ nguyên giá, chuyển thẳng.</li>
 *         <li>Khác loại → tính toán lại giá phòng chênh lệch.</li>
 *       </ul>
 *   </li>
 *   <li>Cập nhật trạng thái phòng:
 *       <ul>
 *         <li>Phòng cũ: Available</li>
 *         <li>Phòng mới: Occupied</li>
 *       </ul>
 *   </li>
 *   <li>Cập nhật BookingDetail với phòng mới.</li>
 *   <li>Cập nhật BookingRoomAccess với phòng mới và mật khẩu mới.</li>
 * </ol>
 *
 * @author HotelSmartBooking Team
 */
public interface RoomChangeService {

    /**
     * Thực hiện chuyển phòng cho khách đang check-in.
     *
     * <p>Chỉ nhân viên có role {@code receptionist} hoặc {@code manager}
     * mới được thực hiện chức năng này.
     *
     * @param request      thông tin yêu cầu chuyển phòng (bookingId, newRoomId, reason)
     * @param staffEmail   email của nhân viên thực hiện thao tác (lấy từ JWT token)
     * @return             thông tin chi tiết kết quả chuyển phòng
     *
     * @throws com.example.hotelsmartbookingbackend.exception.BookingNotFoundException
     *         nếu không tìm thấy booking với bookingId đã cung cấp
     * @throws com.example.hotelsmartbookingbackend.exception.RoomNotAvailableException
     *         nếu phòng mới không ở trạng thái Available
     * @throws RuntimeException
     *         nếu booking không đang ở trạng thái Checked-in,
     *         phòng mới không tồn tại, hoặc nhân viên không có quyền
     */
    RoomChangeResponse changeRoom(RoomChangeRequest request, String staffEmail);

    /**
     * Khách hàng gửi yêu cầu chuyển phòng.
     * Lưu thông tin vào bảng customerrequests với requesttype = "RoomChange".
     */
    com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse submitRoomChangeRequest(
            com.example.hotelsmartbookingbackend.dto.request.CustomerRoomChangeRequest request, String customerEmail);

    /**
     * Nhân viên duyệt yêu cầu chuyển phòng của khách.
     * Sẽ gọi logic chuyển phòng trực tiếp và cập nhật trạng thái yêu cầu thành Approved.
     */
    com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse approveRoomChangeRequest(
            Integer requestId, Integer newRoomId, String staffEmail);

    /**
     * Nhân viên từ chối yêu cầu chuyển phòng của khách.
     * Cập nhật trạng thái yêu cầu thành Rejected và lưu lý do từ chối.
     */
    com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse rejectRoomChangeRequest(
            Integer requestId, String rejectionReason, String staffEmail);

    /**
     * Lấy danh sách các yêu cầu chuyển phòng đang ở trạng thái Pending.
     */
    java.util.List<com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse> getPendingRoomChangeRequests(
            String staffEmail);

    /**
     * Khách hàng gửi yêu cầu gia hạn lưu trú.
     */
    com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse submitStayExtensionRequest(
            com.example.hotelsmartbookingbackend.dto.request.CustomerStayExtensionRequest request, String customerEmail);

    /**
     * Nhân viên phê duyệt yêu cầu gia hạn lưu trú của khách.
     */
    com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse approveStayExtensionRequest(
            Integer requestId, String staffEmail);

    /**
     * Nhân viên từ chối yêu cầu gia hạn lưu trú của khách.
     */
    com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse rejectStayExtensionRequest(
            Integer requestId, String rejectionReason, String staffEmail);

    /**
     * Lấy danh sách các yêu cầu gia hạn lưu trú đang ở trạng thái Pending.
     */
    java.util.List<com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse> getPendingStayExtensionRequests(
            String staffEmail);

    /**
     * Khách hàng gửi yêu cầu check-out sớm.
     */
    com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse submitEarlyCheckOutRequest(
            com.example.hotelsmartbookingbackend.dto.request.CustomerEarlyCheckOutRequest request, String customerEmail);

    /**
     * Nhân viên phê duyệt yêu cầu check-out sớm.
     */
    com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse approveEarlyCheckOutRequest(
            Integer requestId, String staffEmail);

    /**
     * Nhân viên từ chối yêu cầu check-out sớm.
     */
    com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse rejectEarlyCheckOutRequest(
            Integer requestId, String rejectionReason, String staffEmail);

    /**
     * Lấy danh sách yêu cầu check-out sớm đang chờ duyệt.
     */
    java.util.List<com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse> getPendingEarlyCheckOutRequests(
            String staffEmail);
}
