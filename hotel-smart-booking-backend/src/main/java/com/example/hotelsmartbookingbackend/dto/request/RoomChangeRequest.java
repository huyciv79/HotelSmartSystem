package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request DTO cho chức năng "Yêu cầu chuyển phòng" (Room Move).
 *
 * <p>Nhân viên lễ tân gửi request này để yêu cầu chuyển phòng cho khách
 * đang check-in. Hệ thống sẽ kiểm tra trạng thái phòng mới, so sánh loại
 * phòng, tính toán chênh lệch giá (nếu có), và cập nhật trạng thái tương ứng.
 *
 * @author HotelSmartBooking Team
 */
@Data
public class RoomChangeRequest {

    /**
     * ID của booking cần chuyển phòng.
     * Booking phải đang ở trạng thái "Checked-in".
     */
    @NotNull(message = "Booking ID không được để trống")
    private Integer bookingId;

    /**
     * ID của phòng mới muốn chuyển đến.
     * Phòng mới phải đang ở trạng thái "Available".
     */
    @NotNull(message = "ID phòng mới không được để trống")
    private Integer newRoomId;

    /**
     * Lý do chuyển phòng (tùy chọn).
     * Ví dụ: "Khách phàn nàn về tiếng ồn", "Yêu cầu view đẹp hơn", v.v.
     */
    private String reason;
}
