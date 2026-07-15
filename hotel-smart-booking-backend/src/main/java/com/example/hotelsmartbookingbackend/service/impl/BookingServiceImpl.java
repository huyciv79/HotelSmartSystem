package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.AddServiceRequest;
import com.example.hotelsmartbookingbackend.dto.request.CancelBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.UpdateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.WalkInBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceReadinessResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceVerificationResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycIdentitySummaryResponse;
import com.example.hotelsmartbookingbackend.dto.response.InvoiceResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.QrTokenResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomAccessResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.entity.BookingRoomAccess;
import com.example.hotelsmartbookingbackend.entity.BookingDetail;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.entity.RoomType;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.entity.Payment;
import com.example.hotelsmartbookingbackend.entity.CustomerRequest;
import com.example.hotelsmartbookingbackend.enums.Role;

import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.BookingRoomAccessRepository;
import com.example.hotelsmartbookingbackend.repository.BookingDetailRepository;
import com.example.hotelsmartbookingbackend.repository.EkycProfileRepository;
import com.example.hotelsmartbookingbackend.repository.FaceEmbeddingRepository;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomTypeRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.repository.PaymentRepository;
import com.example.hotelsmartbookingbackend.repository.BookingServiceRepository;
import com.example.hotelsmartbookingbackend.repository.CustomerRequestRepository;
import com.example.hotelsmartbookingbackend.repository.ServiceRepository;
import com.example.hotelsmartbookingbackend.service.BookingService;
import com.example.hotelsmartbookingbackend.service.EmailService;
import com.example.hotelsmartbookingbackend.service.WebSocketService;

import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import com.example.hotelsmartbookingbackend.specification.BookingSpecification;
import com.example.hotelsmartbookingbackend.dto.request.BookingFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final String AVAILABLE_ROOM_STATUS = "Available";
    private static final String ROOM_STATUS_OCCUPIED = "Occupied";
    private static final String ACTIVE_STATUS = "Active";
    private static final String DETAIL_STATUS_ACTIVE = "Active";
    private static final String ROOM_KEY_STATUS_ACTIVE = "Active";
    private static final String ROOM_KEY_STATUS_EXPIRED = "Expired";
    private static final int DEFAULT_SINGLE_BOOKING_QUANTITY = 1;
    private static final int QR_TOKEN_RANDOM_BYTES = 32;

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final List<BookingStatus> INVENTORY_HOLDING_BOOKING_STATUSES = List.of(
            BookingStatus.CONFIRMED,
            BookingStatus.CHECKED_IN,
            BookingStatus.STAYING,
            BookingStatus.PAID,
            BookingStatus.PARTIALLY_PAID);

    private static final List<String> INVENTORY_HOLDING_DETAIL_STATUSES = List.of("Active");
    private static final DateTimeFormatter BOOKING_REFERENCE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(HOTEL_ZONE);
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BookingRepository bookingRepository;
    private final BookingDetailRepository bookingDetailRepository;
    private final BookingRoomAccessRepository bookingRoomAccessRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final EkycProfileRepository ekycProfileRepository;
    private final FaceEmbeddingRepository faceEmbeddingRepository;
    private final AesEncryptionService aesEncryptionService;
    private final SupabaseStorageService supabaseStorageService;
    private final WebClient webClient;
    private final PaymentRepository paymentRepository;
    private final BookingServiceRepository bookingServiceRepository;
    private final WebSocketService webSocketService;
    private final ServiceRepository serviceRepository;
    private final EmailService emailService;
    private final CustomerRequestRepository customerRequestRepository;
    private final com.example.hotelsmartbookingbackend.service.NotificationService notificationService;

    @Value("${ai.service.face-verify-url:http://localhost:8000/api/v1/face/verify}")
    private String aiFaceVerifyUrl;

    @Value("${ai.service.face-readiness-url:http://localhost:8000/api/v1/face/check-readiness}")
    private String aiFaceReadinessUrl;

    @Value("${booking.qr-token-ttl-minutes:15}")
    private long qrTokenTtlMinutes;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String customerEmail) {
        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        validateGuestCounts(request.getNumberOfAdults(), request.getNumberOfChildren());

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Integer roomTypeId = request.getRoomTypeId();
        LocalDate checkInDate = request.getCheckInDate();
        LocalDate checkOutDate = request.getCheckOutDate();
        int quantity = DEFAULT_SINGLE_BOOKING_QUANTITY;
        int numberOfAdults = request.getNumberOfAdults();
        int numberOfChildren = request.getNumberOfChildren();
        String bookingType = "Online";
        String checkInMethod = request.getCheckInMethod() != null ? request.getCheckInMethod().trim() : "Manual";
        String specialRequests = request.getSpecialRequests();

        String normalizedCheckInMethod = normalizeCheckInMethod(checkInMethod);
        if ("FaceID".equalsIgnoreCase(normalizedCheckInMethod)) {
            boolean ekycVerified = ekycProfileRepository.existsVerifiedByUserid(customer);
            boolean faceRegistered = faceEmbeddingRepository
                    .findEmbeddingTextByUserId(customer.getId())
                    .filter(embedding -> !embedding.isBlank())
                    .isPresent();

            if (!ekycVerified || !faceRegistered) {
                throw new RuntimeException(
                        "Bạn phải hoàn thành đăng ký eKYC và khuôn mặt trước khi chọn check-in bằng FaceID");
            }
        }

        RoomType roomtype = roomTypeRepository.findByIdForUpdate(roomTypeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));

        if (!ACTIVE_STATUS.equalsIgnoreCase(roomtype.getStatus())) {
            throw new RuntimeException("Loại phòng hiện không hoạt động");
        }

        assertCapacity(roomtype, quantity, numberOfAdults, numberOfChildren);
        assertAvailability(roomtype, checkInDate, checkOutDate, quantity);

        Instant now = Instant.now();
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        BigDecimal totalAmount = roomtype.getBasePrice()
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(nights));

        BigDecimal taxAmount = totalAmount.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal finalAmount = totalAmount.add(taxAmount).setScale(2, java.math.RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setBookingReference(generateBookingReference(now));
        booking.setBookingType(bookingType);
        booking.setCheckInMethod(normalizedCheckInMethod);
        booking.setTotalAmount(totalAmount);
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setDepositAmount(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTaxAmount(taxAmount);
        booking.setServiceChargeAmount(BigDecimal.ZERO);
        booking.setFinalAmount(finalAmount);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSpecialRequests(specialRequests);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);

        Booking savedBooking = bookingRepository.save(booking);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(savedBooking);
        detail.setRoomType(roomtype);
        detail.setQuantity(quantity);
        detail.setExpectedCheckIn(toInstant(checkInDate));
        detail.setExpectedCheckOut(toInstant(checkOutDate));
        detail.setPriceAtBooking(roomtype.getBasePrice());
        detail.setNumberOfAdults(numberOfAdults);
        detail.setNumberOfChildren(numberOfChildren);
        detail.setStatus(DETAIL_STATUS_ACTIVE);
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);

        bookingDetailRepository.save(detail);

        try {
            // Gửi thông báo cho khách hàng
            String customerMsg = String.format("Đặt phòng thành công! Mã đơn của bạn là %s. Phương thức check-in: %s.",
                    savedBooking.getBookingReference(), savedBooking.getCheckInMethod());
            notificationService.sendNotification(customer, "Đặt phòng thành công", customerMsg, "Booking",
                    savedBooking.getId());

            // Gửi thông báo cho nhân viên lễ tân và quản lý
            String staffMsg = String.format("Đơn đặt phòng mới %s từ khách hàng %s (%s).",
                    savedBooking.getBookingReference(), customer.getFullName(), savedBooking.getBookingType());
            notificationService.sendNotificationToRoles(
                    List.of(Role.receptionist, Role.manager),
                    "Đơn đặt phòng mới",
                    staffMsg,
                    "Booking",
                    savedBooking.getId());
        } catch (Exception e) {
            log.error("Failed to send booking notifications: ", e);
        }

        return mapToResponse(savedBooking, detail, roomtype);
    }

    @Override
    @Transactional
    public BookingResponse createGroupBooking(CreateGroupBookingRequest request, String customerEmail) {
        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        validateGroupBookingRequest(request);

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Integer roomTypeId = request.getRoomTypeId();
        LocalDate checkInDate = request.getCheckInDate();
        LocalDate checkOutDate = request.getCheckOutDate();
        int quantity = request.getQuantity();
        int numberOfAdults = request.getNumberOfAdults();
        int numberOfChildren = request.getNumberOfChildren();
        String bookingType = "Group";
        String checkInMethod = request.getCheckInMethod() != null ? request.getCheckInMethod().trim() : "Manual";
        String specialRequests = request.getSpecialRequests();

        String normalizedCheckInMethod = normalizeCheckInMethod(checkInMethod);
        if ("FaceID".equalsIgnoreCase(normalizedCheckInMethod)) {
            boolean ekycVerified = ekycProfileRepository.existsVerifiedByUserid(customer);
            boolean faceRegistered = faceEmbeddingRepository
                    .findEmbeddingTextByUserId(customer.getId())
                    .filter(embedding -> !embedding.isBlank())
                    .isPresent();

            if (!ekycVerified || !faceRegistered) {
                throw new RuntimeException(
                        "Bạn phải hoàn thành đăng ký eKYC và khuôn mặt trước khi chọn check-in bằng FaceID");
            }
        }

        RoomType roomtype = roomTypeRepository.findByIdForUpdate(roomTypeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));

        if (!ACTIVE_STATUS.equalsIgnoreCase(roomtype.getStatus())) {
            throw new RuntimeException("Loại phòng hiện không hoạt động");
        }

        assertCapacity(roomtype, quantity, numberOfAdults, numberOfChildren);
        assertAvailability(roomtype, checkInDate, checkOutDate, quantity);

        Instant now = Instant.now();
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        BigDecimal totalAmount = roomtype.getBasePrice()
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(nights));

        BigDecimal taxAmount = totalAmount.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal finalAmount = totalAmount.add(taxAmount).setScale(2, java.math.RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setBookingReference(generateBookingReference(now));
        booking.setBookingType(bookingType);
        booking.setCheckInMethod(normalizedCheckInMethod);
        booking.setTotalAmount(totalAmount);
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setDepositAmount(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTaxAmount(taxAmount);
        booking.setServiceChargeAmount(BigDecimal.ZERO);
        booking.setFinalAmount(finalAmount);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSpecialRequests(specialRequests);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);

        Booking savedBooking = bookingRepository.save(booking);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(savedBooking);
        detail.setRoomType(roomtype);
        detail.setQuantity(quantity);
        detail.setExpectedCheckIn(toInstant(checkInDate));
        detail.setExpectedCheckOut(toInstant(checkOutDate));
        detail.setPriceAtBooking(roomtype.getBasePrice());
        detail.setNumberOfAdults(numberOfAdults);
        detail.setNumberOfChildren(numberOfChildren);
        detail.setStatus(DETAIL_STATUS_ACTIVE);
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);

        bookingDetailRepository.save(detail);

        try {
            // Gửi thông báo cho khách hàng
            String customerMsg = String.format("Đặt phòng nhóm thành công! Mã đơn của bạn là %s. Phương thức check-in: %s.",
                    savedBooking.getBookingReference(), savedBooking.getCheckInMethod());
            notificationService.sendNotification(customer, "Đặt phòng nhóm thành công", customerMsg, "Booking",
                    savedBooking.getId());

            // Gửi thông báo cho nhân viên lễ tân và quản lý
            String staffMsg = String.format("Đơn đặt phòng mới %s từ khách hàng %s (%s).",
                    savedBooking.getBookingReference(), customer.getFullName(), savedBooking.getBookingType());
            notificationService.sendNotificationToRoles(
                    List.of(Role.receptionist, Role.manager),
                    "Đơn đặt phòng mới",
                    staffMsg,
                    "Booking",
                    savedBooking.getId());
        } catch (Exception e) {
            log.error("Failed to send booking notifications: ", e);
        }

        return mapToResponse(savedBooking, detail, roomtype);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingHistoryResponse> getBookingHistory(String customerEmail) {
        userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return bookingDetailRepository.findBookingHistory(customerEmail).stream()
                .map(detail -> mapToHistoryResponse(detail, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingDetail(Integer bookingId, String customerEmail) {
        BookingDetail detail = bookingDetailRepository.findBookingDetail(bookingId, customerEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy đặt phòng hoặc bạn không có quyền xem đặt phòng này"));

        return mapToResponse(detail.getBooking(), detail, detail.getRoomType());
    }

    private void validateDates(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            throw new RuntimeException("Vui lòng chọn ngày nhận phòng và ngày trả phòng");
        }

        LocalDate today = LocalDate.now(HOTEL_ZONE);
        if (checkInDate.isBefore(today)) {
            throw new RuntimeException("Ngày nhận phòng không được là ngày trong quá khứ");
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new RuntimeException("Ngày trả phòng phải sau ngày nhận phòng");
        }
    }

    private void validateGroupBookingRequest(CreateGroupBookingRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng phòng phải lớn hơn 0");
        }

        if (request.getQuantity() < 2) {
            throw new RuntimeException("Số lượng phòng cho đặt nhóm phải ít nhất là 2");
        }

        validateGuestCounts(request.getNumberOfAdults(), request.getNumberOfChildren());
    }

    private void validateGuestCounts(Integer numberOfAdults, Integer numberOfChildren) {
        if (numberOfAdults == null || numberOfAdults <= 0) {
            throw new RuntimeException("Số người lớn phải lớn hơn 0");
        }

        if (numberOfChildren == null || numberOfChildren < 0) {
            throw new RuntimeException("Số trẻ em không được âm");
        }
    }

    private void assertCapacity(RoomType roomtype, int quantity, int numberOfAdults, int numberOfChildren) {
        int adultCapacity = safeInt(roomtype.getAdultCapacity()) * quantity;
        int childCapacity = safeInt(roomtype.getChildCapacity()) * quantity;

        if (numberOfAdults > adultCapacity) {
            throw new RuntimeException("Số người lớn vượt quá sức chứa của loại phòng");
        }

        if (numberOfChildren > childCapacity) {
            throw new RuntimeException("Số trẻ em vượt quá sức chứa của loại phòng");
        }
    }

    private void assertAvailability(RoomType roomtype, LocalDate checkInDate, LocalDate checkOutDate, int quantity) {
        int totalRooms = Math.toIntExact(roomRepository.countByRoomType_IdAndStatus(
                roomtype.getId(), AVAILABLE_ROOM_STATUS));

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
                    roomtype.getId(),
                    periodStart,
                    periodEnd,
                    INVENTORY_HOLDING_BOOKING_STATUSES,
                    INVENTORY_HOLDING_DETAIL_STATUSES);

            int availableRooms = totalRooms - Math.toIntExact(bookedRooms);
            if (availableRooms < quantity) {
                throw new RuntimeException("Không đủ phòng trống vào ngày "
                        + stayDate.format(DISPLAY_DATE_FORMAT)
                        + ". Số phòng còn trống: " + availableRooms);
            }

            stayDate = stayDate.plusDays(1);
        }
    }

    private String generateBookingReference(Instant now) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String randomSuffix = String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
            String reference = "BK" + BOOKING_REFERENCE_TIME_FORMAT.format(now) + randomSuffix;
            if (!bookingRepository.existsByBookingReference(reference)) {
                return reference;
            }
        }
        throw new RuntimeException("Không thể tạo mã đặt phòng");
    }

    private BookingResponse mapToResponse(Booking booking, BookingDetail detail, RoomType roomtype) {
        LocalDate checkInDate = toLocalDate(detail.getExpectedCheckIn());
        LocalDate checkOutDate = toLocalDate(detail.getExpectedCheckOut());
        Room assignedRoom = detail.getRoom();
        boolean roomKeyUsable = isRoomKeyUsable(booking, detail);
        List<RoomAccessResponse> roomAccesses = mapRoomAccesses(booking, detail, true);

        // Lấy thông tin các yêu cầu đổi phòng / gia hạn / checkout sớm đang chờ phê
        // duyệt từ database
        List<CustomerRequest> requests = customerRequestRepository.findByBooking_Id(booking.getId());
        boolean isRoomChangePending = requests != null && requests.stream()
                .anyMatch(r -> "RoomChange".equalsIgnoreCase(r.getRequestType())
                        && "Pending".equalsIgnoreCase(r.getStatus()));
        boolean isStayExtensionPending = requests != null && requests.stream()
                .anyMatch(r -> "StayExtension".equalsIgnoreCase(r.getRequestType())
                        && "Pending".equalsIgnoreCase(r.getStatus()));
        boolean isEarlyCheckOutPending = requests != null && requests.stream()
                .anyMatch(r -> "EarlyCheckOut".equalsIgnoreCase(r.getRequestType())
                        && "Pending".equalsIgnoreCase(r.getStatus()));

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .bookingType(booking.getBookingType())
                .checkInMethod(booking.getCheckInMethod())
                .roomTypeId(roomtype.getId())
                .roomTypeName(roomtype.getName())
                .quantity(detail.getQuantity())
                .numberOfAdults(detail.getNumberOfAdults())
                .numberOfChildren(detail.getNumberOfChildren())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .nights(ChronoUnit.DAYS.between(checkInDate, checkOutDate))
                .totalAmount(booking.getTotalAmount())
                .finalAmount(booking.getFinalAmount())
                .paidAmount(booking.getPaidAmount())
                .depositAmount(booking.getDepositAmount())
                .status(booking.getStatus())
                .specialRequests(booking.getSpecialRequests())
                .actualCheckIn(detail.getActualCheckIn())
                .actualCheckOut(detail.getActualCheckOut())
                .roomId(assignedRoom != null ? assignedRoom.getId() : null)
                .roomNumber(assignedRoom != null ? assignedRoom.getRoomNumber() : null)
                .roomPassword(roomKeyUsable ? detail.getRoomKeyAccess() : null)
                .roomKeyStatus(detail.getRoomKeyStatus())
                .roomKeyGeneratedAt(detail.getRoomKeyGeneratedAt())
                .roomKeyExpiresAt(detail.getRoomKeyExpiredAt())
                .roomAccesses(roomAccesses)
                .isRoomChangePending(isRoomChangePending)
                .isStayExtensionPending(isStayExtensionPending)
                .isEarlyCheckOutPending(isEarlyCheckOutPending)
                .ekycIdentity(mapEkycIdentitySummary(booking.getUser()))
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private EkycIdentitySummaryResponse mapEkycIdentitySummary(User customer) {
        if (customer == null) {
            return null;
        }

        return ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(customer)
                .map(profile -> {
                    String idNumber = decryptOrNull(profile.getIdCardNumber());
                    String fullName = decryptOrNull(profile.getFullName());
                    String dateOfBirth = decryptOrNull(profile.getDateOfBirth());
                    String gender = decryptOrNull(profile.getGender());
                    String hometown = decryptOrNull(profile.getHomeTown());
                    String provinceName = decryptOrNull(profile.getProvinceName());

                    return EkycIdentitySummaryResponse.builder()
                            .status(profile.getStatus())
                            .fullName(fullName != null ? fullName : customer.getFullName())
                            .idNumber(maskIdNumber(idNumber))
                            .dateOfBirth(dateOfBirth)
                            .gender(gender)
                            .hometown(hometown != null ? hometown : provinceName)
                            .provinceCode(profile.getProvinceCode())
                            .provinceName(provinceName)
                            .verifiedAt(profile.getVerifiedAt())
                            .frontImage(signedUrlOrNull(profile.getFrontImage()))
                            .backImage(signedUrlOrNull(profile.getBackImage()))
                            .faceImage(signedUrlOrNull(profile.getFaceImage()))
                            .build();
                })
                .orElse(null);
    }

    private String decryptOrNull(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return null;
        }
        try {
            return aesEncryptionService.decrypt(encryptedValue);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String signedUrlOrNull(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) {
            return null;
        }
        try {
            return supabaseStorageService.getSignedUrl(pathOrUrl);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.isBlank()) {
            return null;
        }
        if (idNumber.length() <= 5) {
            return "*".repeat(idNumber.length());
        }
        return idNumber.substring(0, 2)
                + "*".repeat(Math.max(0, idNumber.length() - 5))
                + idNumber.substring(idNumber.length() - 3);
    }

    private BookingHistoryResponse mapToHistoryResponse(
            BookingDetail detail,
            boolean includeRoomPassword) {
        Booking booking = detail.getBooking();
        RoomType roomtype = detail.getRoomType();
        Room assignedRoom = detail.getRoom();
        LocalDate checkInDate = toLocalDate(detail.getExpectedCheckIn());
        LocalDate checkOutDate = toLocalDate(detail.getExpectedCheckOut());
        boolean roomKeyUsable = includeRoomPassword && isRoomKeyUsable(booking, detail);
        List<RoomAccessResponse> roomAccesses = mapRoomAccesses(
                booking,
                detail,
                includeRoomPassword);

        return BookingHistoryResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingReference())
                .bookingDate(booking.getCreatedAt())
                .checkInMethod(booking.getCheckInMethod())
                .roomTypeId(roomtype.getId())
                .roomType(roomtype.getName())
                .quantity(detail.getQuantity())
                .numberOfAdults(detail.getNumberOfAdults())
                .numberOfChildren(detail.getNumberOfChildren())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .nights(ChronoUnit.DAYS.between(checkInDate, checkOutDate))
                .totalAmount(booking.getFinalAmount())
                .status(booking.getStatus())
                .guestName(booking.getUser() != null ? booking.getUser().getFullName() : "Khách hàng Elysian")
                .guestEmail(booking.getUser() != null ? booking.getUser().getEmail() : "")
                .actualCheckIn(detail.getActualCheckIn())
                .actualCheckOut(detail.getActualCheckOut())
                .roomNumber(assignedRoom != null ? assignedRoom.getRoomNumber() : null)
                .roomPassword(roomKeyUsable ? detail.getRoomKeyAccess() : null)
                .roomKeyStatus(detail.getRoomKeyStatus())
                .roomKeyExpiresAt(detail.getRoomKeyExpiredAt())
                .roomAccesses(roomAccesses)
                .ekycIdentity(includeRoomPassword && isFaceIdMethod(booking.getCheckInMethod())
                        ? mapEkycIdentitySummary(booking.getUser())
                        : null)
                .paidAmount(booking.getPaidAmount())
                .depositAmount(booking.getDepositAmount())
                .serviceChargeAmount(booking.getServiceChargeAmount())
                .taxAmount(booking.getTaxAmount())
                .discountAmount(booking.getDiscountAmount())
                .finalAmount(booking.getFinalAmount())
                .build();
    }

    @Override
    @Transactional
    public BookingResponse performCheckIn(Integer bookingId, String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name())
                && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.PAID
                && booking.getStatus() != BookingStatus.PARTIALLY_PAID) {
            throw new RuntimeException("Đơn đặt phòng không ở trạng thái có thể nhận phòng");
        }

        BookingDetail detail = bookingDetailRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        return completeCheckIn(booking, detail, staff);
    }

    @Override
    @Transactional
    public BookingResponse performFaceCheckIn(
            Integer bookingId,
            MultipartFile selfieImage,
            MultipartFile challengeImage,
            MultipartFile challengeImage2,
            MultipartFile challengeImage3,
            String challengeDirection,
            String actorEmail) {
        validateSelfie(selfieImage);
        validateSelfie(challengeImage);
        validateSelfie(challengeImage2);
        validateSelfie(challengeImage3);
        String normalizedChallengeDirection = challengeDirection == null
                ? ""
                : challengeDirection.trim().toLowerCase();
        if (!normalizedChallengeDirection.equals("left")
                && !normalizedChallengeDirection.equals("right")) {
            throw new RuntimeException("Hướng thử thách FaceID không hợp lệ");
        }

        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        boolean isManager = "manager".equalsIgnoreCase(actor.getRole().name());

        if (!isManager) {
            throw new RuntimeException("Chỉ Manager mới được check-in bằng FaceID");
        }

        BookingDetail detail = bookingDetailRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));
        Booking booking = detail.getBooking();
        User customer = booking.getUser();
        if (customer == null) {
            throw new RuntimeException("Booking chưa được liên kết với tài khoản khách hàng");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.PAID
                && booking.getStatus() != BookingStatus.PARTIALLY_PAID) {
            throw new RuntimeException("Đơn đặt phòng không ở trạng thái có thể nhận phòng");
        }
        if (!"Face Recognition".equalsIgnoreCase(booking.getCheckInMethod())
                && !"FaceID".equalsIgnoreCase(booking.getCheckInMethod())) {
            throw new RuntimeException("Đơn đặt phòng này không sử dụng phương thức FaceID");
        }
        validateCheckInDateWindow(detail);

        if (!ekycProfileRepository.existsVerifiedByUserid(customer)) {
            throw new RuntimeException(
                    "Bạn chưa hoàn thành đăng ký eKYC nên chưa thể check-in bằng FaceID");
        }

        String registeredEmbedding = faceEmbeddingRepository
                .findEmbeddingTextByUserId(customer.getId())
                .filter(embedding -> !embedding.isBlank())
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy dữ liệu khuôn mặt đã đăng ký"));

        AiFaceVerificationResponse verification = callFaceVerificationService(
                registeredEmbedding,
                selfieImage,
                challengeImage,
                challengeImage2,
                challengeImage3,
                normalizedChallengeDirection);
        if (!Boolean.TRUE.equals(verification.getLivenessPassed())) {
            String livenessMessage = verification.getMessage() != null
                    && !verification.getMessage().isBlank()
                            ? verification.getMessage()
                            : "Không vượt qua kiểm tra liveness";
            if (Boolean.FALSE.equals(verification.getActiveLivenessPassed())) {
                throw new RuntimeException(livenessMessage);
            }
            throw new RuntimeException(livenessMessage
                    + " Vui lòng dùng khuôn mặt thật trước camera, không dùng ảnh hoặc màn hình");
        }
        if (!Boolean.TRUE.equals(verification.getVerified())
                || !Boolean.TRUE.equals(verification.getMatched())) {
            String mismatchMessage = verification.getMessage() != null
                    && !verification.getMessage().isBlank()
                            ? verification.getMessage()
                            : "Check-in bị từ chối: khuôn mặt hiện tại không khớp với khuôn mặt đã đăng ký eKYC.";
            throw new RuntimeException(mismatchMessage);
        }

        return completeCheckIn(booking, detail, actor);
    }

    @Override
    @Transactional
    public QrTokenResponse generateQrCheckInToken(Integer bookingId, String customerEmail) {
        BookingDetail detail = bookingDetailRepository.findBookingDetail(bookingId, customerEmail)
                .orElseThrow(() -> new RuntimeException("Khong tim thay booking QR Code cua ban"));
        Booking booking = detail.getBooking();
        User customer = booking.getUser();
        if (customer == null) {
            throw new RuntimeException("Booking chua duoc lien ket voi tai khoan khach hang");
        }

        validateQrBookingForCheckIn(booking, detail, customer);

        Instant now = Instant.now();
        if (hasActiveQrToken(detail, now)) {
            String existingToken = detail.getQrCodeValue();
            Instant existingExpiresAt = detail.getQrCodeExpiredAt();
            sendQrCheckInEmailSafely(customer, booking, detail, existingToken, existingExpiresAt);
            return buildQrTokenResponse(booking, detail, existingToken, existingExpiresAt);
        }

        Instant expiresAt = calculateQrTokenExpiry(now, detail);
        String token = generateUniqueQrToken();

        detail.setQrCodeValue(token);
        detail.setQrCodeGeneratedAt(now);
        detail.setQrCodeExpiredAt(expiresAt);
        detail.setUpdatedAt(now);
        bookingDetailRepository.save(detail);

        sendQrCheckInEmailSafely(customer, booking, detail, token, expiresAt);

        return buildQrTokenResponse(booking, detail, token, expiresAt);
    }

    private QrTokenResponse buildQrTokenResponse(
            Booking booking,
            BookingDetail detail,
            String token,
            Instant expiresAt) {
        return QrTokenResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .token(token)
                .qrPayload(token)
                .generatedAt(detail.getQrCodeGeneratedAt())
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    @Transactional
    public BookingResponse performQrCheckIn(String qrToken, String actorEmail) {
        if (qrToken == null || qrToken.isBlank()) {
            throw new RuntimeException("Ma QR check-in khong hop le");
        }

        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nguoi dung"));

        String token = qrToken.trim();
        BookingDetail detail = bookingDetailRepository.findByQrCodeValueForUpdate(token)
                .orElseThrow(() -> new RuntimeException("Ma QR check-in khong hop le hoac da het hieu luc"));

        Instant now = Instant.now();
        if (detail.getQrCodeExpiredAt() == null || !now.isBefore(detail.getQrCodeExpiredAt())) {
            clearQrToken(detail, now);
            bookingDetailRepository.save(detail);
            throw new RuntimeException("Ma QR check-in da het hieu luc");
        }

        Booking booking = detail.getBooking();
        User customer = booking.getUser();
        if (customer == null) {
            throw new RuntimeException("Booking chua duoc lien ket voi tai khoan khach hang");
        }

        validateQrCheckInActor(actor, customer);
        validateQrBookingForCheckIn(booking, detail, customer);

        return completeCheckIn(booking, detail, actor);
    }

    @Override
    public AiFaceReadinessResponse checkFaceReadiness(
            MultipartFile selfieImage,
            String actorEmail) {
        validateSelfie(selfieImage);

        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        if (!"manager".equalsIgnoreCase(actor.getRole().name())) {
            throw new RuntimeException("Chỉ Manager mới được kiểm tra camera FaceID");
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        addFaceImagePart(
                bodyBuilder,
                "selfie_image",
                selfieImage,
                "face-readiness.jpg");

        try {
            AiFaceReadinessResponse response = webClient.post()
                    .uri(aiFaceReadinessUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(AiFaceReadinessResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) {
                throw new RuntimeException("Face readiness API trả về phản hồi rỗng");
            }
            return response;
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                    "Face readiness API lỗi: " + ex.getResponseBodyAsString(),
                    ex);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Không thể kết nối tới dịch vụ kiểm tra camera FaceID",
                    ex);
        }
    }

    private BookingResponse completeCheckIn(
            Booking booking,
            BookingDetail detail,
            User checkedInBy) {
        int quantity = detail.getQuantity() == null
                ? DEFAULT_SINGLE_BOOKING_QUANTITY
                : detail.getQuantity();
        if (quantity <= 0) {
            throw new RuntimeException("Số lượng phòng của booking không hợp lệ");
        }

        List<BookingRoomAccess> existingAccesses = bookingRoomAccessRepository
                .findByBooking_IdOrderByRoom_RoomNumberAsc(
                        booking.getId());
        if (!existingAccesses.isEmpty()) {
            throw new RuntimeException("Booking này đã được cấp quyền truy cập phòng");
        }

        List<Room> availableRooms = roomRepository
                .findByRoomType_IdAndStatusOrderByRoomNumberAsc(
                        detail.getRoomType().getId(),
                        AVAILABLE_ROOM_STATUS);

        List<Room> selectedRooms = new ArrayList<>();
        if (detail.getRoom() != null) {
            Room assignedRoom = detail.getRoom();
            if (!detail.getRoomType().getId().equals(assignedRoom.getRoomType().getId())) {
                throw new RuntimeException("Phòng được gán không thuộc loại phòng đã đặt");
            }
            if (!AVAILABLE_ROOM_STATUS.equalsIgnoreCase(assignedRoom.getStatus())) {
                throw new RuntimeException("Phòng được gán hiện chưa sẵn sàng để nhận phòng");
            }
            selectedRooms.add(assignedRoom);
        }

        for (Room room : availableRooms) {
            if (selectedRooms.size() >= quantity) {
                break;
            }
            if (selectedRooms.stream().noneMatch(selected -> selected.getId().equals(room.getId()))) {
                selectedRooms.add(room);
            }
        }

        if (selectedRooms.size() < quantity) {
            throw new RuntimeException(
                    "Không đủ phòng sẵn sàng để nhận phòng. Cần "
                            + quantity + " phòng nhưng chỉ có " + selectedRooms.size());
        }

        Instant now = Instant.now();
        Instant keyExpiresAt = toCheckoutExpiry(detail.getExpectedCheckOut());
        Set<String> generatedPasswords = new HashSet<>();
        List<BookingRoomAccess> roomAccesses = new ArrayList<>();

        for (Room room : selectedRooms) {
            String password = generateUniqueRoomPassword(generatedPasswords);

            BookingRoomAccess access = new BookingRoomAccess();
            access.setBooking(booking);
            access.setRoom(room);
            access.setRoomKeyAccess(password);
            access.setRoomKeyGeneratedAt(now);
            access.setRoomKeyExpiredAt(keyExpiresAt);
            access.setRoomKeyStatus(ROOM_KEY_STATUS_ACTIVE);
            access.setCreatedAt(now);
            access.setUpdatedAt(now);
            roomAccesses.add(access);

            room.setStatus(ROOM_STATUS_OCCUPIED);
            room.setUpdatedAt(now);

            // Broadcast room status change via WebSocket
            webSocketService.broadcastRoomStatus(room.getId(), room.getRoomNumber(), ROOM_STATUS_OCCUPIED);
        }

        BookingRoomAccess primaryAccess = roomAccesses.get(0);
        detail.setRoom(primaryAccess.getRoom());
        detail.setRoomKeyAccess(primaryAccess.getRoomKeyAccess());
        detail.setRoomKeyGeneratedAt(now);
        detail.setRoomKeyExpiredAt(keyExpiresAt);
        detail.setRoomKeyStatus(ROOM_KEY_STATUS_ACTIVE);
        detail.setActualCheckIn(now);
        detail.setCheckedInAt(now);
        detail.setCheckedInBy(checkedInBy);
        clearQrToken(detail, now);
        detail.setUpdatedAt(now);

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setUpdatedAt(now);

        roomRepository.saveAll(selectedRooms);
        bookingRoomAccessRepository.saveAll(roomAccesses);
        bookingRepository.save(booking);
        bookingDetailRepository.save(detail);

        try {
            String roomsText = selectedRooms.stream()
                    .map(Room::getRoomNumber)
                    .collect(java.util.stream.Collectors.joining(", "));
            String checkInMsg = String.format(
                    "Bạn đã nhận phòng thành công! Số phòng của bạn: %s. Mã khóa phòng đã được cập nhật trong thông tin chi tiết đặt phòng.",
                    roomsText);
            notificationService.sendNotification(booking.getUser(), "Nhận phòng thành công", checkInMsg, "CheckIn",
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send check-in notification: ", e);
        }

        return mapToResponse(booking, detail, detail.getRoomType());
    }

    @Override
    @Transactional
    public BookingResponse performCheckOut(Integer bookingId, String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name())
                && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Đơn đặt phòng chưa được check-in");
        }

        BookingDetail detail = bookingDetailRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        Instant now = Instant.now();
        Instant expectedCheckOut = detail.getExpectedCheckOut();

        // Kiểm tra xem đã tính phụ thu check-out trễ chưa (để tránh tính trùng)
        boolean alreadyChargedPenalty = booking.getSpecialRequests() != null
                && booking.getSpecialRequests().contains("[Late Check-out Penalty]");

        if (!alreadyChargedPenalty && now.isAfter(expectedCheckOut)) {
            long lateMinutes = java.time.temporal.ChronoUnit.MINUTES.between(expectedCheckOut, now);
            if (lateMinutes > 15) { // Quá 15 phút mới phạt để tạo trải nghiệm khách hàng thoải mái
                double penaltyRate = 0.0;
                String penaltyName = "";
                if (lateMinutes <= 180) { // <= 3 giờ
                    penaltyRate = 0.3;
                    penaltyName = "30% (Trễ dưới 3 giờ)";
                } else if (lateMinutes <= 360) { // <= 6 giờ
                    penaltyRate = 0.5;
                    penaltyName = "50% (Trễ từ 3-6 giờ)";
                } else {
                    penaltyRate = 1.0;
                    penaltyName = "100% (Trễ trên 6 giờ)";
                }

                BigDecimal oneNightCost = detail.getPriceAtBooking().multiply(BigDecimal.valueOf(detail.getQuantity()));
                BigDecimal penaltyAmount = oneNightCost.multiply(BigDecimal.valueOf(penaltyRate)).setScale(2,
                        RoundingMode.HALF_UP);

                if (penaltyAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // Cộng phụ thu vào Booking
                    BigDecimal newTotal = booking.getTotalAmount().add(penaltyAmount);
                    BigDecimal taxAmount = newTotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal finalAmount = newTotal.add(taxAmount).subtract(booking.getDiscountAmount()).setScale(2,
                            RoundingMode.HALF_UP);

                    booking.setTotalAmount(newTotal);
                    booking.setTaxAmount(taxAmount);
                    booking.setFinalAmount(finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount);

                    String currentRequests = booking.getSpecialRequests() != null ? booking.getSpecialRequests() : "";
                    booking.setSpecialRequests(currentRequests + " [Late Check-out Penalty: Phụ thu check-out trễ "
                            + penaltyName + " + " + formatCurrency(penaltyAmount) + "]");
                    booking.setUpdatedAt(Instant.now());

                    bookingRepository.save(booking);
                }
            }
        }

        BigDecimal dueAmount = booking.getFinalAmount().subtract(booking.getPaidAmount());
        if (dueAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException(
                    "Đơn đặt phòng chưa được thanh toán đầy đủ (bao gồm phụ thu check-out trễ nếu có). Quý khách cần thanh toán thêm "
                            + formatCurrency(dueAmount) + " trước khi trả phòng.");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setUpdatedAt(Instant.now());
        bookingRepository.save(booking);
        now = Instant.now();
        detail.setActualCheckOut(now);
        detail.setCheckedOutAt(now);
        detail.setCheckedOutBy(staff);
        List<BookingRoomAccess> roomAccesses = bookingRoomAccessRepository
                .findByBooking_IdOrderByRoom_RoomNumberAsc(bookingId);
        for (BookingRoomAccess access : roomAccesses) {
            Room room = access.getRoom();
            room.setStatus("Cleaning");
            room.setUpdatedAt(now);
            access.setRoomKeyAccess(null);
            access.setRoomKeyExpiredAt(now);
            access.setRoomKeyStatus(ROOM_KEY_STATUS_EXPIRED);
            access.setUpdatedAt(now);

            // Broadcast room status change via WebSocket
            webSocketService.broadcastRoomStatus(room.getId(), room.getRoomNumber(), "Cleaning");
        }
        if (!roomAccesses.isEmpty()) {
            roomRepository.saveAll(roomAccesses.stream()
                    .map(BookingRoomAccess::getRoom)
                    .toList());
            bookingRoomAccessRepository.saveAll(roomAccesses);
        }

        detail.setRoomKeyAccess(null);
        detail.setRoomKeyExpiredAt(now);
        detail.setRoomKeyStatus(ROOM_KEY_STATUS_EXPIRED);
        detail.setUpdatedAt(now);

        if (roomAccesses.isEmpty() && detail.getRoom() != null) {
            Room room = detail.getRoom();
            room.setStatus("Cleaning");
            room.setUpdatedAt(now);
            roomRepository.save(room);

            // Broadcast room status change via WebSocket
            webSocketService.broadcastRoomStatus(room.getId(), room.getRoomNumber(), "Cleaning");
        }
        bookingDetailRepository.save(detail);

        try {
            String checkOutMsg = String.format(
                    "Bạn đã trả phòng thành công cho đơn đặt phòng %s! Cảm ơn bạn đã lựa chọn Elysian Hotel. Chúc bạn một ngày tốt lành!",
                    booking.getBookingReference());
            notificationService.sendNotification(booking.getUser(), "Trả phòng thành công", checkOutMsg, "CheckOut",
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send check-out notification: ", e);
        }

        return mapToResponse(booking, detail, detail.getRoomType());
    }

    private AiFaceVerificationResponse callFaceVerificationService(
            String registeredEmbedding,
            MultipartFile selfieImage,
            MultipartFile challengeImage,
            MultipartFile challengeImage2,
            MultipartFile challengeImage3,
            String challengeDirection) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("registered_embedding", registeredEmbedding)
                .contentType(MediaType.TEXT_PLAIN);

        addFaceImagePart(bodyBuilder, "selfie_image", selfieImage, "face-center.jpg");
        addFaceImagePart(
                bodyBuilder,
                "challenge_image",
                challengeImage,
                "face-challenge.jpg");
        addFaceImagePart(
                bodyBuilder,
                "challenge_image_2",
                challengeImage2,
                "face-challenge-2.jpg");
        addFaceImagePart(
                bodyBuilder,
                "challenge_image_3",
                challengeImage3,
                "face-challenge-3.jpg");
        bodyBuilder.part("challenge_direction", challengeDirection)
                .contentType(MediaType.TEXT_PLAIN);

        try {
            AiFaceVerificationResponse response = webClient.post()
                    .uri(aiFaceVerifyUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(AiFaceVerificationResponse.class)
                    .timeout(Duration.ofSeconds(120))
                    .block();

            if (response == null) {
                throw new RuntimeException("Face API trả về phản hồi rỗng");
            }
            return response;
        } catch (WebClientResponseException ex) {
            throw new RuntimeException(
                    "Face API lỗi: " + ex.getResponseBodyAsString(),
                    ex);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Không thể kết nối tới dịch vụ xác minh khuôn mặt",
                    ex);
        }
    }

    private void addFaceImagePart(
            MultipartBodyBuilder bodyBuilder,
            String partName,
            MultipartFile image,
            String fallbackFilename) {
        Resource resource = image.getResource();
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (image.getContentType() != null && !image.getContentType().isBlank()) {
            contentType = MediaType.parseMediaType(image.getContentType());
        }
        bodyBuilder.part(partName, resource)
                .filename(image.getOriginalFilename() != null
                        ? image.getOriginalFilename()
                        : fallbackFilename)
                .contentType(contentType);
    }

    private void validateSelfie(MultipartFile selfieImage) {
        if (selfieImage == null || selfieImage.isEmpty()) {
            throw new RuntimeException("Vui lòng chụp hoặc tải lên ảnh khuôn mặt");
        }
        String contentType = selfieImage.getContentType();
        if (contentType != null
                && !contentType.equalsIgnoreCase(MediaType.IMAGE_JPEG_VALUE)
                && !contentType.equalsIgnoreCase(MediaType.IMAGE_PNG_VALUE)
                && !contentType.equalsIgnoreCase("image/webp")) {
            throw new RuntimeException("Ảnh khuôn mặt chỉ hỗ trợ JPG, PNG hoặc WebP");
        }
    }

    private void validateCheckInDateWindow(BookingDetail detail) {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        LocalDate checkInDate = toLocalDate(detail.getExpectedCheckIn());
        LocalDate checkOutDate = toLocalDate(detail.getExpectedCheckOut());

        if (today.isBefore(checkInDate)) {
            throw new RuntimeException("Chưa đến ngày nhận phòng");
        }
        if (!today.isBefore(checkOutDate)) {
            throw new RuntimeException("Đơn đặt phòng đã quá thời gian nhận phòng");
        }
    }

    private void validateFaceIdBookingEligibility(User customer) {
        boolean ekycVerified = ekycProfileRepository.existsVerifiedByUserid(customer);
        boolean faceRegistered = faceEmbeddingRepository
                .findEmbeddingTextByUserId(customer.getId())
                .filter(embedding -> !embedding.isBlank())
                .isPresent();

        if (!ekycVerified || !faceRegistered) {
            throw new RuntimeException(
                    "Ban phai hoan thanh dang ky eKYC va khuon mat truoc khi chon check-in bang FaceID");
        }
    }

    private void validateQrCodeBookingEligibility(User customer) {
        if (!ekycProfileRepository.existsVerifiedByUserid(customer)) {
            throw new RuntimeException(
                    "Ban phai hoan thanh dang ky eKYC truoc khi chon check-in bang QR Code");
        }
    }

    private void validateQrBookingForCheckIn(
            Booking booking,
            BookingDetail detail,
            User customer) {
        if (!isQrCodeMethod(booking.getCheckInMethod())) {
            throw new RuntimeException("Don dat phong nay khong su dung phuong thuc QR Code");
        }
        validateCheckInEligibleStatus(booking);
        validateBookingNotCheckedIn(booking, detail);
        validateCheckInDateWindow(detail);
        validateQrCodeBookingEligibility(customer);
    }

    private void validateCheckInEligibleStatus(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED &&
                booking.getStatus() != BookingStatus.PARTIALLY_PAID &&
                booking.getStatus() != BookingStatus.PAID) {
            throw new RuntimeException("Don dat phong khong o trang thai co the nhan phong");
        }
    }

    private void validateBookingNotCheckedIn(Booking booking, BookingDetail detail) {
        if (detail.getActualCheckIn() != null
                || booking.getStatus() == BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Booking nay da duoc check-in");
        }
    }

    private void validateQrCheckInActor(User actor, User customer) {
        boolean isOwner = actor.getId() != null && actor.getId().equals(customer.getId());
        if (!isOwner && !isStaffCheckInActor(actor)) {
            throw new RuntimeException("Ban khong co quyen check-in booking nay bang QR Code");
        }
    }

    private boolean isStaffCheckInActor(User actor) {
        return hasRole(actor, "receptionist") || hasRole(actor, "manager");
    }

    private boolean hasRole(User user, String role) {
        return user != null
                && user.getRole() != null
                && role.equalsIgnoreCase(user.getRole().name());
    }

    private Instant calculateQrTokenExpiry(Instant generatedAt, BookingDetail detail) {
        long ttlMinutes = Math.max(1, qrTokenTtlMinutes);
        Instant expiresAt = generatedAt.plus(Duration.ofMinutes(ttlMinutes));
        Instant checkoutExpiry = toCheckoutExpiry(detail.getExpectedCheckOut());
        if (expiresAt.isAfter(checkoutExpiry)) {
            expiresAt = checkoutExpiry;
        }
        if (!expiresAt.isAfter(generatedAt)) {
            throw new RuntimeException("Khong the tao QR token cho booking da qua thoi gian nhan phong");
        }
        return expiresAt;
    }

    private String generateUniqueQrToken() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String token = generateQrToken();
            if (!bookingDetailRepository.existsByQrCodeValue(token)) {
                return token;
            }
        }
        throw new RuntimeException("Khong the tao QR token check-in");
    }

    private String generateQrToken() {
        byte[] randomBytes = new byte[QR_TOKEN_RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private boolean hasActiveQrToken(BookingDetail detail, Instant now) {
        return detail.getQrCodeValue() != null
                && !detail.getQrCodeValue().isBlank()
                && detail.getQrCodeExpiredAt() != null
                && now.isBefore(detail.getQrCodeExpiredAt());
    }

    private void sendQrCheckInEmailSafely(
            User customer,
            Booking booking,
            BookingDetail detail,
            String token,
            Instant expiresAt) {
        if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
            return;
        }

        try {
            RoomType roomtype = detail.getRoomType();
            emailService.sendQrCheckInEmail(
                    customer.getEmail(),
                    customer.getFullName(),
                    booking.getBookingReference(),
                    roomtype != null ? roomtype.getName() : null,
                    detail.getExpectedCheckIn() != null ? toLocalDate(detail.getExpectedCheckIn()) : null,
                    detail.getExpectedCheckOut() != null ? toLocalDate(detail.getExpectedCheckOut()) : null,
                    token,
                    expiresAt);
        } catch (RuntimeException ex) {
            log.warn("Failed to send QR check-in email for booking {}", booking.getId(), ex);
        }
    }

    private void clearQrToken(BookingDetail detail, Instant now) {
        detail.setQrCodeValue(null);
        detail.setQrCodeGeneratedAt(null);
        detail.setQrCodeExpiredAt(null);
        detail.setUpdatedAt(now);
    }

    private String generateRoomPassword() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private String generateUniqueRoomPassword(Set<String> generatedPasswords) {
        for (int attempt = 0; attempt < 20; attempt++) {
            String password = generateRoomPassword();
            if (generatedPasswords.add(password)) {
                return password;
            }
        }
        throw new RuntimeException("Không thể tạo mật khẩu riêng cho các phòng");
    }

    private Instant toCheckoutExpiry(Instant expectedCheckout) {
        LocalDate checkoutDate = toLocalDate(expectedCheckout);
        return checkoutDate.atTime(LocalTime.NOON).atZone(HOTEL_ZONE).toInstant();
    }

    private List<RoomAccessResponse> mapRoomAccesses(
            Booking booking,
            BookingDetail detail,
            boolean includeRoomPassword) {
        List<BookingRoomAccess> accesses = bookingRoomAccessRepository.findByBooking_IdOrderByRoom_RoomNumberAsc(
                booking.getId());
        return mapRoomAccesses(booking, detail, accesses, includeRoomPassword);
    }

    private List<RoomAccessResponse> mapRoomAccesses(
            Booking booking,
            BookingDetail detail,
            List<BookingRoomAccess> accesses,
            boolean includeRoomPassword) {
        if (accesses != null && !accesses.isEmpty()) {
            return accesses.stream()
                    .map(access -> {
                        boolean usable = includeRoomPassword
                                && isRoomAccessUsable(booking, access);
                        Room room = access.getRoom();
                        return RoomAccessResponse.builder()
                                .roomId(room.getId())
                                .roomNumber(room.getRoomNumber())
                                .floorNumber(room.getFloorNumber())
                                .roomPassword(usable ? access.getRoomKeyAccess() : null)
                                .roomKeyStatus(access.getRoomKeyStatus())
                                .roomKeyExpiresAt(access.getRoomKeyExpiredAt())
                                .build();
                    })
                    .toList();
        }

        if (detail != null && detail.getRoom() != null) {
            Room room = detail.getRoom();
            boolean usable = includeRoomPassword && isRoomKeyUsable(booking, detail);
            return List.of(RoomAccessResponse.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .floorNumber(room.getFloorNumber())
                    .roomPassword(usable ? detail.getRoomKeyAccess() : null)
                    .roomKeyStatus(detail.getRoomKeyStatus())
                    .roomKeyExpiresAt(detail.getRoomKeyExpiredAt())
                    .build());
        }

        return List.of();
    }

    private boolean isRoomAccessUsable(
            Booking booking,
            BookingRoomAccess access) {
        return (booking.getStatus() == BookingStatus.CHECKED_IN)
                && ROOM_KEY_STATUS_ACTIVE.equalsIgnoreCase(access.getRoomKeyStatus())
                && access.getRoomKeyAccess() != null
                && !access.getRoomKeyAccess().isBlank()
                && access.getRoomKeyExpiredAt() != null
                && Instant.now().isBefore(access.getRoomKeyExpiredAt());
    }

    private boolean isRoomKeyUsable(Booking booking, BookingDetail detail) {
        return (booking.getStatus() == BookingStatus.CHECKED_IN)
                && detail.getActualCheckIn() != null
                && detail.getActualCheckOut() == null
                && ROOM_KEY_STATUS_ACTIVE.equalsIgnoreCase(detail.getRoomKeyStatus())
                && detail.getRoomKeyAccess() != null
                && !detail.getRoomKeyAccess().isBlank()
                && detail.getRoomKeyExpiredAt() != null
                && Instant.now().isBefore(detail.getRoomKeyExpiredAt());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingHistoryResponse> getAllBookingsForStaff(String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name())
                && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        boolean includeRoomPassword = "manager".equalsIgnoreCase(staff.getRole().name());

        return bookingDetailRepository.findAll().stream()
                .map(detail -> mapToHistoryResponse(detail, includeRoomPassword))
                .toList();
    }

    private Instant toInstant(LocalDate date) {
        return date.atStartOfDay(HOTEL_ZONE).toInstant();
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant.atZone(HOTEL_ZONE).toLocalDate();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeCheckInMethod(String checkInMethod) {
        if ("Face Recognition".equalsIgnoreCase(checkInMethod)
                || "Face ID".equalsIgnoreCase(checkInMethod)
                || "FaceID".equalsIgnoreCase(checkInMethod)) {
            return "FaceID";
        }
        if ("QR".equalsIgnoreCase(checkInMethod)
                || "QRCode".equalsIgnoreCase(checkInMethod)
                || "QR Code".equalsIgnoreCase(checkInMethod)) {
            return "QR Code";
        }
        return checkInMethod;
    }

    @Override
    @Transactional
    public BookingResponse createWalkInBooking(WalkInBookingRequest request, String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name())
                && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        validateGuestCounts(request.getNumberOfAdults(), request.getNumberOfChildren());

        User customer = userRepository.findByEmail(request.getCustomerEmail()).orElse(null);
        if (customer == null) {
            if (userRepository.existsByIdCardNumber(request.getCustomerIdCardNumber())) {
                throw new RuntimeException("So CCCD nay da ton tai trong he thong");
            }
            customer = new User();
            customer.setEmail(request.getCustomerEmail());
            customer.setFullName(request.getCustomerFullname());
            customer.setPhoneNumber(request.getCustomerPhonenumber());
            customer.setIdCardNumber(request.getCustomerIdCardNumber());
            customer.setRole(Role.customer);
            customer.setPasswordHash("");
            customer.setStatus("Active");
            customer.setCreatedAt(Instant.now());
            customer = userRepository.save(customer);
        } else if (customer.getIdCardNumber() == null || customer.getIdCardNumber().isBlank()) {
            if (userRepository.existsByIdCardNumber(request.getCustomerIdCardNumber())) {
                throw new RuntimeException("So CCCD nay da ton tai trong he thong");
            }
            customer.setIdCardNumber(request.getCustomerIdCardNumber());
            customer.setUpdatedAt(Instant.now());
            customer = userRepository.save(customer);
        } else if (!customer.getIdCardNumber().equals(request.getCustomerIdCardNumber())) {
            throw new RuntimeException("Email khach hang da gan voi so CCCD khac");
        }

        RoomType roomtype = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));

        if (!"Active".equalsIgnoreCase(roomtype.getStatus())) {
            throw new RuntimeException("Loại phòng hiện không hoạt động");
        }

        assertCapacity(roomtype, request.getQuantity(), request.getNumberOfAdults(), request.getNumberOfChildren());
        assertAvailability(roomtype, request.getCheckInDate(), request.getCheckOutDate(), request.getQuantity());

        Instant now = Instant.now();
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = roomtype.getBasePrice()
                .multiply(BigDecimal.valueOf(request.getQuantity()))
                .multiply(BigDecimal.valueOf(nights));

        BigDecimal taxAmount = totalAmount.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = totalAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setUser(customer);
        booking.setBookingReference(generateBookingReference(now));
        booking.setBookingType("Walk-in");
        booking.setCheckInMethod("Manual");
        booking.setTotalAmount(totalAmount);
        booking.setPaidAmount(BigDecimal.ZERO);
        booking.setDepositAmount(BigDecimal.ZERO);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTaxAmount(taxAmount);
        booking.setServiceChargeAmount(BigDecimal.ZERO);
        booking.setFinalAmount(finalAmount);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);
        booking.setCreatedBy(staff);

        Booking savedBooking = bookingRepository.save(booking);

        BookingDetail detail = new BookingDetail();
        detail.setBooking(savedBooking);
        detail.setRoomType(roomtype);
        detail.setQuantity(request.getQuantity());
        detail.setExpectedCheckIn(toInstant(request.getCheckInDate()));
        detail.setExpectedCheckOut(toInstant(request.getCheckOutDate()));
        detail.setPriceAtBooking(roomtype.getBasePrice());
        detail.setNumberOfAdults(request.getNumberOfAdults());
        detail.setNumberOfChildren(request.getNumberOfChildren());
        detail.setStatus("Active");
        detail.setCreatedAt(now);
        detail.setUpdatedAt(now);

        bookingDetailRepository.save(detail);

        // Record upfront payment if any
        if (request.getPaidAmount() != null && request.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            Payment payment = new Payment();
            payment.setBooking(savedBooking);
            payment.setAmount(request.getPaidAmount());
            payment.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "Cash");
            payment.setPaymentType("Booking Payment");
            payment.setTransactionCode("WALKIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payment.setStatus("Completed");
            payment.setRefundedAmount(BigDecimal.ZERO);
            payment.setPaymentDate(now);
            payment.setNotes("Walk-in payment recorded at creation.");
            paymentRepository.save(payment);

            savedBooking.setPaidAmount(request.getPaidAmount());
            if (savedBooking.getPaidAmount().compareTo(savedBooking.getFinalAmount()) >= 0) {
                savedBooking.setStatus(BookingStatus.PAID);
            } else {
                savedBooking.setStatus(BookingStatus.PARTIALLY_PAID);
            }
            bookingRepository.save(savedBooking);
        }

        // Perform immediate check-in for walk-in booking
        return completeCheckIn(savedBooking, detail, staff);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceDetails(Integer bookingId, String actorEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        boolean isStaff = "receptionist".equalsIgnoreCase(actor.getRole().name())
                || "manager".equalsIgnoreCase(actor.getRole().name());

        if (!isStaff && !actor.getId().equals(booking.getUser().getId())) {
            throw new RuntimeException("Bạn không có quyền xem hóa đơn này");
        }

        BookingDetail detail = bookingDetailRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        List<BookingRoomAccess> roomAccesses = bookingRoomAccessRepository
                .findByBooking_IdOrderByRoom_RoomNumberAsc(bookingId);
        List<String> assignedRooms = new ArrayList<>();
        if (!roomAccesses.isEmpty()) {
            assignedRooms = roomAccesses.stream()
                    .map(access -> access.getRoom().getRoomNumber())
                    .toList();
        } else if (detail.getRoom() != null) {
            assignedRooms = List.of(detail.getRoom().getRoomNumber());
        }

        List<com.example.hotelsmartbookingbackend.entity.BookingService> bookingservices = bookingServiceRepository.findByBooking_Id(bookingId);
        List<InvoiceResponse.ServiceChargeItem> serviceItems = bookingservices.stream()
                .map(bs -> InvoiceResponse.ServiceChargeItem.builder()
                        .usageId(bs.getId())
                        .serviceName(bs.getNote() != null && !bs.getNote().isBlank() ? bs.getNote() : "Dịch vụ phụ thu")
                        .quantity(bs.getQuantity())
                        .unitPrice(bs.getUnitPrice())
                        .totalPrice(bs.getTotalPrice())
                        .implementedAt(bs.getImplementedAt())
                        .note(bs.getNote())
                        .build())
                .toList();

        BigDecimal serviceTotal = bookingservices.stream()
                .map(com.example.hotelsmartbookingbackend.entity.BookingService::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> payments = paymentRepository.findByBooking_Id(bookingId);
        List<InvoiceResponse.PaymentItem> paymentItems = payments.stream()
                .map(p -> InvoiceResponse.PaymentItem.builder()
                        .paymentId(p.getId())
                        .amount(p.getAmount())
                        .paymentMethod(p.getPaymentMethod())
                        .paymentType(p.getPaymentType())
                        .transactionCode(p.getTransactionCode())
                        .status(p.getStatus())
                        .refundedAmount(p.getRefundedAmount())
                        .paymentDate(p.getPaymentDate())
                        .notes(p.getNotes())
                        .build())
                .toList();

        long nights = ChronoUnit.DAYS.between(toLocalDate(detail.getExpectedCheckIn()),
                toLocalDate(detail.getExpectedCheckOut()));
        BigDecimal roomRate = detail.getPriceAtBooking();
        BigDecimal roomTotal = roomRate.multiply(BigDecimal.valueOf(detail.getQuantity()))
                .multiply(BigDecimal.valueOf(nights));
        BigDecimal taxAmount = booking.getTaxAmount();
        BigDecimal discountAmount = booking.getDiscountAmount();
        BigDecimal finalAmount = booking.getFinalAmount();
        BigDecimal paidAmount = booking.getPaidAmount();
        BigDecimal dueAmount = finalAmount.subtract(paidAmount);

        return InvoiceResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .bookingType(booking.getBookingType())
                .customerName(booking.getUser().getFullName())
                .customerEmail(booking.getUser().getEmail())
                .customerPhone(booking.getUser().getPhoneNumber())
                .checkInDate(toLocalDate(detail.getExpectedCheckIn()))
                .checkOutDate(toLocalDate(detail.getExpectedCheckOut()))
                .nights(nights)
                .roomTypeName(detail.getRoomType().getName())
                .quantity(detail.getQuantity())
                .assignedRooms(assignedRooms)
                .roomRate(roomRate)
                .roomTotal(roomTotal)
                .services(serviceItems)
                .serviceTotal(serviceTotal)
                .taxAmount(taxAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .paidAmount(paidAmount)
                .dueAmount(dueAmount)
                .payments(paymentItems)
                .build();
    }

    @Override
    @Transactional
    public void addServiceToBooking(Integer bookingId, AddServiceRequest request, String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name())
                && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Không thể thêm dịch vụ cho đơn đặt phòng ở trạng thái này");
        }

        com.example.hotelsmartbookingbackend.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));

        if (Boolean.FALSE.equals(service.getIsActive())) {
            throw new RuntimeException("Dịch vụ hiện không hoạt động");
        }

        BigDecimal unitPrice = service.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        com.example.hotelsmartbookingbackend.entity.BookingService usage = new com.example.hotelsmartbookingbackend.entity.BookingService();
        usage.setBooking(booking);
        usage.setService(service);
        usage.setImplementedBy(staff);
        usage.setQuantity(request.getQuantity());
        usage.setUnitPrice(unitPrice);
        usage.setTotalPrice(totalPrice);
        usage.setImplementedAt(Instant.now());
        usage.setNote(service.getName() + (request.getNote() != null && !request.getNote().isBlank()
                ? " (" + request.getNote() + ")"
                : ""));
        usage.setStatus("Active");

        bookingServiceRepository.save(usage);

        // Update booking service charge, tax, and final amounts
        BigDecimal serviceTax = totalPrice.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        booking.setServiceChargeAmount(booking.getServiceChargeAmount().add(totalPrice));
        booking.setTaxAmount(booking.getTaxAmount().add(serviceTax));
        booking.setFinalAmount(booking.getFinalAmount().add(totalPrice).add(serviceTax));

        if (booking.getPaidAmount().compareTo(booking.getFinalAmount()) < 0) {
            if (booking.getStatus() == BookingStatus.PAID) {
                booking.setStatus(BookingStatus.PARTIALLY_PAID);
            }
        }

        booking.setUpdatedAt(Instant.now());

        bookingRepository.save(booking);
    }

    private boolean isFaceIdMethod(String checkInMethod) {
        return "Face Recognition".equalsIgnoreCase(checkInMethod)
                || "Face ID".equalsIgnoreCase(checkInMethod)
                || "FaceID".equalsIgnoreCase(checkInMethod);
    }

    private boolean isQrCodeMethod(String checkInMethod) {
        return "QR".equalsIgnoreCase(checkInMethod)
                || "QRCode".equalsIgnoreCase(checkInMethod)
                || "QR Code".equalsIgnoreCase(checkInMethod);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingHistoryResponse> filterBookings(BookingFilter criteria) {
        int page = criteria.getPage() != null ? criteria.getPage() : 0;
        int pageSize = criteria.getPageSize() != null ? criteria.getPageSize() : 10;
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "createdAt";
        Sort.Direction direction = criteria.getSortDirection() != null
                && criteria.getSortDirection().equalsIgnoreCase("DESC")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        Page<Booking> bookings = bookingRepository.findAll(new BookingSpecification(criteria), pageable);

        List<Booking> bookingList = bookings.getContent();
        List<Integer> bookingIds = bookingList.stream().map(Booking::getId).toList();

        List<BookingDetail> detailsList = bookingDetailRepository.findByBooking_IdIn(bookingIds);
        java.util.Map<Integer, BookingDetail> detailsMap = new java.util.HashMap<>();
        for (BookingDetail detail : detailsList) {
            if (detail.getBooking() != null) {
                detailsMap.put(detail.getBooking().getId(), detail);
            }
        }

        List<BookingRoomAccess> accessesList = bookingRoomAccessRepository.findByBooking_IdIn(bookingIds);
        java.util.Map<Integer, List<BookingRoomAccess>> accessesMap = new java.util.HashMap<>();
        for (BookingRoomAccess access : accessesList) {
            if (access.getBooking() != null) {
                accessesMap.computeIfAbsent(access.getBooking().getId(), k -> new java.util.ArrayList<>())
                        .add(access);
            }
        }

        List<BookingHistoryResponse> content = new java.util.ArrayList<>();
        for (Booking booking : bookingList) {
            content.add(mapToBookingHistoryResponse(
                    booking,
                    detailsMap.get(booking.getId()),
                    accessesMap.get(booking.getId())));
        }

        return PageResponse.<BookingHistoryResponse>builder()
                .content(content)
                .page(bookings.getNumber())
                .size(bookings.getSize())
                .totalElements(bookings.getTotalElements())
                .totalPages(bookings.getTotalPages())
                .first(bookings.isFirst())
                .last(bookings.isLast())
                .build();
    }

    @Override
    @Transactional
    public BookingResponse updateBooking(Integer bookingId, UpdateBookingRequest request, String staffEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + bookingId));

        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Không thể cập nhật đơn đặt phòng đã hủy hoặc đã hoàn thành");
        }

        BookingDetail detail = bookingDetailRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        // 1. Phân tích Ngày cũ / mới
        LocalDate checkIn = detail.getExpectedCheckIn().atZone(HOTEL_ZONE).toLocalDate();
        LocalDate checkOut = detail.getExpectedCheckOut().atZone(HOTEL_ZONE).toLocalDate();
        boolean datesChanged = false;

        if (request.getCheckInDate() != null && !request.getCheckInDate().trim().isEmpty()) {
            LocalDate newCheckIn = LocalDate.parse(request.getCheckInDate().trim());
            if (!newCheckIn.equals(checkIn)) {
                checkIn = newCheckIn;
                datesChanged = true;
                detail.setExpectedCheckIn(toInstant(checkIn));
            }
        }
        if (request.getCheckOutDate() != null && !request.getCheckOutDate().trim().isEmpty()) {
            LocalDate newCheckOut = LocalDate.parse(request.getCheckOutDate().trim());
            if (!newCheckOut.equals(checkOut)) {
                checkOut = newCheckOut;
                datesChanged = true;
                detail.setExpectedCheckOut(toInstant(checkOut));
            }
        }
        if (datesChanged) {
            if (checkIn.isAfter(checkOut) || checkIn.equals(checkOut)) {
                throw new RuntimeException("Ngày nhận phòng phải trước ngày trả phòng");
            }
        }

        // 2. Phân tích Loại phòng
        RoomType roomtype = detail.getRoomType();
        boolean roomTypeChanged = false;
        if (request.getRoomTypeId() != null && !request.getRoomTypeId().equals(roomtype.getId())) {
            roomtype = roomTypeRepository.findByIdForUpdate(request.getRoomTypeId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng mới"));
            detail.setRoomType(roomtype);
            detail.setPriceAtBooking(roomtype.getBasePrice());
            roomTypeChanged = true;
        }

        // 3. Phân tích số lượng phòng / khách
        int quantity = detail.getQuantity();
        if (request.getQuantity() != null && request.getQuantity() > 0 && request.getQuantity() != quantity) {
            quantity = request.getQuantity();
            detail.setQuantity(quantity);
        }

        int adults = detail.getNumberOfAdults();
        if (request.getNumberOfAdults() != null && request.getNumberOfAdults() > 0
                && request.getNumberOfAdults() != adults) {
            adults = request.getNumberOfAdults();
            detail.setNumberOfAdults(adults);
        }

        int children = detail.getNumberOfChildren();
        if (request.getNumberOfChildren() != null && request.getNumberOfChildren() >= 0
                && request.getNumberOfChildren() != children) {
            children = request.getNumberOfChildren();
            detail.setNumberOfChildren(children);
        }

        // 4. Validate Capacity và Availability phòng trống
        if (datesChanged || roomTypeChanged || request.getQuantity() != null || request.getNumberOfAdults() != null
                || request.getNumberOfChildren() != null) {
            assertCapacity(roomtype, quantity, adults, children);
            // Bỏ qua kiểm tra availability nếu không đổi ngày và loại phòng, số lượng phòng
            // giảm hoặc giữ nguyên
            if (datesChanged || roomTypeChanged
                    || (request.getQuantity() != null && request.getQuantity() > quantity)) {
                assertAvailability(roomtype, checkIn, checkOut, quantity);
            }
        }

        // 5. Cập nhật Yêu cầu đặc biệt & Ghi chú
        if (request.getSpecialRequests() != null) {
            booking.setSpecialRequests(request.getSpecialRequests());
        }

        // 6. Tính toán lại tài chính
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        if (nights <= 0) {
            nights = 1; // Tối thiểu 1 đêm
        }
        BigDecimal totalAmount = roomtype.getBasePrice()
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(nights));

        booking.setTotalAmount(totalAmount);
        BigDecimal taxAmount = totalAmount.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP);
        booking.setTaxAmount(taxAmount);

        BigDecimal discount = booking.getDiscountAmount();
        if (request.getDiscountAmount() != null) {
            discount = request.getDiscountAmount();
            booking.setDiscountAmount(discount);
        }
        BigDecimal finalAmount = totalAmount.add(taxAmount).subtract(discount).setScale(2,
                java.math.RoundingMode.HALF_UP);
        booking.setFinalAmount(finalAmount.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : finalAmount);

        Instant now = Instant.now();
        booking.setUpdatedAt(now);
        detail.setUpdatedAt(now);

        Booking updatedBooking = bookingRepository.save(booking);
        BookingDetail updatedDetail = bookingDetailRepository.save(detail);

        return mapToResponse(updatedBooking, updatedDetail, roomtype);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Integer bookingId, CancelBookingRequest request, String staffEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + bookingId));

        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Đơn đặt phòng này đã được hủy trước đó");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy đơn đặt phòng đã trả phòng");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(request.getCancellationReason());
        booking.setCancelledAt(Instant.now());
        booking.setCancelledBy(staff);
        booking.setUpdatedAt(Instant.now());

        BookingDetail detail = bookingDetailRepository
                .findByBooking_Id(bookingId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy chi tiết đơn đặt phòng"));

        detail.setStatus("Cancelled");
        detail.setUpdatedAt(Instant.now());
        bookingDetailRepository.save(detail);

        Booking cancelledBooking = bookingRepository.save(booking);

        try {
            String cancelMsg = String.format("Đơn đặt phòng %s của bạn đã bị hủy bởi nhân viên khách sạn. Lý do: %s.",
                    booking.getBookingReference(), booking.getCancellationReason());
            notificationService.sendNotification(booking.getUser(), "Đơn đặt phòng bị hủy", cancelMsg, "Cancellation",
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send staff cancellation notification: ", e);
        }

        return mapToBookingResponse(cancelledBooking);
    }

    @Override
    @Transactional
    public BookingResponse customerCancelBooking(Integer bookingId, CancelBookingRequest request,
            String customerEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + bookingId));

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        if (!booking.getUser().getId().equals(customer.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đơn đặt phòng này");
        }

        BookingStatus status = booking.getStatus();
        if (status == BookingStatus.CHECKED_IN || status == BookingStatus.COMPLETED
                || status == BookingStatus.CANCELLED) {
            throw new RuntimeException(
                    "Không thể hủy đơn đặt phòng ở trạng thái: " + (status != null ? status.getValue() : "null"));
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(
                request != null && request.getCancellationReason() != null ? request.getCancellationReason()
                        : "Khách hàng tự hủy trực tuyến");
        booking.setCancelledAt(Instant.now());
        booking.setCancelledBy(customer);
        booking.setUpdatedAt(Instant.now());

        BookingDetail detail = bookingDetailRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đơn đặt phòng"));

        detail.setStatus("Cancelled");
        detail.setUpdatedAt(Instant.now());
        bookingDetailRepository.save(detail);

        Booking saved = bookingRepository.save(booking);

        try {
            String cancelMsg = String.format("Khách hàng %s đã hủy đơn đặt phòng %s. Lý do: %s.",
                    customer.getFullName(), booking.getBookingReference(), booking.getCancellationReason());
            notificationService.sendNotificationToRoles(
                    List.of(Role.receptionist, Role.manager),
                    "Đơn đặt phòng bị hủy",
                    cancelMsg,
                    "Cancellation",
                    booking.getId());
        } catch (Exception e) {
            log.error("Failed to send customer cancellation notification: ", e);
        }

        return mapToBookingResponse(saved);
    }

    private BookingHistoryResponse mapToBookingHistoryResponse(Booking booking) {
        BookingDetail detail = bookingDetailRepository.findByBooking_Id(booking.getId()).orElse(null);
        return mapToBookingHistoryResponse(booking, detail, null);
    }

    private BookingHistoryResponse mapToBookingHistoryResponse(Booking booking, BookingDetail detail) {
        return mapToBookingHistoryResponse(booking, detail, null);
    }

    private BookingHistoryResponse mapToBookingHistoryResponse(Booking booking, BookingDetail detail,
            List<BookingRoomAccess> accesses) {
        BookingHistoryResponse.BookingHistoryResponseBuilder builder = BookingHistoryResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingReference())
                .bookingDate(booking.getCreatedAt())
                .status(booking.getStatus())
                .checkInMethod(booking.getCheckInMethod())
                .totalAmount(booking.getTotalAmount())
                .paidAmount(booking.getPaidAmount())
                .depositAmount(booking.getDepositAmount())
                .discountAmount(booking.getDiscountAmount())
                .taxAmount(booking.getTaxAmount())
                .serviceChargeAmount(booking.getServiceChargeAmount())
                .finalAmount(booking.getFinalAmount())
                .guestName(booking.getUser() != null ? booking.getUser().getFullName() : "Khách hàng Elysian")
                .guestEmail(booking.getUser() != null ? booking.getUser().getEmail() : "");

        if (detail != null) {
            RoomType roomtype = detail.getRoomType();
            Room assignedRoom = detail.getRoom();
            LocalDate checkInDate = toLocalDate(detail.getExpectedCheckIn());
            LocalDate checkOutDate = toLocalDate(detail.getExpectedCheckOut());

            builder.roomTypeId(roomtype != null ? roomtype.getId() : null)
                    .roomType(roomtype != null ? roomtype.getName() : null)
                    .quantity(detail.getQuantity())
                    .numberOfAdults(detail.getNumberOfAdults())
                    .numberOfChildren(detail.getNumberOfChildren())
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .nights(ChronoUnit.DAYS.between(checkInDate, checkOutDate))
                    .actualCheckIn(detail.getActualCheckIn())
                    .actualCheckOut(detail.getActualCheckOut())
                    .roomNumber(assignedRoom != null ? assignedRoom.getRoomNumber() : null)
                    .roomPassword(isRoomKeyUsable(booking, detail) ? detail.getRoomKeyAccess() : null)
                    .roomKeyStatus(detail.getRoomKeyStatus())
                    .roomKeyExpiresAt(detail.getRoomKeyExpiredAt())
                    .roomAccesses(mapRoomAccesses(booking, detail, accesses, true));
        }

        return builder.build();
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .bookingType(booking.getBookingType())
                .checkInMethod(booking.getCheckInMethod())
                .totalAmount(booking.getTotalAmount())
                .finalAmount(booking.getFinalAmount())
                .paidAmount(booking.getPaidAmount())
                .status(booking.getStatus())
                .specialRequests(booking.getSpecialRequests())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return String.format("%,.0f VND", amount);
    }
}
