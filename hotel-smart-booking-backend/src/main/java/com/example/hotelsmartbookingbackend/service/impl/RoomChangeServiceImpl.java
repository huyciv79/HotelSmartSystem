package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.RoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.response.RoomChangeResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.BookingRoomAccess;
import com.example.hotelsmartbookingbackend.entity.Bookingdetail;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.entity.Roomtype;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.BookingRoomAccessRepository;
import com.example.hotelsmartbookingbackend.repository.BookingdetailRepository;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.service.RoomChangeService;
import com.example.hotelsmartbookingbackend.service.WebSocketService;

import com.example.hotelsmartbookingbackend.dto.request.CustomerRoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.request.CustomerStayExtensionRequest;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.entity.Customerrequest;
import com.example.hotelsmartbookingbackend.repository.CustomerrequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Implementation của {@link RoomChangeService}.
 *
 * <p><b>Sơ đồ nghiệp vụ (Business Flow):</b>
 * <pre>
 *   Staff gọi API
 *       │
 *       ▼
 *   [Bước 1] Xác thực quyền nhân viên (Receptionist / Manager)
 *       │
 *       ▼
 *   [Bước 2] Kiểm tra Booking tồn tại & đang "Checked-in"
 *              + Kiểm tra phòng hiện tại khớp với booking
 *       │
 *       ▼
 *   [Bước 3] Kiểm tra phòng mới: tồn tại & trạng thái "Available"
 *       │
 *       ▼
 *   [Bước 4] So sánh RoomType cũ và mới
 *       ├── Cùng RoomType  →  Giữ nguyên giá, tạo PIN mới cho phòng mới
 *       └── Khác RoomType  →  Tính lại tổng tiền theo baseprice phòng mới
 *                              + Cập nhật roomtypeid trong BookingDetail
 *       │
 *       ▼
 *   [Bước 5] Cập nhật trạng thái phòng:
 *              Phòng cũ → "Available"
 *              Phòng mới → "Occupied"
 *       │
 *       ▼
 *   [Bước 6] Cập nhật BookingDetail + BookingRoomAccess
 *       │
 *       ▼
 *   [Bước 7] Broadcast WebSocket + Build response
 * </pre>
 *
 * @author HotelSmartBooking Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomChangeServiceImpl implements RoomChangeService {

    // ── Hằng số trạng thái ───────────────────────────────────────────────────

    /** Trạng thái phòng đang trống, sẵn sàng nhận khách. */
    private static final String ROOM_STATUS_AVAILABLE = "Available";

    /** Trạng thái phòng đang có khách. */
    private static final String ROOM_STATUS_OCCUPIED = "Occupied";

    /** Trạng thái booking đang check-in. */
    private static final String BOOKING_STATUS_CHECKED_IN = "Checked-in";

    /** Trạng thái room key đang hoạt động. */
    private static final String ROOM_KEY_STATUS_ACTIVE = "Active";

    /** Trạng thái room key đã hết hạn (áp dụng cho phòng cũ sau khi chuyển). */
    private static final String ROOM_KEY_STATUS_EXPIRED = "Expired";

    /** Thuế VAT áp dụng (10%). */
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");

    // ── Dependencies ─────────────────────────────────────────────────────────

    private final BookingRepository bookingRepository;
    private final BookingdetailRepository bookingdetailRepository;
    private final BookingRoomAccessRepository bookingRoomAccessRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final CustomerrequestRepository customerrequestRepository;

    // ═════════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     *
     * <p>Toàn bộ quy trình chuyển phòng được bọc trong một transaction
     * để đảm bảo tính toàn vẹn dữ liệu (ACID).
     */
    @Override
    @Transactional
    public RoomChangeResponse changeRoom(RoomChangeRequest request, String staffEmail) {
        log.info("[RoomChange] Staff='{}' yêu cầu chuyển phòng: bookingId={}, newRoomId={}",
                staffEmail, request.getBookingId(), request.getNewRoomId());

        // ── Bước 1: Xác thực quyền nhân viên ─────────────────────────────────
        User staff = validateStaffPermission(staffEmail);

        // ── Bước 2: Kiểm tra Booking & trạng thái ────────────────────────────
        Booking booking = validateBookingIsCheckedIn(request.getBookingId());
        Bookingdetail detail = fetchBookingDetail(booking.getId());

        // Phòng hiện tại của khách (phòng cũ)
        Room currentRoom = detail.getRoomid();
        validateCurrentRoomAssigned(currentRoom, booking.getId());

        // ── Bước 3: Kiểm tra phòng mới hợp lệ & đang trống ──────────────────
        Room newRoom = fetchAndValidateNewRoom(request.getNewRoomId(), currentRoom);

        // ── Bước 4: So sánh RoomType & tính toán tài chính ───────────────────
        Roomtype oldRoomType = currentRoom.getRoomtypeid();
        Roomtype newRoomType = newRoom.getRoomtypeid();

        boolean isSameRoomType = oldRoomType.getId().equals(newRoomType.getId());
        FinancialResult financialResult = computeFinancialResult(
                isSameRoomType, booking, detail, newRoomType);

        // ── Bước 5 & 6: Thực hiện chuyển phòng ───────────────────────────────
        Instant now = Instant.now();
        String newRoomPassword = performRoomSwap(
                booking, detail, currentRoom, newRoom, financialResult, staff, now);

        // ── Bước 7: Build và trả về response ─────────────────────────────────
        RoomChangeResponse response = buildResponse(
                booking, detail, currentRoom, newRoom,
                oldRoomType, newRoomType,
                financialResult, isSameRoomType,
                newRoomPassword, staff, now, request.getReason());

        log.info("[RoomChange] Hoàn thành chuyển phòng: bookingId={}, phòng {} → {}, loại={}",
                booking.getId(), currentRoom.getRoomnumber(), newRoom.getRoomnumber(),
                isSameRoomType ? "SAME_TYPE" : "DIFFERENT_TYPE");

        return response;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  VALIDATION METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Xác thực nhân viên tồn tại và có quyền thực hiện chuyển phòng.
     *
     * @param staffEmail email nhân viên từ JWT token
     * @return đối tượng User của nhân viên
     * @throws RuntimeException nếu không tìm thấy hoặc không có quyền
     */
    private User validateStaffPermission(String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy tài khoản nhân viên: " + staffEmail));

        String role = staff.getRole() != null ? staff.getRole().name() : "";
        boolean isAuthorized = "receptionist".equalsIgnoreCase(role)
                || "manager".equalsIgnoreCase(role);

        if (!isAuthorized) {
            throw new RuntimeException(
                    "Bạn không có quyền thực hiện chức năng chuyển phòng. " +
                    "Chỉ nhân viên lễ tân (Receptionist) hoặc Quản lý (Manager) mới được thực hiện.");
        }

        return staff;
    }

    /**
     * Kiểm tra booking tồn tại và đang ở trạng thái "Checked-in".
     *
     * <p><b>Điều kiện hợp lệ:</b> Booking phải đang trong trạng thái
     * {@code "Checked-in"} (hoặc {@code "Checked In"} để tương thích).
     * Chỉ khách đang ở trong phòng mới được chuyển phòng.
     *
     * @param bookingId ID của booking cần kiểm tra
     * @return đối tượng Booking hợp lệ
     * @throws RuntimeException         nếu booking không ở trạng thái Checked-in
     */
    private Booking validateBookingIsCheckedIn(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + bookingId));

        String status = booking.getStatus();
        boolean isCheckedIn = BOOKING_STATUS_CHECKED_IN.equalsIgnoreCase(status)
                || "Checked In".equalsIgnoreCase(status)
                || "Checked-In".equalsIgnoreCase(status);

        if (!isCheckedIn) {
            throw new RuntimeException(String.format(
                    "Không thể chuyển phòng. Đơn đặt phòng #%d đang ở trạng thái '%s'. " +
                    "Chỉ có thể chuyển phòng khi khách đã check-in.",
                    bookingId, status));
        }

        return booking;
    }

    /**
     * Lấy Bookingdetail của booking.
     *
     * @param bookingId ID của booking
     * @return đối tượng Bookingdetail
     * @throws RuntimeException nếu không tìm thấy chi tiết booking
     */
    private Bookingdetail fetchBookingDetail(Integer bookingId) {
        return bookingdetailRepository.findByBookingid_Id(bookingId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy chi tiết đơn đặt phòng với ID: " + bookingId));
    }

    /**
     * Kiểm tra phòng hiện tại đã được gán cho booking.
     *
     * <p>Nếu {@code currentRoom} là {@code null}, nghĩa là booking chưa được
     * gán phòng cụ thể — điều này không nên xảy ra khi booking đã Checked-in.
     *
     * @param currentRoom phòng hiện tại từ BookingDetail
     * @param bookingId   ID booking để log thông báo lỗi rõ ràng
     * @throws RuntimeException nếu phòng hiện tại chưa được gán
     */
    private void validateCurrentRoomAssigned(Room currentRoom, Integer bookingId) {
        if (currentRoom == null) {
            throw new RuntimeException(String.format(
                    "Đơn đặt phòng #%d chưa được gán phòng cụ thể. Không thể thực hiện chuyển phòng.",
                    bookingId));
        }
    }

    /**
     * Lấy phòng mới và kiểm tra các điều kiện hợp lệ.
     *
     * <p><b>Các kiểm tra được thực hiện:</b>
     * <ol>
     *   <li>Phòng mới phải tồn tại trong hệ thống.</li>
     *   <li>Phòng mới không được trùng với phòng hiện tại.</li>
     *   <li>Phòng mới phải đang ở trạng thái {@code "Available"}.</li>
     * </ol>
     *
     * @param newRoomId   ID của phòng mới
     * @param currentRoom phòng hiện tại (để kiểm tra trùng lặp)
     * @return đối tượng Room của phòng mới
     * @throws RuntimeException          nếu phòng không tồn tại hoặc trùng với phòng hiện tại
     */
    private Room fetchAndValidateNewRoom(Integer newRoomId, Room currentRoom) {
        // Phòng mới phải tồn tại
        Room newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy phòng với ID: " + newRoomId));

        // Không được chuyển sang chính phòng hiện tại
        if (currentRoom.getId().equals(newRoom.getId())) {
            throw new RuntimeException(
                    "Phòng mới không được trùng với phòng hiện tại (Phòng " +
                    currentRoom.getRoomnumber() + ").");
        }

        // Phòng mới phải đang trống
        String newRoomStatus = newRoom.getStatus();
        if (!ROOM_STATUS_AVAILABLE.equalsIgnoreCase(newRoomStatus)) {
            throw new RuntimeException(String.format(
                    "Phòng '%s' hiện không trống (trạng thái: %s). Vui lòng chọn phòng khác.",
                    newRoom.getRoomnumber(), newRoomStatus));
        }

        return newRoom;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  FINANCIAL CALCULATION
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Tính toán kết quả tài chính dựa trên việc so sánh loại phòng.
     *
     * <p><b>Quy tắc:</b>
     * <ul>
     *   <li>Cùng loại phòng: giữ nguyên {@code totalAmount}, {@code finalAmount}.
     *       Không phát sinh chênh lệch.</li>
     *   <li>Khác loại phòng: tính lại dựa trên baseprice của loại phòng mới,
     *       số đêm thực tế còn lại và số lượng phòng trong booking.</li>
     * </ul>
     *
     * <p><b>Công thức tính lại tiền khi khác loại phòng:</b>
     * <pre>
     *   remainingNights = checkoutDate - today  (tính từ hôm nay, không phải từ check-in)
     *   newRoomTotal    = newBaseprice × quantity × remainingNights
     *   newTaxAmount    = newRoomTotal × 10%
     *   newFinalAmount  = newRoomTotal + newTaxAmount
     *
     *   priceDifference = newFinalAmount - currentFinalAmount
     *   changeType      = priceDifference > 0 ? "UPGRADE" : "DOWNGRADE"
     * </pre>
     *
     * @param isSameRoomType  {@code true} nếu cùng loại phòng
     * @param booking         thông tin booking hiện tại
     * @param detail          chi tiết booking (số đêm, số lượng phòng)
     * @param newRoomType     loại phòng mới
     * @return {@link FinancialResult} chứa thông tin tài chính sau chuyển phòng
     */
    private FinancialResult computeFinancialResult(
            boolean isSameRoomType,
            Booking booking,
            Bookingdetail detail,
            Roomtype newRoomType) {

        if (isSameRoomType) {
            // Cùng loại phòng: giữ nguyên giá, không phát sinh chênh lệch
            return FinancialResult.sameType(booking.getTotalamount(), booking.getFinalamount());
        }

        // Khác loại phòng: tính lại tiền từ ngày hôm nay đến ngày checkout
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        LocalDate checkoutDate = detail.getExpectedcheckout()
                .atZone(HOTEL_ZONE).toLocalDate();

        // Số đêm còn lại (tính từ hôm nay để phản ánh thực tế)
        long remainingNights = ChronoUnit.DAYS.between(today, checkoutDate);
        if (remainingNights <= 0) {
            // Trường hợp biên: nếu đã đến ngày checkout, vẫn tính ít nhất 1 đêm
            remainingNights = 1;
        }

        int quantity = detail.getQuantity() != null ? detail.getQuantity() : 1;
        BigDecimal newBaseprice = newRoomType.getBaseprice();

        // Tính lại tiền phòng cho số đêm còn lại với loại phòng mới
        BigDecimal newTotalAmount = newBaseprice
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(remainingNights))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal newTaxAmount = newTotalAmount
                .multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal newFinalAmount = newTotalAmount
                .add(newTaxAmount)
                .setScale(2, RoundingMode.HALF_UP);

        // Tính chênh lệch so với giá hiện tại
        BigDecimal currentFinalAmount = booking.getFinalamount();
        BigDecimal difference = newFinalAmount.subtract(currentFinalAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return FinancialResult.differentType(newTotalAmount, newFinalAmount, difference);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CORE ROOM SWAP LOGIC
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Thực hiện toàn bộ thao tác chuyển phòng và cập nhật dữ liệu.
     *
     * <p><b>Các thao tác trong method này:</b>
     * <ol>
     *   <li>Đổi trạng thái phòng cũ → {@code Available}.</li>
     *   <li>Đổi trạng thái phòng mới → {@code Occupied}.</li>
     *   <li>Cập nhật {@link BookingRoomAccess}: thu hồi access phòng cũ, cấp access phòng mới.</li>
     *   <li>Cập nhật {@link Bookingdetail}: roomid, roomtypeid (nếu khác loại), giá mới (nếu có).</li>
     *   <li>Cập nhật {@link Booking}: totalAmount, finalAmount (nếu khác loại phòng).</li>
     *   <li>Broadcast WebSocket để cập nhật trạng thái phòng real-time.</li>
     * </ol>
     *
     * @param booking         booking cần cập nhật
     * @param detail          chi tiết booking cần cập nhật
     * @param currentRoom     phòng cũ
     * @param newRoom         phòng mới
     * @param financialResult kết quả tính toán tài chính
     * @param staff           nhân viên thực hiện
     * @param now             thời điểm hiện tại
     * @return mật khẩu phòng mới được tạo ra
     */
    private String performRoomSwap(
            Booking booking,
            Bookingdetail detail,
            Room currentRoom,
            Room newRoom,
            FinancialResult financialResult,
            User staff,
            Instant now) {

        // ── 5a. Cập nhật trạng thái phòng cũ → Available ─────────────────────
        log.debug("[RoomChange] Giải phóng phòng cũ: {} → Available", currentRoom.getRoomnumber());
        currentRoom.setStatus(ROOM_STATUS_AVAILABLE);
        currentRoom.setUpdatedat(now);
        roomRepository.save(currentRoom);

        // Broadcast trạng thái phòng cũ qua WebSocket
        webSocketService.broadcastRoomStatus(
                currentRoom.getId(), currentRoom.getRoomnumber(), ROOM_STATUS_AVAILABLE);

        // ── 5b. Cập nhật trạng thái phòng mới → Occupied ─────────────────────
        log.debug("[RoomChange] Chiếm phòng mới: {} → Occupied", newRoom.getRoomnumber());
        newRoom.setStatus(ROOM_STATUS_OCCUPIED);
        newRoom.setUpdatedat(now);
        roomRepository.save(newRoom);

        // Broadcast trạng thái phòng mới qua WebSocket
        webSocketService.broadcastRoomStatus(
                newRoom.getId(), newRoom.getRoomnumber(), ROOM_STATUS_OCCUPIED);

        // ── 6a. Cập nhật BookingRoomAccess ────────────────────────────────────
        String newPassword = updateBookingRoomAccesses(booking, currentRoom, newRoom, now);

        // ── 6b. Cập nhật BookingDetail ────────────────────────────────────────
        updateBookingDetail(detail, newRoom, financialResult, now);

        // ── 6c. Cập nhật Booking (tài chính nếu khác loại phòng) ──────────────
        if (financialResult.isDifferentType()) {
            updateBookingFinancials(booking, financialResult, now);
        } else {
            // Cùng loại phòng: chỉ cập nhật timestamp
            booking.setUpdatedat(now);
            bookingRepository.save(booking);
        }

        return newPassword;
    }

    /**
     * Cập nhật bảng {@code BookingRoomAccess}:
     * <ul>
     *   <li>Thu hồi access của phòng cũ (đặt status = Expired, xóa mật khẩu).</li>
     *   <li>Cập nhật access sang phòng mới với mật khẩu mới.</li>
     * </ul>
     *
     * <p><b>Lưu ý:</b> Hệ thống giữ nguyên thời hạn key ({@code roomkeyexpiredat})
     * từ booking gốc để đảm bảo khách không bị hết hạn access sớm hơn dự kiến.
     *
     * @param booking     booking cần cập nhật access
     * @param currentRoom phòng cũ cần thu hồi access
     * @param newRoom     phòng mới cần cấp access
     * @param now         thời điểm hiện tại
     * @return mật khẩu mới được sinh cho phòng mới
     */
    private String updateBookingRoomAccesses(
            Booking booking,
            Room currentRoom,
            Room newRoom,
            Instant now) {

        List<BookingRoomAccess> accesses = bookingRoomAccessRepository
                .findByBookingid_IdOrderByRoomid_RoomnumberAsc(booking.getId());

        String newPassword = generateRoomPassword();
        boolean foundOldRoom = false;

        for (BookingRoomAccess access : accesses) {
            if (access.getRoomid().getId().equals(currentRoom.getId())) {
                // Thu hồi access phòng cũ và tái sử dụng record này cho phòng mới
                // (tối ưu: tránh tạo thêm record mới không cần thiết)
                foundOldRoom = true;
                Instant originalExpiry = access.getRoomkeyexpiredat(); // Giữ nguyên thời hạn

                access.setRoomid(newRoom);
                access.setRoomkeyaccess(newPassword);
                access.setRoomkeygeneratedat(now);
                access.setRoomkeyexpiredat(originalExpiry); // Kế thừa thời hạn cũ
                access.setRoomkeystatus(ROOM_KEY_STATUS_ACTIVE);
                access.setUpdatedat(now);

                log.debug("[RoomChange] Cập nhật BookingRoomAccess: phòng {} → {}, key mới được tạo",
                        currentRoom.getRoomnumber(), newRoom.getRoomnumber());
            }
        }

        // Trường hợp không tìm thấy access record của phòng cũ
        // (không nên xảy ra, nhưng xử lý phòng ngừa)
        if (!foundOldRoom && !accesses.isEmpty()) {
            log.warn("[RoomChange] Không tìm thấy BookingRoomAccess cho phòng cũ '{}'. " +
                    "Cập nhật record đầu tiên.", currentRoom.getRoomnumber());
            BookingRoomAccess firstAccess = accesses.get(0);
            Instant originalExpiry = firstAccess.getRoomkeyexpiredat();

            firstAccess.setRoomid(newRoom);
            firstAccess.setRoomkeyaccess(newPassword);
            firstAccess.setRoomkeygeneratedat(now);
            firstAccess.setRoomkeyexpiredat(originalExpiry);
            firstAccess.setRoomkeystatus(ROOM_KEY_STATUS_ACTIVE);
            firstAccess.setUpdatedat(now);
        }

        bookingRoomAccessRepository.saveAll(accesses);
        return newPassword;
    }

    /**
     * Cập nhật {@link Bookingdetail} với thông tin phòng mới.
     *
     * <p>Nếu chuyển khác loại phòng ({@code isDifferentType}), sẽ đồng thời
     * cập nhật {@code roomtypeid} và {@code priceatbooking} sang giá mới.
     *
     * @param detail          chi tiết booking cần cập nhật
     * @param newRoom         phòng mới
     * @param financialResult kết quả tài chính
     * @param now             thời điểm hiện tại
     */
    private void updateBookingDetail(
            Bookingdetail detail,
            Room newRoom,
            FinancialResult financialResult,
            Instant now) {

        // Gán phòng mới
        detail.setRoomid(newRoom);

        if (financialResult.isDifferentType()) {
            // Khác loại phòng: cập nhật roomtype và giá mới
            detail.setRoomtypeid(newRoom.getRoomtypeid());
            detail.setPriceatbooking(newRoom.getRoomtypeid().getBaseprice());
            log.debug("[RoomChange] Cập nhật BookingDetail: loại phòng mới='{}', giá mới={}",
                    newRoom.getRoomtypeid().getName(), newRoom.getRoomtypeid().getBaseprice());
        }

        // Cập nhật thông tin key phòng mới trong BookingDetail (sync với BookingRoomAccess)
        detail.setRoomkeyaccess(null);       // BookingRoomAccess là nguồn chính xác cho key
        detail.setRoomkeystatus(ROOM_KEY_STATUS_ACTIVE);
        detail.setUpdatedat(now);

        bookingdetailRepository.save(detail);
    }

    /**
     * Cập nhật tài chính của {@link Booking} khi chuyển sang loại phòng khác.
     *
     * <p>Phương thức này chỉ được gọi khi phòng mới có loại phòng khác với phòng cũ.
     *
     * @param booking         booking cần cập nhật
     * @param financialResult kết quả tài chính mới
     * @param now             thời điểm hiện tại
     */
    private void updateBookingFinancials(Booking booking, FinancialResult financialResult, Instant now) {
        log.debug("[RoomChange] Cập nhật tài chính Booking: totalAmount={}, finalAmount={}",
                financialResult.getNewTotalAmount(), financialResult.getNewFinalAmount());

        booking.setTotalamount(financialResult.getNewTotalAmount());

        // Tính lại tax dựa trên total mới
        BigDecimal newTaxAmount = financialResult.getNewTotalAmount()
                .multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        booking.setTaxamount(newTaxAmount);
        booking.setFinalamount(financialResult.getNewFinalAmount());
        booking.setUpdatedat(now);

        bookingRepository.save(booking);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RESPONSE BUILDER
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Xây dựng đối tượng {@link RoomChangeResponse} để trả về cho client.
     *
     * @param booking        booking đã được cập nhật
     * @param detail         chi tiết booking
     * @param oldRoom        phòng cũ
     * @param newRoom        phòng mới
     * @param oldRoomType    loại phòng cũ
     * @param newRoomType    loại phòng mới
     * @param financialResult kết quả tài chính
     * @param isSameRoomType {@code true} nếu cùng loại phòng
     * @param newPassword    mật khẩu mới cho phòng
     * @param staff          nhân viên thực hiện
     * @param now            thời điểm thực hiện
     * @param reason         lý do chuyển phòng
     * @return response đầy đủ thông tin
     */
    private RoomChangeResponse buildResponse(
            Booking booking,
            Bookingdetail detail,
            Room oldRoom,
            Room newRoom,
            Roomtype oldRoomType,
            Roomtype newRoomType,
            FinancialResult financialResult,
            boolean isSameRoomType,
            String newPassword,
            User staff,
            Instant now,
            String reason) {

        String changeType = resolveChangeType(isSameRoomType, financialResult.getPriceDifference());
        String message = buildChangeMessage(changeType, oldRoom, newRoom, financialResult);

        return RoomChangeResponse.builder()
                // Thông tin booking
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingreference())
                .bookingStatus(booking.getStatus())
                // Phòng cũ
                .oldRoomId(oldRoom.getId())
                .oldRoomNumber(oldRoom.getRoomnumber())
                .oldRoomTypeName(oldRoomType.getName())
                // Phòng mới
                .newRoomId(newRoom.getId())
                .newRoomNumber(newRoom.getRoomnumber())
                .newRoomTypeId(newRoomType.getId())
                .newRoomTypeName(newRoomType.getName())
                .newRoomPassword(newPassword)
                // Kết quả tài chính
                .changeType(changeType)
                .priceDifference(financialResult.getPriceDifference().abs()) // luôn trả dương
                .newTotalAmount(financialResult.getNewTotalAmount())
                .newFinalAmount(financialResult.getNewFinalAmount())
                // Thông tin thực hiện
                .changedAt(now)
                .changedByStaff(staff.getFullname() != null ? staff.getFullname() : staff.getEmail())
                .reason(reason)
                .message(message)
                .build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  UTILITY METHODS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Xác định loại chuyển phòng dựa trên sự thay đổi giá.
     *
     * @param isSameRoomType  {@code true} nếu cùng loại phòng
     * @param priceDifference chênh lệch giá (có thể âm hoặc dương)
     * @return {@code "SAME_TYPE"}, {@code "UPGRADE"}, hoặc {@code "DOWNGRADE"}
     */
    private String resolveChangeType(boolean isSameRoomType, BigDecimal priceDifference) {
        if (isSameRoomType) {
            return "SAME_TYPE";
        }
        return priceDifference.compareTo(BigDecimal.ZERO) > 0 ? "UPGRADE" : "DOWNGRADE";
    }

    /**
     * Tạo thông báo mô tả kết quả chuyển phòng cho nhân viên.
     *
     * @param changeType      loại chuyển phòng
     * @param oldRoom         phòng cũ
     * @param newRoom         phòng mới
     * @param financialResult kết quả tài chính
     * @return chuỗi thông báo thân thiện
     */
    private String buildChangeMessage(
            String changeType,
            Room oldRoom,
            Room newRoom,
            FinancialResult financialResult) {

        String base = String.format("Chuyển phòng thành công: %s → %s. ",
                oldRoom.getRoomnumber(), newRoom.getRoomnumber());

        return switch (changeType) {
            case "SAME_TYPE" ->
                    base + "Cùng loại phòng — giá không thay đổi.";
            case "UPGRADE" ->
                    base + String.format(
                            "Phòng mới cao cấp hơn — khách cần trả thêm %,.0f VND.",
                            financialResult.getPriceDifference().abs());
            case "DOWNGRADE" ->
                    base + String.format(
                            "Phòng mới rẻ hơn — ghi nhận credit %,.0f VND cho khách.",
                            financialResult.getPriceDifference().abs());
            default ->
                    base;
        };
    }

    /**
     * Sinh mật khẩu ngẫu nhiên 6 chữ số cho phòng.
     *
     * @return chuỗi 6 chữ số (ví dụ: "042873")
     */
    private String generateRoomPassword() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  CUSTOMER REQUEST API IMPLEMENTATIONS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Khách hàng gửi yêu cầu chuyển phòng.
     */
    @Override
    @Transactional
    public CustomerRequestResponse submitRoomChangeRequest(CustomerRoomChangeRequest request, String customerEmail) {
        log.info("[RoomChangeRequest] Khách hàng '{}' yêu cầu đổi sang hạng phòng ID={}", customerEmail, request.getNewRoomId());

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + request.getBookingId()));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng: " + customerEmail));

        // Xác thực quyền: Phải là chủ đặt phòng hoặc nhân viên
        boolean isStaff = "receptionist".equalsIgnoreCase(customer.getRole().name())
                || "manager".equalsIgnoreCase(customer.getRole().name());

        if (!isStaff && !customer.getId().equals(booking.getUserid().getId())) {
            throw new RuntimeException("Bạn không có quyền gửi yêu cầu chuyển phòng cho đơn đặt phòng này.");
        }

        // Validate booking đang check-in
        String status = booking.getStatus();
        boolean isCheckedIn = BOOKING_STATUS_CHECKED_IN.equalsIgnoreCase(status)
                || "Checked In".equalsIgnoreCase(status)
                || "Checked-In".equalsIgnoreCase(status);

        if (!isCheckedIn) {
            throw new RuntimeException("Chỉ có thể gửi yêu cầu chuyển phòng khi đang lưu trú (Đã Check-in).");
        }

        Bookingdetail detail = fetchBookingDetail(booking.getId());
        Room currentRoom = detail.getRoomid();
        validateCurrentRoomAssigned(currentRoom, booking.getId());

        // Validate loại phòng mới yêu cầu
        Roomtype currentRoomType = currentRoom.getRoomtypeid();
        if (currentRoomType.getId().equals(request.getNewRoomId())) {
            // Trường hợp cùng loại phòng: hợp lệ
            log.info("[RoomChangeRequest] Khách hàng yêu cầu đổi sang phòng cùng loại: roomTypeId={}", request.getNewRoomId());
        }

        // Lưu yêu cầu đổi phòng vào bảng customerrequests (newvalue lưu roomTypeId mong muốn)
        Customerrequest customerRequest = new Customerrequest();
        customerRequest.setBookingid(booking);
        customerRequest.setRequesttype("RoomChange");
        customerRequest.setDescription(request.getReason() != null ? request.getReason() : "Yêu cầu đổi hạng phòng");
        customerRequest.setOldvalue(currentRoom.getId().toString()); // Lưu ID phòng cũ
        customerRequest.setNewvalue(request.getNewRoomId().toString()); // Lưu ID loại phòng mới yêu cầu
        customerRequest.setStatus("Pending");
        customerRequest.setCreatedat(Instant.now());

        Customerrequest saved = customerrequestRepository.save(customerRequest);

        return mapToCustomerRequestResponse(saved);
    }

    /**
     * Nhân viên duyệt yêu cầu đổi phòng của khách.
     */
    @Override
    @Transactional
    public CustomerRequestResponse approveRoomChangeRequest(Integer requestId, Integer newRoomId, String staffEmail) {
        log.info("[RoomChangeRequest] Nhân viên '{}' duyệt yêu cầu đổi phòng ID={} bằng cách gán phòng ID={}", staffEmail, requestId, newRoomId);

        // 1. Validate quyền staff
        User staff = validateStaffPermission(staffEmail);

        // 2. Fetch request
        Customerrequest req = customerrequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu chuyển phòng với ID: " + requestId));

        if (!"RoomChange".equalsIgnoreCase(req.getRequesttype())) {
            throw new RuntimeException("Yêu cầu này không phải là loại chuyển phòng (RoomChange).");
        }

        if (!"Pending".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý từ trước (Trạng thái: " + req.getStatus() + ").");
        }

        // 3. Kiểm tra phòng mới được lễ tân gán
        Room newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng được chỉ định với ID: " + newRoomId));

        if (!"Available".equalsIgnoreCase(newRoom.getStatus())) {
            throw new RuntimeException(String.format("Phòng %s hiện tại không trống (trạng thái: %s).", newRoom.getRoomnumber(), newRoom.getStatus()));
        }

        // Kiểm tra phòng chọn có đúng hạng phòng yêu cầu hay không
        Integer newRoomTypeId = Integer.parseInt(req.getNewvalue());
        if (!newRoom.getRoomtypeid().getId().equals(newRoomTypeId)) {
            throw new RuntimeException("Phòng được chọn không thuộc hạng phòng mà khách hàng yêu cầu đổi sang.");
        }

        Integer bookingId = req.getBookingid().getId();
        String reason = req.getDescription();

        // Chuẩn bị payload và gọi method changeRoom đã có
        RoomChangeRequest changeRequest = new RoomChangeRequest();
        changeRequest.setBookingId(bookingId);
        changeRequest.setNewRoomId(newRoom.getId());
        changeRequest.setReason(reason);

        // Gọi method chuyển phòng
        changeRoom(changeRequest, staffEmail);

        // 4. Cập nhật trạng thái yêu cầu
        req.setStatus("Approved");
        req.setResolvedat(Instant.now());
        Customerrequest saved = customerrequestRepository.save(req);

        return mapToCustomerRequestResponse(saved);
    }

    /**
     * Nhân viên từ chối yêu cầu đổi phòng của khách.
     */
    @Override
    @Transactional
    public CustomerRequestResponse rejectRoomChangeRequest(Integer requestId, String rejectionReason, String staffEmail) {
        log.info("[RoomChangeRequest] Nhân viên '{}' từ chối yêu cầu đổi phòng ID={}, Lý do: {}", staffEmail, requestId, rejectionReason);

        // 1. Validate quyền staff
        validateStaffPermission(staffEmail);

        // 2. Fetch request
        Customerrequest req = customerrequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu chuyển phòng với ID: " + requestId));

        if (!"RoomChange".equalsIgnoreCase(req.getRequesttype())) {
            throw new RuntimeException("Yêu cầu này không phải là loại chuyển phòng (RoomChange).");
        }

        if (!"Pending".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý từ trước.");
        }

        // 3. Cập nhật trạng thái từ chối
        req.setStatus("Rejected");
        req.setRejectionreason(rejectionReason);
        req.setResolvedat(Instant.now());
        Customerrequest saved = customerrequestRepository.save(req);

        return mapToCustomerRequestResponse(saved);
    }

    /**
     * Lấy tất cả yêu cầu chuyển phòng đang chờ duyệt.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CustomerRequestResponse> getPendingRoomChangeRequests(String staffEmail) {
        // Validate staff
        validateStaffPermission(staffEmail);

        List<Customerrequest> pendingRequests = customerrequestRepository
                .findByRequesttypeAndStatusOrderByCreatedatDesc("RoomChange", "Pending");

        return pendingRequests.stream()
                .map(this::mapToCustomerRequestResponse)
                .toList();
    }

    /**
     * Helper mapper map từ Customerrequest sang CustomerRequestResponse.
     */
    private CustomerRequestResponse mapToCustomerRequestResponse(Customerrequest req) {
        return CustomerRequestResponse.builder()
                .requestId(req.getId())
                .bookingId(req.getBookingid().getId())
                .bookingReference(req.getBookingid().getBookingreference())
                .requestType(req.getRequesttype())
                .description(req.getDescription())
                .oldValue(req.getOldvalue())
                .newValue(req.getNewvalue())
                .status(req.getStatus())
                .resolvedAt(req.getResolvedat())
                .rejectionReason(req.getRejectionreason())
                .createdAt(req.getCreatedat())
                .build();
    }

    private Instant toInstant(LocalDate date) {
        return date.atStartOfDay(HOTEL_ZONE).toInstant();
    }

    private void assertAvailability(Roomtype roomtype, LocalDate checkInDate, LocalDate checkOutDate, int quantity) {
        int totalRooms = Math.toIntExact(roomRepository.countByRoomtypeid_IdAndStatus(
                roomtype.getId(), ROOM_STATUS_AVAILABLE));

        if (totalRooms <= 0) {
            throw new RuntimeException("Không có phòng đang hoạt động cho loại phòng này");
        }

        if (quantity > totalRooms) {
            throw new RuntimeException("Số lượng phòng đặt vượt quá tổng số phòng của loại phòng này");
        }

        LocalDate stayDate = checkInDate;
        while (stayDate.isBefore(checkOutDate)) {
            Instant periodStart = toInstant(stayDate);
            Instant periodEnd = toInstant(stayDate.plusDays(1));

            long bookedRooms = bookingdetailRepository.sumBookedQuantity(
                    roomtype.getId(),
                    periodStart,
                    periodEnd,
                    List.of("Confirmed", "Checked In", "Checked-in", "Staying", "Paid", "Partially Paid"),
                    List.of("Active", "Checked In", "Checked-in", "Staying"));

            int availableRooms = totalRooms - Math.toIntExact(bookedRooms);
            if (availableRooms < quantity) {
                throw new RuntimeException("Không đủ phòng trống vào ngày "
                        + stayDate.toString()
                        + ". Số phòng còn trống: " + availableRooms);
            }

            stayDate = stayDate.plusDays(1);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  INNER VALUE OBJECT: FinancialResult
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Value Object chứa kết quả tính toán tài chính sau khi chuyển phòng.
     *
     * <p>Giúp truyền nhiều giá trị tài chính qua các phương thức mà không cần
     * tạo nhiều biến tham số rời rạc.
     */
    private static class FinancialResult {

        private final BigDecimal newTotalAmount;
        private final BigDecimal newFinalAmount;
        private final BigDecimal priceDifference; // Dương: upgrade, Âm: downgrade, Không: same type
        private final boolean differentType;

        private FinancialResult(BigDecimal newTotalAmount, BigDecimal newFinalAmount,
                                BigDecimal priceDifference, boolean differentType) {
            this.newTotalAmount = newTotalAmount;
            this.newFinalAmount = newFinalAmount;
            this.priceDifference = priceDifference;
            this.differentType = differentType;
        }

        /**
         * Factory method cho trường hợp cùng loại phòng.
         * Giá trị giữ nguyên từ booking gốc.
         */
        static FinancialResult sameType(BigDecimal currentTotal, BigDecimal currentFinal) {
            return new FinancialResult(currentTotal, currentFinal, BigDecimal.ZERO, false);
        }

        /**
         * Factory method cho trường hợp khác loại phòng.
         *
         * @param newTotal    tổng tiền phòng mới
         * @param newFinal    tổng tiền final (sau thuế) mới
         * @param difference  chênh lệch (dương = upgrade, âm = downgrade)
         */
        static FinancialResult differentType(BigDecimal newTotal, BigDecimal newFinal,
                                              BigDecimal difference) {
            return new FinancialResult(newTotal, newFinal, difference, true);
        }

        BigDecimal getNewTotalAmount() { return newTotalAmount; }
        BigDecimal getNewFinalAmount() { return newFinalAmount; }
        BigDecimal getPriceDifference() { return priceDifference; }
        boolean isDifferentType() { return differentType; }
    }

    /**
     * Khách hàng gửi yêu cầu gia hạn lưu trú.
     */
    @Override
    @Transactional
    public CustomerRequestResponse submitStayExtensionRequest(CustomerStayExtensionRequest request, String customerEmail) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (!booking.getUserid().getId().equals(customer.getId())) {
            throw new RuntimeException("Bạn không có quyền gửi yêu cầu cho đơn đặt phòng này");
        }

        if (!"Checked-in".equalsIgnoreCase(booking.getStatus()) && !"Checked In".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Chỉ đơn đặt phòng đang lưu trú (Checked In) mới được phép yêu cầu gia hạn");
        }

        Bookingdetail detail = bookingdetailRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        LocalDate currentCheckOut = detail.getExpectedcheckout().atZone(HOTEL_ZONE).toLocalDate();
        LocalDate newCheckOut = LocalDate.parse(request.getNewCheckOutDate().trim());

        if (newCheckOut.isBefore(currentCheckOut) || newCheckOut.equals(currentCheckOut)) {
            throw new RuntimeException("Ngày trả phòng mới phải sau ngày trả phòng hiện tại (" + currentCheckOut + ")");
        }

        // Tạo request StayExtension
        Customerrequest customerrequest = new Customerrequest();
        customerrequest.setBookingid(booking);
        customerrequest.setRequesttype("StayExtension");
        customerrequest.setDescription(request.getDescription());
        customerrequest.setOldvalue(currentCheckOut.toString());
        customerrequest.setNewvalue(newCheckOut.toString());
        customerrequest.setStatus("Pending");
        customerrequest.setCreatedat(Instant.now());

        Customerrequest saved = customerrequestRepository.save(customerrequest);
        return mapToCustomerRequestResponse(saved);
    }

    /**
     * Nhân viên phê duyệt yêu cầu gia hạn lưu trú của khách.
     */
    @Override
    @Transactional
    public CustomerRequestResponse approveStayExtensionRequest(Integer requestId, String staffEmail) {
        validateStaffPermission(staffEmail);

        Customerrequest req = customerrequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu gia hạn"));

        if (!req.getRequesttype().equals("StayExtension")) {
            throw new RuntimeException("Yêu cầu này không phải là yêu cầu gia hạn lưu trú");
        }

        if (!req.getStatus().equals("Pending")) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        Booking booking = req.getBookingid();
        Bookingdetail detail = bookingdetailRepository.findById(booking.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        LocalDate checkIn = detail.getExpectedcheckin().atZone(HOTEL_ZONE).toLocalDate();
        LocalDate newCheckOut = LocalDate.parse(req.getNewvalue());

        // 1. Kiểm tra availability phòng trống của loại phòng này trước
        assertAvailability(detail.getRoomtypeid(), checkIn, newCheckOut, detail.getQuantity());

        // 2. Đảm bảo gia hạn chính phòng vật lý hiện tại của khách (kiểm tra xem phòng đó có bị đặt trùng trong tương lai không)
        if (detail.getRoomid() != null) {
            boolean hasOverlap = bookingdetailRepository.existsOverlappingBookingForRoom(
                    detail.getRoomid().getId(),
                    booking.getId(),
                    detail.getExpectedcheckout(), // kiểm tra từ ngày checkout cũ
                    toInstant(newCheckOut)        // đến ngày checkout mới mong muốn
            );
            if (hasOverlap) {
                throw new RuntimeException("Không thể gia hạn chính phòng này vì phòng vật lý "
                        + detail.getRoomid().getRoomnumber()
                        + " đã có khách hàng khác đặt trước trong thời gian gia hạn. Vui lòng đổi sang phòng khác.");
            }
        }

        // Tính toán lại tài chính
        long newNights = ChronoUnit.DAYS.between(checkIn, newCheckOut);
        BigDecimal newTotal = detail.getPriceatbooking()
                .multiply(BigDecimal.valueOf(detail.getQuantity()))
                .multiply(BigDecimal.valueOf(newNights));

        BigDecimal taxAmount = newTotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = newTotal.add(taxAmount).subtract(booking.getDiscountamount()).setScale(2, RoundingMode.HALF_UP);

        // Cập nhật booking & detail
        booking.setTotalamount(newTotal);
        booking.setTaxamount(taxAmount);
        booking.setFinalamount(finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount);
        booking.setUpdatedat(Instant.now());

        detail.setExpectedcheckout(toInstant(newCheckOut));
        detail.setUpdatedat(Instant.now());

        // 3. Gia hạn thời hạn của mã khóa phòng (bookingroomaccesses)
        List<BookingRoomAccess> roomAccesses = bookingRoomAccessRepository
                .findByBookingid_IdOrderByRoomid_RoomnumberAsc(booking.getId());
        if (roomAccesses != null && !roomAccesses.isEmpty()) {
            for (BookingRoomAccess access : roomAccesses) {
                access.setRoomkeyexpiredat(toInstant(newCheckOut));
                access.setUpdatedat(Instant.now());
            }
            bookingRoomAccessRepository.saveAll(roomAccesses);
        }

        bookingRepository.save(booking);
        bookingdetailRepository.save(detail);

        // Cập nhật request
        req.setStatus("Approved");
        req.setResolvedat(Instant.now());
        Customerrequest saved = customerrequestRepository.save(req);

        return mapToCustomerRequestResponse(saved);
    }

    /**
     * Nhân viên từ chối yêu cầu gia hạn lưu trú của khách.
     */
    @Override
    @Transactional
    public CustomerRequestResponse rejectStayExtensionRequest(Integer requestId, String rejectionReason, String staffEmail) {
        validateStaffPermission(staffEmail);

        Customerrequest req = customerrequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu gia hạn"));

        if (!req.getRequesttype().equals("StayExtension")) {
            throw new RuntimeException("Yêu cầu này không phải là yêu cầu gia hạn");
        }

        if (!req.getStatus().equals("Pending")) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        req.setStatus("Rejected");
        req.setRejectionreason(rejectionReason);
        req.setResolvedat(Instant.now());

        Customerrequest saved = customerrequestRepository.save(req);

        return mapToCustomerRequestResponse(saved);
    }

    /**
     * Lấy danh sách các yêu cầu gia hạn lưu trú đang ở trạng thái Pending.
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.List<CustomerRequestResponse> getPendingStayExtensionRequests(String staffEmail) {
        validateStaffPermission(staffEmail);

        List<Customerrequest> pendingRequests = customerrequestRepository
                .findByRequesttypeAndStatusOrderByCreatedatDesc("StayExtension", "Pending");

        return pendingRequests.stream()
                .map(this::mapToCustomerRequestResponse)
                .toList();
    }
}
