package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.RoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.response.RoomChangeResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.entity.BookingRoomAccess;
import com.example.hotelsmartbookingbackend.entity.BookingDetail;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.BookingRoomAccessRepository;
import com.example.hotelsmartbookingbackend.repository.BookingDetailRepository;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.service.RoomChangeService;
import com.example.hotelsmartbookingbackend.service.WebSocketService;

import com.example.hotelsmartbookingbackend.dto.request.CustomerRoomChangeRequest;
import com.example.hotelsmartbookingbackend.dto.request.CustomerStayExtensionRequest;
import com.example.hotelsmartbookingbackend.dto.request.CustomerEarlyCheckOutRequest;
import com.example.hotelsmartbookingbackend.dto.response.CustomerRequestResponse;
import com.example.hotelsmartbookingbackend.entity.CustomerRequest;
import com.example.hotelsmartbookingbackend.repository.CustomerRequestRepository;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomChangeServiceImpl implements RoomChangeService {

    private static final String ROOM_STATUS_AVAILABLE = "Available";
    private static final String ROOM_STATUS_OCCUPIED = "Occupied";
    private static final String ROOM_KEY_STATUS_ACTIVE = "Active";
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final BookingRoomAccessRepository bookingRoomAccessRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final CustomerRequestRepository customerRequestRepository;
    private final com.example.hotelsmartbookingbackend.service.NotificationService notificationService;
    private final com.example.hotelsmartbookingbackend.repository.ServiceRepository serviceRepository;
    private final com.example.hotelsmartbookingbackend.repository.BookingServiceRepository bookingServiceRepository;

    @Override
    @Transactional
    public RoomChangeResponse changeRoom(RoomChangeRequest request, String staffEmail) {
        log.info("[RoomChange] Staff='{}' yêu cầu chuyển phòng: bookingId={}, newRoomId={}",
                staffEmail, request.getBookingId(), request.getNewRoomId());

        // ── Bước 1: Xác thực quyền nhân viên ─────────────────────────────────
        User staff = validateStaffPermission(staffEmail);

        // ── Bước 2: Kiểm tra Booking & trạng thái ────────────────────────────
        Booking booking = validateBookingIsCheckedIn(request.getBookingId());
        BookingDetail detail = fetchBookingDetail(booking.getId());

        // Phòng hiện tại của khách (phòng cũ)
        Room currentRoom = detail.getRoom();
        validateCurrentRoomAssigned(currentRoom, booking.getId());

        // ── Bước 3: Kiểm tra phòng mới hợp lệ & đang trống ──────────────────
        Room newRoom = fetchAndValidateNewRoom(request.getNewRoomId(), currentRoom);

        // ── Bước 4: So sánh RoomType & tính toán tài chính ───────────────────
        RoomType oldRoomType = currentRoom.getRoomType();
        RoomType newRoomType = newRoom.getRoomType();

        boolean isSameRoomType = oldRoomType.getId().equals(newRoomType.getId());
        FinancialResult financialResult = computeFinancialResult(
                isSameRoomType, booking, detail, newRoomType);

        // ── Bước 5 & 6: Thực hiện chuyển phòng ───────────────────────────────
        Instant now = Instant.now();
        String newRoomPassword = performRoomSwap(
                booking, detail, currentRoom, newRoom, financialResult, now);

        // ── Bước 7: Build và trả về response ─────────────────────────────────
        RoomChangeResponse response = buildResponse(
                booking, currentRoom, newRoom,
                oldRoomType, newRoomType,
                financialResult, isSameRoomType,
                newRoomPassword, staff, now, request.getReason());

        try {
            String changeMsg = String.format(
                    "Phòng của bạn đã được chuyển từ phòng %s (%s) sang phòng %s (%s) thành công. Mật khẩu cửa phòng mới đã được cập nhật.",
                    currentRoom.getRoomNumber(), oldRoomType.getName(), newRoom.getRoomNumber(), newRoomType.getName());
            notificationService.sendNotification(booking.getUser(), "Chuyển phòng thành công", changeMsg, "RoomChange",
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send room change notification: ", e);
        }

        log.info("[RoomChange] Hoàn thành chuyển phòng: bookingId={}, phòng {} → {}, loại={}",
                booking.getId(), currentRoom.getRoomNumber(), newRoom.getRoomNumber(),
                isSameRoomType ? "SAME_TYPE" : "DIFFERENT_TYPE");

        return response;
    }

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

    private Booking validateBookingIsCheckedIn(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + bookingId));

        BookingStatus status = booking.getStatus();
        boolean isCheckedIn = (status == BookingStatus.CHECKED_IN);

        if (!isCheckedIn) {
            throw new RuntimeException(String.format(
                    "Không thể chuyển phòng. Đơn đặt phòng #%d đang ở trạng thái '%s'. " +
                            "Chỉ có thể chuyển phòng khi khách đã check-in.",
                    bookingId, status));
        }

        return booking;
    }

    private BookingDetail fetchBookingDetail(Integer bookingId) {
        return bookingDetailRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy chi tiết đơn đặt phòng với ID: " + bookingId));
    }

    private void validateCurrentRoomAssigned(Room currentRoom, Integer bookingId) {
        if (currentRoom == null) {
            throw new RuntimeException(String.format(
                    "Đơn đặt phòng #%d chưa được gán phòng cụ thể. Không thể thực hiện chuyển phòng.",
                    bookingId));
        }
    }

    private Room fetchAndValidateNewRoom(Integer newRoomId, Room currentRoom) {
        // Phòng mới phải tồn tại
        Room newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy phòng với ID: " + newRoomId));

        // Không được chuyển sang chính phòng hiện tại
        if (currentRoom.getId().equals(newRoom.getId())) {
            throw new RuntimeException(
                    "Phòng mới không được trùng với phòng hiện tại (Phòng " +
                            currentRoom.getRoomNumber() + ").");
        }

        // Phòng mới phải đang trống
        String newRoomStatus = newRoom.getStatus();
        if (!ROOM_STATUS_AVAILABLE.equalsIgnoreCase(newRoomStatus)) {
            throw new RuntimeException(String.format(
                    "Phòng '%s' hiện không trống (trạng thái: %s). Vui lòng chọn phòng khác.",
                    newRoom.getRoomNumber(), newRoomStatus));
        }

        return newRoom;
    }

    private FinancialResult computeFinancialResult(
            boolean isSameRoomType,
            Booking booking,
            BookingDetail detail,
            RoomType newRoomType) {

        if (isSameRoomType) {
            // Cùng loại phòng: giữ nguyên giá, không phát sinh chênh lệch
            return FinancialResult.sameType(booking.getTotalAmount(), booking.getFinalAmount());
        }

        // Khác loại phòng: tính lại tiền từ ngày hôm nay đến ngày checkout
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        LocalDate checkoutDate = detail.getExpectedCheckOut()
                .atZone(HOTEL_ZONE).toLocalDate();

        // Số đêm còn lại (tính từ hôm nay để phản ánh thực tế)
        long remainingNights = ChronoUnit.DAYS.between(today, checkoutDate);
        if (remainingNights <= 0) {
            // Trường hợp biên: nếu đã đến ngày checkout, vẫn tính ít nhất 1 đêm
            remainingNights = 1;
        }

        int quantity = detail.getQuantity() != null ? detail.getQuantity() : 1;
        BigDecimal newBasePrice = newRoomType.getBasePrice();

        // Tính lại tiền phòng cho số đêm còn lại với loại phòng mới
        BigDecimal newTotalAmount = newBasePrice
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
        BigDecimal currentFinalAmount = booking.getFinalAmount();
        BigDecimal difference = newFinalAmount.subtract(currentFinalAmount)
                .setScale(2, RoundingMode.HALF_UP);

        return FinancialResult.differentType(newTotalAmount, newFinalAmount, difference);
    }

    private String performRoomSwap(
            Booking booking,
            BookingDetail detail,
            Room currentRoom,
            Room newRoom,
            FinancialResult financialResult,
            Instant now) {

        // ── 5a. Cập nhật trạng thái phòng cũ → Available ─────────────────────
        log.debug("[RoomChange] Giải phóng phòng cũ: {} → Available", currentRoom.getRoomNumber());
        currentRoom.setStatus(ROOM_STATUS_AVAILABLE);
        currentRoom.setUpdatedAt(now);
        roomRepository.save(currentRoom);

        // Broadcast trạng thái phòng cũ qua WebSocket
        webSocketService.broadcastRoomStatus(
                currentRoom.getId(), currentRoom.getRoomNumber(), ROOM_STATUS_AVAILABLE);

        // ── 5b. Cập nhật trạng thái phòng mới → Occupied ─────────────────────
        log.debug("[RoomChange] Chiếm phòng mới: {} → Occupied", newRoom.getRoomNumber());
        newRoom.setStatus(ROOM_STATUS_OCCUPIED);
        newRoom.setUpdatedAt(now);
        roomRepository.save(newRoom);

        // Broadcast trạng thái phòng mới qua WebSocket
        webSocketService.broadcastRoomStatus(
                newRoom.getId(), newRoom.getRoomNumber(), ROOM_STATUS_OCCUPIED);

        // ── 6a. Cập nhật BookingRoomAccess ────────────────────────────────────
        String newPassword = updateBookingRoomAccesses(booking, currentRoom, newRoom, now);

        // ── 6b. Cập nhật BookingDetail ────────────────────────────────────────
        updateBookingDetail(detail, newRoom, financialResult, now);

        // ── 6c. Cập nhật Booking (tài chính nếu khác loại phòng) ──────────────
        if (financialResult.isDifferentType()) {
            updateBookingFinancials(booking, financialResult, now);
        } else {
            // Cùng loại phòng: chỉ cập nhật timestamp
            booking.setUpdatedAt(now);
            bookingRepository.save(booking);
        }

        return newPassword;
    }

    private String updateBookingRoomAccesses(
            Booking booking,
            Room currentRoom,
            Room newRoom,
            Instant now) {

        List<BookingRoomAccess> accesses = bookingRoomAccessRepository
                .findByBooking_IdOrderByRoom_RoomNumberAsc(booking.getId());

        String newPassword = generateRoomPassword();
        boolean foundOldRoom = false;

        for (BookingRoomAccess access : accesses) {
            if (access.getRoom().getId().equals(currentRoom.getId())) {
                // Thu hồi access phòng cũ và tái sử dụng record này cho phòng mới
                foundOldRoom = true;
                Instant originalExpiry = access.getRoomKeyExpiredAt(); // Giữ nguyên thời hạn

                access.setRoom(newRoom);
                access.setRoomKeyAccess(newPassword);
                access.setRoomKeyGeneratedAt(now);
                access.setRoomKeyExpiredAt(originalExpiry); // Kế thừa thời hạn cũ
                access.setRoomKeyStatus(ROOM_KEY_STATUS_ACTIVE);
                access.setUpdatedAt(now);

                log.debug("[RoomChange] Cập nhật BookingRoomAccess: phòng {} → {}, key mới được tạo",
                        currentRoom.getRoomNumber(), newRoom.getRoomNumber());
            }
        }

        // Trường hợp không tìm thấy access record của phòng cũ
        if (!foundOldRoom && !accesses.isEmpty()) {
            log.warn("[RoomChange] Không tìm thấy BookingRoomAccess cho phòng cũ '{}'. " +
                    "Cập nhật record đầu tiên.", currentRoom.getRoomNumber());
            BookingRoomAccess firstAccess = accesses.get(0);
            Instant originalExpiry = firstAccess.getRoomKeyExpiredAt();

            firstAccess.setRoom(newRoom);
            firstAccess.setRoomKeyAccess(newPassword);
            firstAccess.setRoomKeyGeneratedAt(now);
            firstAccess.setRoomKeyExpiredAt(originalExpiry);
            firstAccess.setRoomKeyStatus(ROOM_KEY_STATUS_ACTIVE);
            firstAccess.setUpdatedAt(now);
        }

        bookingRoomAccessRepository.saveAll(accesses);
        return newPassword;
    }

    private void updateBookingDetail(
            BookingDetail detail,
            Room newRoom,
            FinancialResult financialResult,
            Instant now) {

        // Gán phòng mới
        detail.setRoom(newRoom);

        if (financialResult.isDifferentType()) {
            // Khác loại phòng: cập nhật roomtype và giá mới
            detail.setRoomType(newRoom.getRoomType());
            detail.setPriceAtBooking(newRoom.getRoomType().getBasePrice());
            log.debug("[RoomChange] Cập nhật BookingDetail: loại phòng mới='{}', giá mới={}",
                    newRoom.getRoomType().getName(), newRoom.getRoomType().getBasePrice());
        }

        // Cập nhật thông tin key phòng mới trong BookingDetail (sync với
        // BookingRoomAccess)
        detail.setRoomKeyAccess(null); // BookingRoomAccess là nguồn chính xác cho key
        detail.setRoomKeyStatus(ROOM_KEY_STATUS_ACTIVE);
        detail.setUpdatedAt(now);

        bookingDetailRepository.save(detail);
    }

    private void updateBookingFinancials(Booking booking, FinancialResult financialResult, Instant now) {
        log.debug("[RoomChange] Cập nhật tài chính Booking: totalAmount={}, finalAmount={}",
                financialResult.getNewTotalAmount(), financialResult.getNewFinalAmount());

        booking.setTotalAmount(financialResult.getNewTotalAmount());

        // Tính lại tax dựa trên total mới
        BigDecimal newTaxAmount = financialResult.getNewTotalAmount()
                .multiply(TAX_RATE)
                .setScale(2, RoundingMode.HALF_UP);
        booking.setTaxAmount(newTaxAmount);
        booking.setFinalAmount(financialResult.getNewFinalAmount());
        booking.setUpdatedAt(now);

        bookingRepository.save(booking);
    }

    private RoomChangeResponse buildResponse(
            Booking booking,
            Room oldRoom,
            Room newRoom,
            RoomType oldRoomType,
            RoomType newRoomType,
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
                .bookingReference(booking.getBookingReference())
                .bookingStatus(booking.getStatus() != null ? booking.getStatus().getValue() : null)
                // Phòng cũ
                .oldRoomId(oldRoom.getId())
                .oldRoomNumber(oldRoom.getRoomNumber())
                .oldRoomTypeName(oldRoomType.getName())
                // Phòng mới
                .newRoomId(newRoom.getId())
                .newRoomNumber(newRoom.getRoomNumber())
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
                .changedByStaff(staff.getFullName() != null ? staff.getFullName() : staff.getEmail())
                .reason(reason)
                .message(message)
                .build();
    }

    private String resolveChangeType(boolean isSameRoomType, BigDecimal priceDifference) {
        if (isSameRoomType) {
            return "SAME_TYPE";
        }
        return priceDifference.compareTo(BigDecimal.ZERO) > 0 ? "UPGRADE" : "DOWNGRADE";
    }

    private String buildChangeMessage(
            String changeType,
            Room oldRoom,
            Room newRoom,
            FinancialResult financialResult) {

        String base = String.format("Chuyển phòng thành công: %s → %s. ",
                oldRoom.getRoomNumber(), newRoom.getRoomNumber());

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

    private String generateRoomPassword() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    @Override
    @Transactional
    public CustomerRequestResponse submitRoomChangeRequest(CustomerRoomChangeRequest request, String customerEmail) {
        log.info("[RoomChangeRequest] Khách hàng '{}' yêu cầu đổi sang hạng phòng ID={}", customerEmail,
                request.getNewRoomId());

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(
                        () -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + request.getBookingId()));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng: " + customerEmail));

        // Xác thực quyền: Phải là chủ đặt phòng hoặc nhân viên
        boolean isStaff = "receptionist".equalsIgnoreCase(customer.getRole().name())
                || "manager".equalsIgnoreCase(customer.getRole().name());

        if (!isStaff && !customer.getId().equals(booking.getUser().getId())) {
            throw new RuntimeException("Bạn không có quyền gửi yêu cầu chuyển phòng cho đơn đặt phòng này.");
        }

        // Validate booking đang check-in
        BookingStatus status = booking.getStatus();
        boolean isCheckedIn = (status == BookingStatus.CHECKED_IN);

        if (!isCheckedIn) {
            throw new RuntimeException("Chỉ có thể gửi yêu cầu chuyển phòng khi đang lưu trú (Đã Check-in).");
        }

        BookingDetail detail = fetchBookingDetail(booking.getId());
        Room currentRoom = detail.getRoom();
        validateCurrentRoomAssigned(currentRoom, booking.getId());

        // Validate loại phòng mới yêu cầu
        RoomType currentRoomType = currentRoom.getRoomType();
        if (currentRoomType.getId().equals(request.getNewRoomId())) {
            // Trường hợp cùng loại phòng: hợp lệ
            log.info("[RoomChangeRequest] Khách hàng yêu cầu đổi sang phòng cùng loại: roomTypeId={}",
                    request.getNewRoomId());
        }

        // Lưu yêu cầu đổi phòng vào bảng customerrequests (newvalue lưu roomTypeId mong
        // muốn)
        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setBooking(booking);
        customerRequest.setRequestType("RoomChange");
        customerRequest.setDescription(request.getReason() != null ? request.getReason() : "Yêu cầu đổi hạng phòng");
        customerRequest.setOldValue(currentRoom.getId().toString()); // Lưu ID phòng cũ
        customerRequest.setNewValue(request.getNewRoomId().toString()); // Lưu ID loại phòng mới yêu cầu
        customerRequest.setStatus("Pending");
        customerRequest.setCreatedAt(Instant.now());

        CustomerRequest saved = customerRequestRepository.save(customerRequest);

        try {
            String changeReqMsg = String.format("Khách hàng %s yêu cầu đổi phòng cho đơn %s. Lý do: %s.",
                    customer.getFullName(), booking.getBookingReference(), customerRequest.getDescription());
            notificationService.sendNotificationToRoles(
                    List.of(com.example.hotelsmartbookingbackend.enums.Role.receptionist,
                            com.example.hotelsmartbookingbackend.enums.Role.manager),
                    "Yêu cầu đổi phòng mới",
                    changeReqMsg,
                    "RoomChange",
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send room change request notification: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional
    public CustomerRequestResponse approveRoomChangeRequest(Integer requestId, Integer newRoomId, String staffEmail) {
        log.info("[RoomChangeRequest] Nhân viên '{}' duyệt yêu cầu đổi phòng ID={} bằng cách gán phòng ID={}",
                staffEmail, requestId, newRoomId);

        // 1. Validate quyền staff

        // 2. Fetch request
        CustomerRequest req = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu chuyển phòng với ID: " + requestId));

        if (!"RoomChange".equalsIgnoreCase(req.getRequestType())) {
            throw new RuntimeException("Yêu cầu này không phải là loại chuyển phòng (RoomChange).");
        }

        if (!"Pending".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý từ trước (Trạng thái: " + req.getStatus() + ").");
        }

        // 3. Kiểm tra phòng mới được lễ tân gán
        Room newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng được chỉ định với ID: " + newRoomId));

        if (!"Available".equalsIgnoreCase(newRoom.getStatus())) {
            throw new RuntimeException(String.format("Phòng %s hiện tại không trống (trạng thái: %s).",
                    newRoom.getRoomNumber(), newRoom.getStatus()));
        }

        // Kiểm tra phòng chọn có đúng hạng phòng yêu cầu hay không
        Integer newRoomTypeId = Integer.parseInt(req.getNewValue());
        if (!newRoom.getRoomType().getId().equals(newRoomTypeId)) {
            throw new RuntimeException("Phòng được chọn không thuộc hạng phòng mà khách hàng yêu cầu đổi sang.");
        }

        Integer bookingId = req.getBooking().getId();
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
        req.setResolvedAt(Instant.now());
        CustomerRequest saved = customerRequestRepository.save(req);

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional
    public CustomerRequestResponse rejectRoomChangeRequest(Integer requestId, String rejectionReason,
            String staffEmail) {
        log.info("[RoomChangeRequest] Nhân viên '{}' từ chối yêu cầu đổi phòng ID={}, Lý do: {}", staffEmail, requestId,
                rejectionReason);

        // 1. Validate quyền staff
        validateStaffPermission(staffEmail);

        // 2. Fetch request
        CustomerRequest req = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu chuyển phòng với ID: " + requestId));

        if (!"RoomChange".equalsIgnoreCase(req.getRequestType())) {
            throw new RuntimeException("Yêu cầu này không phải là loại chuyển phòng (RoomChange).");
        }

        if (!"Pending".equalsIgnoreCase(req.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý từ trước.");
        }

        // 3. Cập nhật trạng thái từ chối
        req.setStatus("Rejected");
        req.setRejectionReason(rejectionReason);
        req.setResolvedAt(Instant.now());
        CustomerRequest saved = customerRequestRepository.save(req);

        try {
            String rejectMsg = String.format(
                    "Yêu cầu đổi hạng phòng của bạn cho đơn đặt phòng %s đã bị từ chối. Lý do: %s.",
                    req.getBooking().getBookingReference(), rejectionReason);
            notificationService.sendNotification(req.getBooking().getUser(), "Yêu cầu đổi phòng bị từ chối", rejectMsg,
                    "RoomChange", req.getBooking().getId());
        } catch (Exception e) {
            log.error("Failed to send room change rejection notification: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerRequestResponse> getPendingRoomChangeRequests(String staffEmail) {
        // Validate staff
        validateStaffPermission(staffEmail);

        List<CustomerRequest> pendingRequests = customerRequestRepository
                .findByRequestTypeAndStatusOrderByCreatedAtDesc("RoomChange", "Pending");

        return pendingRequests.stream()
                .map(this::mapToCustomerRequestResponse)
                .toList();
    }

    private CustomerRequestResponse mapToCustomerRequestResponse(CustomerRequest req) {
        return CustomerRequestResponse.builder()
                .requestId(req.getId())
                .bookingId(req.getBooking().getId())
                .bookingReference(req.getBooking().getBookingReference())
                .requestType(req.getRequestType())
                .description(req.getDescription())
                .oldValue(req.getOldValue())
                .newValue(req.getNewValue())
                .status(req.getStatus())
                .resolvedAt(req.getResolvedAt())
                .rejectionReason(req.getRejectionReason())
                .createdAt(req.getCreatedAt())
                .build();
    }

    private Instant toInstant(LocalDate date) {
        return date.atStartOfDay(HOTEL_ZONE).toInstant();
    }

    private void assertAvailability(RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate, int quantity) {
        int totalRooms = Math.toIntExact(roomRepository.countByRoomType_IdAndStatus(
                roomType.getId(), ROOM_STATUS_AVAILABLE));

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

            long bookedRooms = bookingDetailRepository.sumBookedQuantity(
                    roomType.getId(),
                    periodStart,
                    periodEnd,
                    List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.STAYING,
                            BookingStatus.PAID, BookingStatus.PARTIALLY_PAID),
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

    @Override
    @Transactional
    public CustomerRequestResponse submitStayExtensionRequest(CustomerStayExtensionRequest request,
            String customerEmail) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (!booking.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("Bạn không có quyền gửi yêu cầu cho đơn đặt phòng này");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Chỉ đơn đặt phòng đang lưu trú (Checked In) mới được phép yêu cầu gia hạn");
        }

        BookingDetail detail = bookingDetailRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        LocalDate currentCheckOut = detail.getExpectedCheckOut().atZone(HOTEL_ZONE).toLocalDate();
        LocalDate newCheckOut = LocalDate.parse(request.getNewCheckOutDate().trim());

        if (newCheckOut.isBefore(currentCheckOut) || newCheckOut.equals(currentCheckOut)) {
            throw new RuntimeException("Ngày trả phòng mới phải sau ngày trả phòng hiện tại (" + currentCheckOut + ")");
        }

        // 1. Kiểm tra gia hạn chính phòng vật lý hiện tại của khách (nếu đã có lịch đặt trùng thì báo lỗi cụ thể)
        if (detail.getRoom() != null) {
            boolean hasOverlap = bookingDetailRepository.existsOverlappingBookingForRoom(
                    detail.getRoom().getId(),
                    booking.getId(),
                    detail.getExpectedCheckOut(),
                    toInstant(newCheckOut)
            );
            if (hasOverlap) {
                throw new RuntimeException("Phòng " + detail.getRoom().getRoomNumber()
                        + " đã có khách hàng khác đặt trước từ ngày " + currentCheckOut + " đến " + newCheckOut
                        + ". Rất tiếc không thể gia hạn thêm phòng này.");
            }
        }

        // 2. Kiểm tra tính khả dụng của loại phòng trong thời gian gia hạn
        assertAvailability(detail.getRoomType(), currentCheckOut, newCheckOut, detail.getQuantity());

        // Tạo request StayExtension
        CustomerRequest customerrequest = new CustomerRequest();
        customerrequest.setBooking(booking);
        customerrequest.setRequestType("StayExtension");
        customerrequest.setDescription(request.getDescription());
        customerrequest.setOldValue(currentCheckOut.toString());
        customerrequest.setNewValue(newCheckOut.toString());
        customerrequest.setStatus("Pending");
        customerrequest.setCreatedAt(Instant.now());

        CustomerRequest saved = customerRequestRepository.save(customerrequest);

        try {
            String extensionMsg = String.format(
                    "Khách hàng %s yêu cầu gia hạn lưu trú cho đơn đặt phòng %s đến ngày %s. Lý do: %s.",
                    customer.getFullName(), booking.getBookingReference(), newCheckOut, request.getDescription());
            notificationService.sendNotificationToRoles(
                    List.of(com.example.hotelsmartbookingbackend.enums.Role.receptionist,
                            com.example.hotelsmartbookingbackend.enums.Role.manager),
                    "Yêu cầu gia hạn mới",
                    extensionMsg,
                    "StayExtension",
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send stay extension request notification: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional
    public CustomerRequestResponse approveStayExtensionRequest(Integer requestId, String staffEmail) {
        validateStaffPermission(staffEmail);

        CustomerRequest req = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu gia hạn"));

        if (!req.getRequestType().equals("StayExtension")) {
            throw new RuntimeException("Yêu cầu này không phải là yêu cầu gia hạn lưu trú");
        }

        if (!req.getStatus().equals("Pending")) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        Booking booking = req.getBooking();
        BookingDetail detail = bookingDetailRepository.findById(booking.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        LocalDate checkIn = detail.getExpectedCheckIn().atZone(HOTEL_ZONE).toLocalDate();
        LocalDate newCheckOut = LocalDate.parse(req.getNewValue());

        // 1. Kiểm tra availability phòng trống của loại phòng này trước
        assertAvailability(detail.getRoomType(), checkIn, newCheckOut, detail.getQuantity());

        // 2. Đảm bảo gia hạn chính phòng vật lý hiện tại của khách (kiểm tra xem phòng
        // đó có bị đặt trùng trong tương lai không)
        if (detail.getRoom() != null) {
            boolean hasOverlap = bookingDetailRepository.existsOverlappingBookingForRoom(
                    detail.getRoom().getId(),
                    booking.getId(),
                    detail.getExpectedCheckOut(), // kiểm tra từ ngày checkout cũ
                    toInstant(newCheckOut) // đến ngày checkout mới mong muốn
            );
            if (hasOverlap) {
                throw new RuntimeException("Không thể gia hạn chính phòng này vì phòng vật lý "
                        + detail.getRoom().getRoomNumber()
                        + " đã có khách hàng khác đặt trước trong thời gian gia hạn. Vui lòng đổi sang phòng khác.");
            }
        }

        // Tính toán lại tài chính
        long newNights = ChronoUnit.DAYS.between(checkIn, newCheckOut);
        BigDecimal newTotal = detail.getPriceAtBooking()
                .multiply(BigDecimal.valueOf(detail.getQuantity()))
                .multiply(BigDecimal.valueOf(newNights));

        BigDecimal serviceCharge = booking.getServiceChargeAmount() != null ? booking.getServiceChargeAmount() : BigDecimal.ZERO;
        BigDecimal subtotalWithServices = newTotal.add(serviceCharge);
        BigDecimal taxAmount = subtotalWithServices.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmt = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = subtotalWithServices.add(taxAmount).subtract(discountAmt).setScale(2,
                RoundingMode.HALF_UP);

        // Cập nhật booking & detail
        booking.setTotalAmount(newTotal);
        booking.setTaxAmount(taxAmount);
        booking.setFinalAmount(finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount);
        booking.setUpdatedAt(Instant.now());

        detail.setExpectedCheckOut(toInstant(newCheckOut));
        detail.setUpdatedAt(Instant.now());

        // 3. Gia hạn thời hạn của mã khóa phòng (bookingroomaccesses)
        List<BookingRoomAccess> roomAccesses = bookingRoomAccessRepository
                .findByBooking_IdOrderByRoom_RoomNumberAsc(booking.getId());
        if (roomAccesses != null && !roomAccesses.isEmpty()) {
            for (BookingRoomAccess access : roomAccesses) {
                access.setRoomKeyExpiredAt(toInstant(newCheckOut));
                access.setUpdatedAt(Instant.now());
            }
            bookingRoomAccessRepository.saveAll(roomAccesses);
        }

        bookingRepository.save(booking);
        bookingDetailRepository.save(detail);

        // Cập nhật request
        req.setStatus("Approved");
        req.setResolvedAt(Instant.now());
        CustomerRequest saved = customerRequestRepository.save(req);

        try {
            String approveMsg = String.format(
                    "Yêu cầu gia hạn lưu trú cho đơn đặt phòng %s đã được phê duyệt. Ngày trả phòng mới: %s.",
                    booking.getBookingReference(), req.getNewValue());
            notificationService.sendNotification(booking.getUser(), "Gia hạn lưu trú thành công", approveMsg,
                    "StayExtension", booking.getId());
        } catch (Exception e) {
            log.error("Failed to send stay extension approval notification: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional
    public CustomerRequestResponse rejectStayExtensionRequest(Integer requestId, String rejectionReason,
            String staffEmail) {
        validateStaffPermission(staffEmail);

        CustomerRequest req = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu gia hạn"));

        if (!req.getRequestType().equals("StayExtension")) {
            throw new RuntimeException("Yêu cầu này không phải là yêu cầu gia hạn");
        }

        if (!req.getStatus().equals("Pending")) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        req.setStatus("Rejected");
        req.setRejectionReason(rejectionReason);
        req.setResolvedAt(Instant.now());

        CustomerRequest saved = customerRequestRepository.save(req);

        try {
            String rejectMsg = String.format(
                    "Yêu cầu gia hạn lưu trú của bạn cho đơn đặt phòng %s đã bị từ chối. Lý do: %s.",
                    req.getBooking().getBookingReference(), rejectionReason);
            notificationService.sendNotification(req.getBooking().getUser(), "Gia hạn lưu trú bị từ chối", rejectMsg,
                    "StayExtension", req.getBooking().getId());
        } catch (Exception e) {
            log.error("Failed to send stay extension rejection notification: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<CustomerRequestResponse> getPendingStayExtensionRequests(String staffEmail) {
        validateStaffPermission(staffEmail);

        List<CustomerRequest> pendingRequests = customerRequestRepository
                .findByRequestTypeAndStatusOrderByCreatedAtDesc("StayExtension", "Pending");

        return pendingRequests.stream()
                .map(this::mapToCustomerRequestResponse)
                .toList();
    }

    @Override
    @Transactional
    public CustomerRequestResponse submitEarlyCheckOutRequest(CustomerEarlyCheckOutRequest request,
            String customerEmail) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (!booking.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("Bạn không có quyền gửi yêu cầu cho đơn đặt phòng này");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException(
                    "Chỉ đơn đặt phòng đang lưu trú (Checked In) mới được phép yêu cầu check-out sớm");
        }

        BookingDetail detail = bookingDetailRepository.findById(request.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        LocalDate currentCheckOut = detail.getExpectedCheckOut().atZone(HOTEL_ZONE).toLocalDate();
        LocalDate checkIn = detail.getExpectedCheckIn().atZone(HOTEL_ZONE).toLocalDate();
        LocalDate newCheckOut = LocalDate.parse(request.getNewCheckOutDate().trim());

        if (!newCheckOut.isBefore(currentCheckOut)) {
            throw new RuntimeException("Ngày check-out mới phải sớm hơn ngày check-out hiện tại");
        }

        if (newCheckOut.isBefore(checkIn)) {
            throw new RuntimeException("Ngày check-out mới không được trước ngày check-in (" + checkIn + ")");
        }

        // Tạo yêu cầu
        CustomerRequest customerrequest = new CustomerRequest();
        customerrequest.setBooking(booking);
        customerrequest.setRequestType("EarlyCheckOut");
        customerrequest.setDescription(request.getDescription());
        customerrequest.setOldValue(currentCheckOut.toString());
        customerrequest.setNewValue(newCheckOut.toString());
        customerrequest.setStatus("Pending");
        customerrequest.setCreatedAt(Instant.now());

        CustomerRequest saved = customerRequestRepository.save(customerrequest);

        try {
            String earlyMsg = String.format(
                    "Khách hàng %s yêu cầu check-out sớm cho đơn đặt phòng %s vào ngày %s. Lý do: %s.",
                    customer.getFullName(), booking.getBookingReference(), newCheckOut, request.getDescription());
            notificationService.sendNotificationToRoles(
                    List.of(com.example.hotelsmartbookingbackend.enums.Role.receptionist,
                            com.example.hotelsmartbookingbackend.enums.Role.manager),
                    "Yêu cầu trả phòng sớm mới",
                    earlyMsg,
                    "EarlyCheckOut",
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send early early check-out request notification: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional
    public CustomerRequestResponse approveEarlyCheckOutRequest(Integer requestId, String staffEmail) {
        validateStaffPermission(staffEmail);

        CustomerRequest req = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu check-out sớm"));

        if (!req.getRequestType().equals("EarlyCheckOut")) {
            throw new RuntimeException("Yêu cầu này không phải là yêu cầu check-out sớm");
        }

        if (!req.getStatus().equals("Pending")) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        Booking booking = req.getBooking();
        BookingDetail detail = bookingDetailRepository.findById(booking.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        LocalDate checkIn = detail.getExpectedCheckIn().atZone(HOTEL_ZONE).toLocalDate();
        LocalDate newCheckOut = LocalDate.parse(req.getNewValue());

        // Tính toán lại tài chính
        long newNights = ChronoUnit.DAYS.between(checkIn, newCheckOut);
        if (newNights <= 0) {
            newNights = 1; // Tối thiểu tính tiền 1 đêm
        }
        BigDecimal newTotal = detail.getPriceAtBooking()
                .multiply(BigDecimal.valueOf(detail.getQuantity()))
                .multiply(BigDecimal.valueOf(newNights));

        BigDecimal serviceCharge = booking.getServiceChargeAmount() != null ? booking.getServiceChargeAmount() : BigDecimal.ZERO;
        BigDecimal subtotalWithServices = newTotal.add(serviceCharge);
        BigDecimal taxAmount = subtotalWithServices.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmt = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = subtotalWithServices.add(taxAmount).subtract(discountAmt).setScale(2,
                RoundingMode.HALF_UP);

        // Cập nhật booking & detail
        booking.setTotalAmount(newTotal);
        booking.setTaxAmount(taxAmount);
        booking.setFinalAmount(finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount);
        booking.setUpdatedAt(Instant.now());

        detail.setExpectedCheckOut(toInstant(newCheckOut));
        detail.setUpdatedAt(Instant.now());

        // Gia hạn/rút ngắn thời hạn của mã khóa phòng (bookingroomaccesses)
        List<BookingRoomAccess> roomAccesses = bookingRoomAccessRepository
                .findByBooking_IdOrderByRoom_RoomNumberAsc(booking.getId());
        if (roomAccesses != null && !roomAccesses.isEmpty()) {
            for (BookingRoomAccess access : roomAccesses) {
                access.setRoomKeyExpiredAt(toInstant(newCheckOut));
                access.setUpdatedAt(Instant.now());
            }
            bookingRoomAccessRepository.saveAll(roomAccesses);
        }

        bookingRepository.save(booking);
        bookingDetailRepository.save(detail);

        // Cập nhật request
        req.setStatus("Approved");
        req.setResolvedAt(Instant.now());
        CustomerRequest saved = customerRequestRepository.save(req);

        try {
            String approveMsg = String.format(
                    "Yêu cầu trả phòng sớm cho đơn đặt phòng %s đã được phê duyệt. Ngày trả phòng mới của bạn: %s.",
                    booking.getBookingReference(), req.getNewValue());
            notificationService.sendNotification(booking.getUser(), "Trả phòng sớm được phê duyệt", approveMsg,
                    "EarlyCheckOut", booking.getId());
        } catch (Exception e) {
            log.error("Failed to send early checkout approval notification: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional
    public CustomerRequestResponse rejectEarlyCheckOutRequest(Integer requestId, String rejectionReason,
            String staffEmail) {
        validateStaffPermission(staffEmail);

        CustomerRequest req = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (!req.getRequestType().equals("EarlyCheckOut")) {
            throw new RuntimeException("Yêu cầu này không phải là yêu cầu check-out sớm");
        }

        if (!req.getStatus().equals("Pending")) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        req.setStatus("Rejected");
        req.setRejectionReason(rejectionReason);
        req.setResolvedAt(Instant.now());

        CustomerRequest saved = customerRequestRepository.save(req);

        try {
            String rejectMsg = String.format(
                    "Yêu cầu trả phòng sớm của bạn cho đơn đặt phòng %s đã bị từ chối. Lý do: %s.",
                    req.getBooking().getBookingReference(), rejectionReason);
            notificationService.sendNotification(req.getBooking().getUser(), "Trả phòng sớm bị từ chối", rejectMsg,
                    "EarlyCheckOut", req.getBooking().getId());
        } catch (Exception e) {
            log.error("Failed to send early checkout rejection notification: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<CustomerRequestResponse> getPendingEarlyCheckOutRequests(String staffEmail) {
        validateStaffPermission(staffEmail);

        List<CustomerRequest> pendingRequests = customerRequestRepository
                .findByRequestTypeAndStatusOrderByCreatedAtDesc("EarlyCheckOut", "Pending");

        return pendingRequests.stream()
                .map(this::mapToCustomerRequestResponse)
                .toList();
    }

    @Override
    @Transactional
    public CustomerRequestResponse submitServiceRequest(
            com.example.hotelsmartbookingbackend.dto.request.CustomerServiceRequest request, String customerEmail) {
        log.info("[ServiceRequest] Guest='{}' gửi yêu cầu dịch vụ. BookingId={}, serviceId={}, quantity={}",
                customerEmail, request.getBookingId(), request.getServiceId(), request.getQuantity());

        Booking booking = validateBookingIsCheckedIn(request.getBookingId());

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng: " + customerEmail));

        boolean isStaff = "receptionist".equalsIgnoreCase(customer.getRole().name())
                || "manager".equalsIgnoreCase(customer.getRole().name());

        if (!isStaff && !customer.getId().equals(booking.getUser().getId())) {
            throw new RuntimeException("Bạn không có quyền gửi yêu cầu dịch vụ cho đơn đặt phòng này.");
        }

        com.example.hotelsmartbookingbackend.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));

        if (Boolean.FALSE.equals(service.getIsActive())) {
            throw new RuntimeException("Dịch vụ hiện không hoạt động");
        }

        CustomerRequest customerRequest = new CustomerRequest();
        customerRequest.setBooking(booking);
        customerRequest.setRequestType("ServiceRequest");
        String noteStr = request.getNote() != null ? request.getNote().trim() : "";
        customerRequest.setDescription(String.format("Yêu cầu dịch vụ: %s (Số lượng: %d)%s",
                service.getName(), request.getQuantity(), noteStr.isBlank() ? "" : " - Ghi chú: " + noteStr));
        customerRequest.setOldValue(service.getId().toString());
        customerRequest.setNewValue(request.getQuantity().toString() + (noteStr.isBlank() ? "" : "|" + noteStr));
        customerRequest.setStatus("Pending");
        customerRequest.setCreatedAt(Instant.now());

        CustomerRequest saved = customerRequestRepository.save(customerRequest);

        try {
            String serviceReqMsg = String.format("Phòng của khách %s yêu cầu dịch vụ: %s x%d. Ghi chú: %s",
                    customer.getFullName(), service.getName(), request.getQuantity(), noteStr.isBlank() ? "Không có" : noteStr);
            notificationService.sendNotificationToRoles(
                    List.of(com.example.hotelsmartbookingbackend.enums.Role.receptionist,
                            com.example.hotelsmartbookingbackend.enums.Role.manager),
                    "Yêu cầu dịch vụ phòng mới",
                    serviceReqMsg,
                    "ServiceRequest",
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send notification for service request: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerRequestResponse> getPendingServiceRequests(String staffEmail) {
        validateStaffPermission(staffEmail);
        List<CustomerRequest> requests = customerRequestRepository
                .findByRequestTypeInAndStatusOrderByCreatedAtDesc(List.of("ServiceRequest", "SERVICE_REQUEST"), "Pending");
        return requests.stream().map(this::mapToCustomerRequestResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerRequestResponse> getServiceRequestsByBooking(Integer bookingId, String actorEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người dùng"));

        boolean isStaff = "receptionist".equalsIgnoreCase(actor.getRole().name())
                || "manager".equalsIgnoreCase(actor.getRole().name())
                || "admin".equalsIgnoreCase(actor.getRole().name());

        if (!isStaff && !booking.getUser().getId().equals(actor.getId())) {
            throw new RuntimeException("Bạn không có quyền xem dịch vụ của đơn đặt phòng này");
        }

        List<CustomerRequest> requests = customerRequestRepository.findByBooking_IdOrderByCreatedAtDesc(bookingId);
        return requests.stream()
                .filter(r -> "ServiceRequest".equalsIgnoreCase(r.getRequestType()) || "SERVICE_REQUEST".equalsIgnoreCase(r.getRequestType()))
                .map(this::mapToCustomerRequestResponse)
                .toList();
    }

    @Override
    @Transactional
    public CustomerRequestResponse approveServiceRequest(Integer requestId, String staffEmail) {
        User staff = validateStaffPermission(staffEmail);

        CustomerRequest customerRequest = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu dịch vụ"));

        if (!"ServiceRequest".equalsIgnoreCase(customerRequest.getRequestType()) && !"SERVICE_REQUEST".equalsIgnoreCase(customerRequest.getRequestType())) {
            throw new RuntimeException("Yêu cầu này không phải là loại dịch vụ phòng (ServiceRequest).");
        }

        if (!"Pending".equalsIgnoreCase(customerRequest.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý từ trước.");
        }

        Integer serviceId = Integer.parseInt(customerRequest.getOldValue());
        String[] newParts = customerRequest.getNewValue().split("\\|", 2);
        Integer quantity = Integer.parseInt(newParts[0]);
        String note = newParts.length > 1 ? newParts[1] : "";

        com.example.hotelsmartbookingbackend.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));

        Booking booking = customerRequest.getBooking();
        BigDecimal unitPrice = service.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        // Tự động tạo bản ghi BookingService để cộng dồn tiền vào Hóa đơn khi Checkout
        com.example.hotelsmartbookingbackend.entity.BookingService usage = new com.example.hotelsmartbookingbackend.entity.BookingService();
        usage.setBooking(booking);
        usage.setService(service);
        usage.setImplementedBy(staff);
        usage.setQuantity(quantity);
        usage.setUnitPrice(unitPrice);
        usage.setTotalPrice(totalPrice);
        usage.setImplementedAt(Instant.now());
        usage.setNote("Yêu cầu dịch vụ: " + service.getName() + (note.isBlank() ? "" : " (" + note + ")"));
        usage.setStatus("Active");
        bookingServiceRepository.save(usage);

        // Cập nhật phụ thu dịch vụ, thuế VAT và tổng cộng cho Booking
        BigDecimal currentServiceCharge = booking.getServiceChargeAmount() != null ? booking.getServiceChargeAmount() : BigDecimal.ZERO;
        BigDecimal currentTax = booking.getTaxAmount() != null ? booking.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal currentFinal = booking.getFinalAmount() != null ? booking.getFinalAmount() : (booking.getTotalAmount() != null ? booking.getTotalAmount() : BigDecimal.ZERO);

        BigDecimal serviceTax = totalPrice.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP);
        booking.setServiceChargeAmount(currentServiceCharge.add(totalPrice));
        booking.setTaxAmount(currentTax.add(serviceTax));
        booking.setFinalAmount(currentFinal.add(totalPrice).add(serviceTax));

        if (booking.getPaidAmount() != null && booking.getPaidAmount().compareTo(booking.getFinalAmount()) < 0) {
            if (booking.getStatus() == BookingStatus.PAID) {
                booking.setStatus(BookingStatus.PARTIALLY_PAID);
            }
        }

        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);

        customerRequest.setStatus("Approved");
        customerRequest.setResolvedAt(Instant.now());
        CustomerRequest saved = customerRequestRepository.save(customerRequest);

        try {
            String msg = String.format("Yêu cầu dịch vụ '%s' (Số lượng: %d) của bạn đã được phê duyệt và cộng vào hóa đơn checkout.",
                    service.getName(), quantity);
            notificationService.sendNotification(booking.getUser(), "Dịch vụ phòng được tiếp nhận", msg, "ServiceRequest", booking.getId());
        } catch (Exception e) {
            log.error("Failed to send approval notification for service request: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

    @Override
    @Transactional
    public CustomerRequestResponse rejectServiceRequest(Integer requestId, String rejectionReason, String staffEmail) {
        validateStaffPermission(staffEmail);

        CustomerRequest customerRequest = customerRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu dịch vụ"));

        if (!"ServiceRequest".equalsIgnoreCase(customerRequest.getRequestType()) && !"SERVICE_REQUEST".equalsIgnoreCase(customerRequest.getRequestType())) {
            throw new RuntimeException("Yêu cầu này không phải là loại dịch vụ phòng.");
        }

        if (!"Pending".equalsIgnoreCase(customerRequest.getStatus())) {
            throw new RuntimeException("Yêu cầu này đã được xử lý từ trước.");
        }

        String cleanReason = rejectionReason != null && !rejectionReason.isBlank()
                ? rejectionReason.replace("\"", "").trim()
                : "Không thể phục vụ tại thời điểm này";

        customerRequest.setStatus("Rejected");
        customerRequest.setRejectionReason(cleanReason);
        customerRequest.setResolvedAt(Instant.now());
        CustomerRequest saved = customerRequestRepository.save(customerRequest);

        try {
            String msg = String.format("Yêu cầu dịch vụ của bạn không thể thực hiện. Lý do: %s", cleanReason);
            notificationService.sendNotification(customerRequest.getBooking().getUser(), "Yêu cầu dịch vụ bị từ chối", msg, "ServiceRequest", customerRequest.getBooking().getId());
        } catch (Exception e) {
            log.error("Failed to send rejection notification for service request: ", e);
        }

        return mapToCustomerRequestResponse(saved);
    }

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

        static FinancialResult sameType(BigDecimal currentTotal, BigDecimal currentFinal) {
            return new FinancialResult(currentTotal, currentFinal, BigDecimal.ZERO, false);
        }

        static FinancialResult differentType(BigDecimal newTotal, BigDecimal newFinal,
                                              BigDecimal difference) {
            return new FinancialResult(newTotal, newFinal, difference, true);
        }

        BigDecimal getNewTotalAmount() { return newTotalAmount; }
        BigDecimal getNewFinalAmount() { return newFinalAmount; }
        BigDecimal getPriceDifference() { return priceDifference; }
        boolean isDifferentType() { return differentType; }
    }
}
