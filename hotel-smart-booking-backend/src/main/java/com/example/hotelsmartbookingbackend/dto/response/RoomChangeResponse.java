package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO sau khi xử lý thành công "Yêu cầu chuyển phòng" (Room Move).
 *
 * <p>Trả về đầy đủ thông tin về booking, phòng cũ, phòng mới,
 * và kết quả tính toán chênh lệch tiền phòng (nếu khác loại phòng).
 *
 * @author HotelSmartBooking Team
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomChangeResponse {

    // ── Thông tin booking ─────────────────────────────────────────────────────

    /** ID của booking vừa được xử lý chuyển phòng. */
    private Integer bookingId;

    /** Mã tham chiếu booking (ví dụ: BK20240630001). */
    private String bookingReference;

    /** Trạng thái hiện tại của booking sau khi chuyển. */
    private String bookingStatus;

    // ── Thông tin phòng cũ ────────────────────────────────────────────────────

    /** ID phòng cũ (trước khi chuyển). */
    private Integer oldRoomId;

    /** Số phòng cũ. */
    private String oldRoomNumber;

    /** Tên loại phòng cũ. */
    private String oldRoomTypeName;

    // ── Thông tin phòng mới ───────────────────────────────────────────────────

    /** ID phòng mới (sau khi chuyển). */
    private Integer newRoomId;

    /** Số phòng mới. */
    private String newRoomNumber;

    /** ID loại phòng mới. */
    private Integer newRoomTypeId;

    /** Tên loại phòng mới. */
    private String newRoomTypeName;

    /** Mã PIN truy cập phòng mới (nếu có). */
    private String newRoomPassword;

    // ── Kết quả tính toán tài chính ───────────────────────────────────────────

    /**
     * Loại chênh lệch phòng.
     * <ul>
     *   <li>{@code SAME_TYPE}    – Cùng loại phòng, không phát sinh thêm chi phí.</li>
     *   <li>{@code UPGRADE}      – Phòng mới đắt hơn, khách cần trả thêm.</li>
     *   <li>{@code DOWNGRADE}    – Phòng mới rẻ hơn, có thể hoàn tiền/ghi nhận credit.</li>
     * </ul>
     */
    private String changeType;

    /**
     * Số tiền chênh lệch (tuyệt đối, không âm).
     * <ul>
     *   <li>UPGRADE: số tiền khách cần trả thêm.</li>
     *   <li>DOWNGRADE: số tiền sẽ được ghi nhận credit/hoàn tiền.</li>
     *   <li>SAME_TYPE: luôn là 0.</li>
     * </ul>
     */
    private BigDecimal priceDifference;

    /** Tổng tiền phòng mới (sau khi tính lại với loại phòng mới). */
    private BigDecimal newTotalAmount;

    /** Số tiền final (sau thuế) mới. */
    private BigDecimal newFinalAmount;

    // ── Thông tin thời gian ───────────────────────────────────────────────────

    /** Thời điểm thực hiện chuyển phòng. */
    private Instant changedAt;

    /** Nhân viên thực hiện chuyển phòng. */
    private String changedByStaff;

    /** Lý do chuyển phòng. */
    private String reason;

    /**
     * Thông báo mô tả kết quả chuyển phòng cho nhân viên.
     * Ví dụ: "Chuyển phòng thành công. Phòng mới cùng loại, giá không đổi."
     */
    private String message;
}
