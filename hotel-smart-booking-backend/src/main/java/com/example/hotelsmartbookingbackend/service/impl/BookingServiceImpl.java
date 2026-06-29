package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.BookingFilter;
import com.example.hotelsmartbookingbackend.dto.request.CancelBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.WalkInBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.AddServiceRequest;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceVerificationResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.InvoiceResponse;
import com.example.hotelsmartbookingbackend.dto.request.UpdateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceReadinessResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceVerificationResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycIdentitySummaryResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.QrTokenResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomAccessResponse;

import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.BookingRoomAccess;
import com.example.hotelsmartbookingbackend.entity.Bookingdetail;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.entity.Roomtype;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.entity.Payment;
import com.example.hotelsmartbookingbackend.entity.Bookingservice;
import com.example.hotelsmartbookingbackend.enums.Role;

import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.BookingRoomAccessRepository;
import com.example.hotelsmartbookingbackend.repository.BookingdetailRepository;
import com.example.hotelsmartbookingbackend.repository.EkycProfileRepository;
import com.example.hotelsmartbookingbackend.repository.FaceembeddingRepository;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomtypeRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.repository.PaymentRepository;
import com.example.hotelsmartbookingbackend.repository.BookingserviceRepository;
import com.example.hotelsmartbookingbackend.repository.ServiceRepository;
import com.example.hotelsmartbookingbackend.service.BookingService;
import com.example.hotelsmartbookingbackend.service.EmailService;
import com.example.hotelsmartbookingbackend.service.WebSocketService;

import com.example.hotelsmartbookingbackend.service.BookingService;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;

import com.example.hotelsmartbookingbackend.specification.BookingSpecification;

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
import java.util.UUID;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");
    private static final String ACTIVE_STATUS = "Active";
    private static final String AVAILABLE_ROOM_STATUS = "Available";
    private static final String BOOKING_TYPE_ONLINE = "Online";
    private static final String BOOKING_TYPE_GROUP = "Group";
    private static final String BOOKING_STATUS_CONFIRMED = "Confirmed";
    private static final String DETAIL_STATUS_ACTIVE = "Active";
    private static final String ROOM_KEY_STATUS_ACTIVE = "Active";
    private static final String ROOM_KEY_STATUS_EXPIRED = "Expired";
    private static final String ROOM_STATUS_OCCUPIED = "Occupied";
    private static final int DEFAULT_SINGLE_BOOKING_QUANTITY = 1;
    private static final int QR_TOKEN_RANDOM_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final List<String> INVENTORY_HOLDING_BOOKING_STATUSES = List.of("Pending", "Confirmed",
            "Checked In");
    private static final List<String> INVENTORY_HOLDING_DETAIL_STATUSES = List.of("Active");
    private static final DateTimeFormatter BOOKING_REFERENCE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(HOTEL_ZONE);
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BookingRepository bookingRepository;
    private final BookingdetailRepository bookingdetailRepository;
    private final BookingRoomAccessRepository bookingRoomAccessRepository;
    private final RoomtypeRepository roomtypeRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final EkycProfileRepository ekycProfileRepository;
    private final FaceembeddingRepository faceembeddingRepository;
    private final AesEncryptionService aesEncryptionService;
    private final SupabaseStorageService supabaseStorageService;
    private final WebClient webClient;
    private final PaymentRepository paymentRepository;
    private final BookingserviceRepository bookingserviceRepository;
    private final WebSocketService webSocketService;
    private final ServiceRepository serviceRepository;
    private final EmailService emailService;

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

        return createBookingInternal(
                customerEmail,
                request.getRoomTypeId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                DEFAULT_SINGLE_BOOKING_QUANTITY,
                request.getNumberOfAdults(),
                request.getNumberOfChildren(),
                request.getSpecialRequests(),
                request.getCheckInMethod(),
                BOOKING_TYPE_ONLINE);
    }

    @Override
    @Transactional
    public BookingResponse createGroupBooking(CreateGroupBookingRequest request, String customerEmail) {
        validateDates(request.getCheckInDate(), request.getCheckOutDate());
        validateGroupBookingRequest(request);

        return createBookingInternal(
                customerEmail,
                request.getRoomTypeId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                request.getQuantity(),
                request.getNumberOfAdults(),
                request.getNumberOfChildren(),
                request.getSpecialRequests(),
                request.getCheckInMethod(),
                BOOKING_TYPE_GROUP);
    }

    private BookingResponse createBookingInternal(
            String customerEmail,
            Integer roomTypeId,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int quantity,
            int numberOfAdults,
            int numberOfChildren,
            String specialRequests,
            String checkInMethod,
            String bookingType) {
        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        String normalizedCheckInMethod = normalizeCheckInMethod(checkInMethod);
        if (isFaceIdMethod(normalizedCheckInMethod)) {
            validateFaceIdBookingEligibility(customer);
        }
        if (isQrCodeMethod(normalizedCheckInMethod)) {
            validateQrCodeBookingEligibility(customer);
        }
        if ("FaceID".equalsIgnoreCase(normalizedCheckInMethod)) {
            boolean ekycVerified = ekycProfileRepository.existsByUseridAndStatus(
                    customer,
                    "Verified");
            boolean faceRegistered = faceembeddingRepository
                    .findEmbeddingTextByUserId(customer.getId())
                    .filter(embedding -> !embedding.isBlank())
                    .isPresent();

            if (!ekycVerified || !faceRegistered) {
                throw new RuntimeException(
                        "Bạn phải hoàn thành đăng ký eKYC và khuôn mặt trước khi chọn check-in bằng FaceID");
            }
        }

        Roomtype roomtype = roomtypeRepository.findByIdForUpdate(roomTypeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));

        if (!ACTIVE_STATUS.equalsIgnoreCase(roomtype.getStatus())) {
            throw new RuntimeException("Loại phòng hiện không hoạt động");
        }

        assertCapacity(roomtype, quantity, numberOfAdults, numberOfChildren);
        assertAvailability(roomtype, checkInDate, checkOutDate, quantity);

        Instant now = Instant.now();
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        BigDecimal totalAmount = roomtype.getBaseprice()
                .multiply(BigDecimal.valueOf(quantity))
                .multiply(BigDecimal.valueOf(nights));

        BigDecimal taxAmount = totalAmount.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal finalAmount = totalAmount.add(taxAmount).setScale(2, java.math.RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setUserid(customer);
        booking.setBookingreference(generateBookingReference(now));
        booking.setBookingtype(bookingType);
        booking.setCheckinmethod(normalizedCheckInMethod);
        booking.setTotalamount(totalAmount);
        booking.setPaidamount(BigDecimal.ZERO);
        booking.setDepositamount(BigDecimal.ZERO);
        booking.setDiscountamount(BigDecimal.ZERO);
        booking.setTaxamount(taxAmount);
        booking.setServicechargeamount(BigDecimal.ZERO);
        booking.setFinalamount(finalAmount);
        booking.setStatus(BOOKING_STATUS_CONFIRMED);
        booking.setSpecialrequests(specialRequests);
        booking.setCreatedat(now);
        booking.setUpdatedat(now);

        Booking savedBooking = bookingRepository.save(booking);

        Bookingdetail detail = new Bookingdetail();
        detail.setBookingid(savedBooking);
        detail.setRoomtypeid(roomtype);
        detail.setQuantity(quantity);
        detail.setExpectedcheckin(toInstant(checkInDate));
        detail.setExpectedcheckout(toInstant(checkOutDate));
        detail.setPriceatbooking(roomtype.getBaseprice());
        detail.setNumberofadults(numberOfAdults);
        detail.setNumberofchildren(numberOfChildren);
        detail.setStatus(DETAIL_STATUS_ACTIVE);
        detail.setCreatedat(now);
        detail.setUpdatedat(now);

        bookingdetailRepository.save(detail);

        return mapToResponse(savedBooking, detail, roomtype);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingHistoryResponse> getBookingHistory(String customerEmail) {
        userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return bookingdetailRepository.findBookingHistory(customerEmail).stream()
                .map(detail -> mapToHistoryResponse(detail, false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingDetail(Integer bookingId, String customerEmail) {
        Bookingdetail detail = bookingdetailRepository.findBookingDetail(bookingId, customerEmail)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy đặt phòng hoặc bạn không có quyền xem đặt phòng này"));

        return mapToResponse(detail.getBookingid(), detail, detail.getRoomtypeid());
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

    private void assertCapacity(Roomtype roomtype, int quantity, int numberOfAdults, int numberOfChildren) {
        int adultCapacity = safeInt(roomtype.getAdultcapacity()) * quantity;
        int childCapacity = safeInt(roomtype.getChildcapacity()) * quantity;

        if (numberOfAdults > adultCapacity) {
            throw new RuntimeException("Số người lớn vượt quá sức chứa của loại phòng");
        }

        if (numberOfChildren > childCapacity) {
            throw new RuntimeException("Số trẻ em vượt quá sức chứa của loại phòng");
        }
    }

    private void assertAvailability(Roomtype roomtype, LocalDate checkInDate, LocalDate checkOutDate, int quantity) {
        int totalRooms = Math.toIntExact(roomRepository.countByRoomtypeid_IdAndStatus(
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

            long bookedRooms = bookingdetailRepository.sumBookedQuantity(
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
            if (!bookingRepository.existsByBookingreference(reference)) {
                return reference;
            }
        }
        throw new RuntimeException("Không thể tạo mã đặt phòng");
    }

    private BookingResponse mapToResponse(Booking booking, Bookingdetail detail, Roomtype roomtype) {
        LocalDate checkInDate = toLocalDate(detail.getExpectedcheckin());
        LocalDate checkOutDate = toLocalDate(detail.getExpectedcheckout());
        Room assignedRoom = detail.getRoomid();
        boolean roomKeyUsable = isRoomKeyUsable(booking, detail);
        List<RoomAccessResponse> roomAccesses = mapRoomAccesses(booking, detail, true);

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingreference())
                .bookingType(booking.getBookingtype())
                .checkInMethod(booking.getCheckinmethod())
                .roomTypeId(roomtype.getId())
                .roomTypeName(roomtype.getName())
                .quantity(detail.getQuantity())
                .numberOfAdults(detail.getNumberofadults())
                .numberOfChildren(detail.getNumberofchildren())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .nights(ChronoUnit.DAYS.between(checkInDate, checkOutDate))
                .totalAmount(booking.getTotalamount())
                .finalAmount(booking.getFinalamount())
                .paidAmount(booking.getPaidamount())
                .depositAmount(booking.getDepositamount())
                .status(booking.getStatus())
                .specialRequests(booking.getSpecialrequests())
                .actualCheckIn(detail.getActualcheckin())
                .actualCheckOut(detail.getActualcheckout())
                .roomId(assignedRoom != null ? assignedRoom.getId() : null)
                .roomNumber(assignedRoom != null ? assignedRoom.getRoomnumber() : null)
                .roomPassword(roomKeyUsable ? detail.getRoomkeyaccess() : null)
                .roomKeyStatus(detail.getRoomkeystatus())
                .roomKeyGeneratedAt(detail.getRoomkeygeneratedat())
                .roomKeyExpiresAt(detail.getRoomkeyexpiredat())
                .roomAccesses(roomAccesses)
                .ekycIdentity(mapEkycIdentitySummary(booking.getUserid()))
                .createdAt(booking.getCreatedat())
                .build();
    }

    private EkycIdentitySummaryResponse mapEkycIdentitySummary(User customer) {
        if (customer == null) {
            return null;
        }

        return ekycProfileRepository.findTopByUseridOrderByCreatedatDesc(customer)
                .map(profile -> {
                    String idNumber = decryptOrNull(profile.getIdcardnumber());
                    String fullName = decryptOrNull(profile.getFullname());
                    String dateOfBirth = decryptOrNull(profile.getDateofbirth());
                    String gender = decryptOrNull(profile.getGender());
                    String hometown = decryptOrNull(profile.getHometown());
                    String provinceName = decryptOrNull(profile.getProvincename());

                    return EkycIdentitySummaryResponse.builder()
                            .status(profile.getStatus())
                            .fullName(fullName != null ? fullName : customer.getFullname())
                            .idNumber(maskIdNumber(idNumber))
                            .dateOfBirth(dateOfBirth)
                            .gender(gender)
                            .hometown(hometown != null ? hometown : provinceName)
                            .provinceCode(profile.getProvincecode())
                            .provinceName(provinceName)
                            .verifiedAt(profile.getVerifiedat())
                            .frontImage(signedUrlOrNull(profile.getFrontimage()))
                            .backImage(signedUrlOrNull(profile.getBackimage()))
                            .faceImage(signedUrlOrNull(profile.getFaceimage()))
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
            Bookingdetail detail,
            boolean includeRoomPassword) {
        Booking booking = detail.getBookingid();
        Roomtype roomtype = detail.getRoomtypeid();
        Room assignedRoom = detail.getRoomid();
        LocalDate checkInDate = toLocalDate(detail.getExpectedcheckin());
        LocalDate checkOutDate = toLocalDate(detail.getExpectedcheckout());
        boolean roomKeyUsable = includeRoomPassword && isRoomKeyUsable(booking, detail);
        List<RoomAccessResponse> roomAccesses = mapRoomAccesses(
                booking,
                detail,
                includeRoomPassword);

        return BookingHistoryResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingreference())
                .bookingDate(booking.getCreatedat())
                .checkInMethod(booking.getCheckinmethod())
                .roomTypeId(roomtype.getId())
                .roomType(roomtype.getName())
                .quantity(detail.getQuantity())
                .numberOfAdults(detail.getNumberofadults())
                .numberOfChildren(detail.getNumberofchildren())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .nights(ChronoUnit.DAYS.between(checkInDate, checkOutDate))
                .totalAmount(booking.getFinalamount())
                .status(booking.getStatus())
                .guestName(booking.getUserid() != null ? booking.getUserid().getFullname() : "Khách hàng Elysian")
                .guestEmail(booking.getUserid() != null ? booking.getUserid().getEmail() : "")
                .actualCheckIn(detail.getActualcheckin())
                .actualCheckOut(detail.getActualcheckout())
                .roomNumber(assignedRoom != null ? assignedRoom.getRoomnumber() : null)
                .roomPassword(roomKeyUsable ? detail.getRoomkeyaccess() : null)
                .roomKeyStatus(detail.getRoomkeystatus())
                .roomKeyExpiresAt(detail.getRoomkeyexpiredat())
                .roomAccesses(roomAccesses)
                .ekycIdentity(includeRoomPassword && isFaceIdMethod(booking.getCheckinmethod())
                        ? mapEkycIdentitySummary(booking.getUserid())
                        : null)
                .paidAmount(booking.getPaidamount())
                .depositAmount(booking.getDepositamount())
                .serviceChargeAmount(booking.getServicechargeamount())
                .taxAmount(booking.getTaxamount())
                .discountAmount(booking.getDiscountamount())
                .finalAmount(booking.getFinalamount())
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

        if (!"Confirmed".equalsIgnoreCase(booking.getStatus())
                && !"Paid".equalsIgnoreCase(booking.getStatus())
                && !"Partially Paid".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Đơn đặt phòng không ở trạng thái có thể nhận phòng");
        }

        Bookingdetail detail = bookingdetailRepository.findByBookingid_Id(bookingId)
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

        Bookingdetail detail = bookingdetailRepository.findByBookingid_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));
        Booking booking = detail.getBookingid();
        User customer = booking.getUserid();
        if (customer == null) {
            throw new RuntimeException("Booking chưa được liên kết với tài khoản khách hàng");
        }

        if (!"Confirmed".equalsIgnoreCase(booking.getStatus())
                && !"Paid".equalsIgnoreCase(booking.getStatus())
                && !"Partially Paid".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Đơn đặt phòng không ở trạng thái có thể nhận phòng");
        }
        if (!"Face Recognition".equalsIgnoreCase(booking.getCheckinmethod())
                && !"FaceID".equalsIgnoreCase(booking.getCheckinmethod())) {
            throw new RuntimeException("Đơn đặt phòng này không sử dụng phương thức FaceID");
        }
        validateCheckInDateWindow(detail);

        if (!ekycProfileRepository.existsByUseridAndStatus(customer, "Verified")) {
            throw new RuntimeException(
                    "Bạn chưa hoàn thành đăng ký eKYC nên chưa thể check-in bằng FaceID");
        }

        String registeredEmbedding = faceembeddingRepository
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
        Bookingdetail detail = bookingdetailRepository.findBookingDetail(bookingId, customerEmail)
                .orElseThrow(() -> new RuntimeException("Khong tim thay booking QR Code cua ban"));
        Booking booking = detail.getBookingid();
        User customer = booking.getUserid();
        if (customer == null) {
            throw new RuntimeException("Booking chua duoc lien ket voi tai khoan khach hang");
        }

        validateQrBookingForCheckIn(booking, detail, customer);

        Instant now = Instant.now();
        if (hasActiveQrToken(detail, now)) {
            String existingToken = detail.getQrcodevalue();
            Instant existingExpiresAt = detail.getQrcodeexpiredat();
            sendQrCheckInEmailSafely(customer, booking, detail, existingToken, existingExpiresAt);
            return buildQrTokenResponse(booking, detail, existingToken, existingExpiresAt);
        }

        Instant expiresAt = calculateQrTokenExpiry(now, detail);
        String token = generateUniqueQrToken();

        detail.setQrcodevalue(token);
        detail.setQrcodegeneratedat(now);
        detail.setQrcodeexpiredat(expiresAt);
        detail.setUpdatedat(now);
        bookingdetailRepository.save(detail);

        sendQrCheckInEmailSafely(customer, booking, detail, token, expiresAt);

        return buildQrTokenResponse(booking, detail, token, expiresAt);
    }

    private QrTokenResponse buildQrTokenResponse(
            Booking booking,
            Bookingdetail detail,
            String token,
            Instant expiresAt
    ) {
        return QrTokenResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingreference())
                .token(token)
                .qrPayload(token)
                .generatedAt(detail.getQrcodegeneratedat())
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
        Bookingdetail detail = bookingdetailRepository.findByQrcodevalueForUpdate(token)
                .orElseThrow(() -> new RuntimeException("Ma QR check-in khong hop le hoac da het hieu luc"));

        Instant now = Instant.now();
        if (detail.getQrcodeexpiredat() == null || !now.isBefore(detail.getQrcodeexpiredat())) {
            clearQrToken(detail, now);
            bookingdetailRepository.save(detail);
            throw new RuntimeException("Ma QR check-in da het hieu luc");
        }

        Booking booking = detail.getBookingid();
        User customer = booking.getUserid();
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
            Bookingdetail detail,
            User checkedInBy) {
        int quantity = detail.getQuantity() == null
                ? DEFAULT_SINGLE_BOOKING_QUANTITY
                : detail.getQuantity();
        if (quantity <= 0) {
            throw new RuntimeException("Số lượng phòng của booking không hợp lệ");
        }

        List<BookingRoomAccess> existingAccesses = bookingRoomAccessRepository
                .findByBookingid_IdOrderByRoomid_RoomnumberAsc(
                        booking.getId());
        if (!existingAccesses.isEmpty()) {
            throw new RuntimeException("Booking này đã được cấp quyền truy cập phòng");
        }

        List<Room> availableRooms = roomRepository
                .findByRoomtypeid_IdAndStatusOrderByRoomnumberAsc(
                        detail.getRoomtypeid().getId(),
                        AVAILABLE_ROOM_STATUS);

        List<Room> selectedRooms = new ArrayList<>();
        if (detail.getRoomid() != null) {
            Room assignedRoom = detail.getRoomid();
            if (!detail.getRoomtypeid().getId().equals(assignedRoom.getRoomtypeid().getId())) {
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
        Instant keyExpiresAt = toCheckoutExpiry(detail.getExpectedcheckout());
        Set<String> generatedPasswords = new HashSet<>();
        List<BookingRoomAccess> roomAccesses = new ArrayList<>();

        for (Room room : selectedRooms) {
            String password = generateUniqueRoomPassword(generatedPasswords);

            BookingRoomAccess access = new BookingRoomAccess();
            access.setBookingid(booking);
            access.setRoomid(room);
            access.setRoomkeyaccess(password);
            access.setRoomkeygeneratedat(now);
            access.setRoomkeyexpiredat(keyExpiresAt);
            access.setRoomkeystatus(ROOM_KEY_STATUS_ACTIVE);
            access.setCreatedat(now);
            access.setUpdatedat(now);
            roomAccesses.add(access);

            room.setStatus(ROOM_STATUS_OCCUPIED);
            room.setUpdatedat(now);

            // Broadcast room status change via WebSocket
            webSocketService.broadcastRoomStatus(room.getId(), room.getRoomnumber(), ROOM_STATUS_OCCUPIED);
        }

        BookingRoomAccess primaryAccess = roomAccesses.get(0);
        detail.setRoomid(primaryAccess.getRoomid());
        detail.setRoomkeyaccess(primaryAccess.getRoomkeyaccess());
        detail.setRoomkeygeneratedat(now);
        detail.setRoomkeyexpiredat(keyExpiresAt);
        detail.setRoomkeystatus(ROOM_KEY_STATUS_ACTIVE);
        detail.setActualcheckin(now);
        detail.setCheckedinat(now);
        detail.setCheckedinby(checkedInBy);
        clearQrToken(detail, now);
        detail.setUpdatedat(now);

        booking.setStatus("Checked-in");
        booking.setUpdatedat(now);

        roomRepository.saveAll(selectedRooms);
        bookingRoomAccessRepository.saveAll(roomAccesses);
        bookingRepository.save(booking);
        bookingdetailRepository.save(detail);

        return mapToResponse(booking, detail, detail.getRoomtypeid());
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

        if (!"Checked-in".equalsIgnoreCase(booking.getStatus())
                && !"Checked In".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Đơn đặt phòng chưa được check-in");
        }

        BigDecimal dueAmount = booking.getFinalamount().subtract(booking.getPaidamount());
        if (dueAmount.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Đơn đặt phòng chưa được thanh toán đầy đủ. Quý khách cần thanh toán thêm "
                    + formatCurrency(dueAmount) + " trước khi trả phòng.");
        }

        booking.setStatus("Completed");
        booking.setUpdatedat(Instant.now());
        bookingRepository.save(booking);

        Bookingdetail detail = bookingdetailRepository.findByBookingid_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));
        Instant now = Instant.now();
        detail.setActualcheckout(now);
        detail.setCheckedoutat(now);
        detail.setCheckedoutby(staff);
        List<BookingRoomAccess> roomAccesses = bookingRoomAccessRepository
                .findByBookingid_IdOrderByRoomid_RoomnumberAsc(bookingId);
        for (BookingRoomAccess access : roomAccesses) {
            Room room = access.getRoomid();
            room.setStatus("Cleaning");
            room.setUpdatedat(now);
            access.setRoomkeyaccess(null);
            access.setRoomkeyexpiredat(now);
            access.setRoomkeystatus(ROOM_KEY_STATUS_EXPIRED);
            access.setUpdatedat(now);

            // Broadcast room status change via WebSocket
            webSocketService.broadcastRoomStatus(room.getId(), room.getRoomnumber(), "Cleaning");
        }
        if (!roomAccesses.isEmpty()) {
            roomRepository.saveAll(roomAccesses.stream()
                    .map(BookingRoomAccess::getRoomid)
                    .toList());
            bookingRoomAccessRepository.saveAll(roomAccesses);
        }

        detail.setRoomkeyaccess(null);
        detail.setRoomkeyexpiredat(now);
        detail.setRoomkeystatus(ROOM_KEY_STATUS_EXPIRED);
        detail.setUpdatedat(now);

        if (roomAccesses.isEmpty() && detail.getRoomid() != null) {
            Room room = detail.getRoomid();
            room.setStatus("Cleaning");
            room.setUpdatedat(now);
            roomRepository.save(room);

            // Broadcast room status change via WebSocket
            webSocketService.broadcastRoomStatus(room.getId(), room.getRoomnumber(), "Cleaning");
        }
        bookingdetailRepository.save(detail);

        return mapToResponse(booking, detail, detail.getRoomtypeid());
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

    private void validateCheckInDateWindow(Bookingdetail detail) {
        LocalDate today = LocalDate.now(HOTEL_ZONE);
        LocalDate checkInDate = toLocalDate(detail.getExpectedcheckin());
        LocalDate checkOutDate = toLocalDate(detail.getExpectedcheckout());

        if (today.isBefore(checkInDate)) {
            throw new RuntimeException("Chưa đến ngày nhận phòng");
        }
        if (!today.isBefore(checkOutDate)) {
            throw new RuntimeException("Đơn đặt phòng đã quá thời gian nhận phòng");
        }
    }

    private void validateFaceIdBookingEligibility(User customer) {
        boolean ekycVerified = ekycProfileRepository.existsByUseridAndStatus(
                customer,
                "Verified"
        );
        boolean faceRegistered = faceembeddingRepository
                .findEmbeddingTextByUserId(customer.getId())
                .filter(embedding -> !embedding.isBlank())
                .isPresent();

        if (!ekycVerified || !faceRegistered) {
            throw new RuntimeException(
                    "Ban phai hoan thanh dang ky eKYC va khuon mat truoc khi chon check-in bang FaceID"
            );
        }
    }

    private void validateQrCodeBookingEligibility(User customer) {
        if (!ekycProfileRepository.existsByUseridAndStatus(customer, "Verified")) {
            throw new RuntimeException(
                    "Ban phai hoan thanh dang ky eKYC truoc khi chon check-in bang QR Code"
            );
        }
    }

    private void validateQrBookingForCheckIn(
            Booking booking,
            Bookingdetail detail,
            User customer
    ) {
        if (!isQrCodeMethod(booking.getCheckinmethod())) {
            throw new RuntimeException("Don dat phong nay khong su dung phuong thuc QR Code");
        }
        validateCheckInEligibleStatus(booking);
        validateBookingNotCheckedIn(booking, detail);
        validateCheckInDateWindow(detail);
        validateQrCodeBookingEligibility(customer);
    }

    private void validateCheckInEligibleStatus(Booking booking) {
        if (!"Confirmed".equalsIgnoreCase(booking.getStatus()) &&
                !"Partially Paid".equalsIgnoreCase(booking.getStatus()) &&
                !"Paid".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Don dat phong khong o trang thai co the nhan phong");
        }
    }

    private void validateBookingNotCheckedIn(Booking booking, Bookingdetail detail) {
        if (detail.getActualcheckin() != null
                || "Checked-in".equalsIgnoreCase(booking.getStatus())
                || "Checked In".equalsIgnoreCase(booking.getStatus())) {
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

    private Instant calculateQrTokenExpiry(Instant generatedAt, Bookingdetail detail) {
        long ttlMinutes = Math.max(1, qrTokenTtlMinutes);
        Instant expiresAt = generatedAt.plus(Duration.ofMinutes(ttlMinutes));
        Instant checkoutExpiry = toCheckoutExpiry(detail.getExpectedcheckout());
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
            if (!bookingdetailRepository.existsByQrcodevalue(token)) {
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

    private boolean hasActiveQrToken(Bookingdetail detail, Instant now) {
        return detail.getQrcodevalue() != null
                && !detail.getQrcodevalue().isBlank()
                && detail.getQrcodeexpiredat() != null
                && now.isBefore(detail.getQrcodeexpiredat());
    }

    private void sendQrCheckInEmailSafely(
            User customer,
            Booking booking,
            Bookingdetail detail,
            String token,
            Instant expiresAt
    ) {
        if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
            return;
        }

        try {
            Roomtype roomtype = detail.getRoomtypeid();
            emailService.sendQrCheckInEmail(
                    customer.getEmail(),
                    customer.getFullname(),
                    booking.getBookingreference(),
                    roomtype != null ? roomtype.getName() : null,
                    detail.getExpectedcheckin() != null ? toLocalDate(detail.getExpectedcheckin()) : null,
                    detail.getExpectedcheckout() != null ? toLocalDate(detail.getExpectedcheckout()) : null,
                    token,
                    expiresAt
            );
        } catch (RuntimeException ex) {
            log.warn("Failed to send QR check-in email for booking {}", booking.getId(), ex);
        }
    }

    private void clearQrToken(Bookingdetail detail, Instant now) {
        detail.setQrcodevalue(null);
        detail.setQrcodegeneratedat(null);
        detail.setQrcodeexpiredat(null);
        detail.setUpdatedat(now);
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
            Bookingdetail detail,
            boolean includeRoomPassword) {
        List<BookingRoomAccess> accesses = bookingRoomAccessRepository.findByBookingid_IdOrderByRoomid_RoomnumberAsc(
                booking.getId());
        return mapRoomAccesses(booking, detail, accesses, includeRoomPassword);
    }

    private List<RoomAccessResponse> mapRoomAccesses(
            Booking booking,
            Bookingdetail detail,
            List<BookingRoomAccess> accesses,
            boolean includeRoomPassword) {
        if (accesses != null && !accesses.isEmpty()) {
            return accesses.stream()
                    .map(access -> {
                        boolean usable = includeRoomPassword
                                && isRoomAccessUsable(booking, access);
                        Room room = access.getRoomid();
                        return RoomAccessResponse.builder()
                                .roomId(room.getId())
                                .roomNumber(room.getRoomnumber())
                                .floorNumber(room.getFloornumber())
                                .roomPassword(usable ? access.getRoomkeyaccess() : null)
                                .roomKeyStatus(access.getRoomkeystatus())
                                .roomKeyExpiresAt(access.getRoomkeyexpiredat())
                                .build();
                    })
                    .toList();
        }

        if (detail != null && detail.getRoomid() != null) {
            Room room = detail.getRoomid();
            boolean usable = includeRoomPassword && isRoomKeyUsable(booking, detail);
            return List.of(RoomAccessResponse.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomnumber())
                    .floorNumber(room.getFloornumber())
                    .roomPassword(usable ? detail.getRoomkeyaccess() : null)
                    .roomKeyStatus(detail.getRoomkeystatus())
                    .roomKeyExpiresAt(detail.getRoomkeyexpiredat())
                    .build());
        }

        return List.of();
    }

    private boolean isRoomAccessUsable(
            Booking booking,
            BookingRoomAccess access) {
        return ("Checked-in".equalsIgnoreCase(booking.getStatus())
                || "Checked In".equalsIgnoreCase(booking.getStatus()))
                && ROOM_KEY_STATUS_ACTIVE.equalsIgnoreCase(access.getRoomkeystatus())
                && access.getRoomkeyaccess() != null
                && !access.getRoomkeyaccess().isBlank()
                && access.getRoomkeyexpiredat() != null
                && Instant.now().isBefore(access.getRoomkeyexpiredat());
    }

    private boolean isRoomKeyUsable(Booking booking, Bookingdetail detail) {
        return ("Checked-in".equalsIgnoreCase(booking.getStatus())
                || "Checked In".equalsIgnoreCase(booking.getStatus()))
                && detail.getActualcheckin() != null
                && detail.getActualcheckout() == null
                && ROOM_KEY_STATUS_ACTIVE.equalsIgnoreCase(detail.getRoomkeystatus())
                && detail.getRoomkeyaccess() != null
                && !detail.getRoomkeyaccess().isBlank()
                && detail.getRoomkeyexpiredat() != null
                && Instant.now().isBefore(detail.getRoomkeyexpiredat());
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

        return bookingdetailRepository.findAll().stream()
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
            customer = new User();
            customer.setEmail(request.getCustomerEmail());
            customer.setFullname(request.getCustomerFullname());
            customer.setPhonenumber(request.getCustomerPhonenumber());
            customer.setRole(Role.customer);
            customer.setPasswordhash("");
            customer.setStatus("Active");
            customer.setCreatedat(Instant.now());
            customer = userRepository.save(customer);
        }

        Roomtype roomtype = roomtypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));

        if (!"Active".equalsIgnoreCase(roomtype.getStatus())) {
            throw new RuntimeException("Loại phòng hiện không hoạt động");
        }

        assertCapacity(roomtype, request.getQuantity(), request.getNumberOfAdults(), request.getNumberOfChildren());
        assertAvailability(roomtype, request.getCheckInDate(), request.getCheckOutDate(), request.getQuantity());

        Instant now = Instant.now();
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = roomtype.getBaseprice()
                .multiply(BigDecimal.valueOf(request.getQuantity()))
                .multiply(BigDecimal.valueOf(nights));

        BigDecimal taxAmount = totalAmount.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = totalAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setUserid(customer);
        booking.setBookingreference(generateBookingReference(now));
        booking.setBookingtype("Walk-in");
        booking.setCheckinmethod("Manual");
        booking.setTotalamount(totalAmount);
        booking.setPaidamount(BigDecimal.ZERO);
        booking.setDepositamount(BigDecimal.ZERO);
        booking.setDiscountamount(BigDecimal.ZERO);
        booking.setTaxamount(taxAmount);
        booking.setServicechargeamount(BigDecimal.ZERO);
        booking.setFinalamount(finalAmount);
        booking.setStatus("Confirmed");
        booking.setSpecialrequests(request.getSpecialRequests());
        booking.setCreatedat(now);
        booking.setUpdatedat(now);
        booking.setCreatedby(staff);

        Booking savedBooking = bookingRepository.save(booking);

        Bookingdetail detail = new Bookingdetail();
        detail.setBookingid(savedBooking);
        detail.setRoomtypeid(roomtype);
        detail.setQuantity(request.getQuantity());
        detail.setExpectedcheckin(toInstant(request.getCheckInDate()));
        detail.setExpectedcheckout(toInstant(request.getCheckOutDate()));
        detail.setPriceatbooking(roomtype.getBaseprice());
        detail.setNumberofadults(request.getNumberOfAdults());
        detail.setNumberofchildren(request.getNumberOfChildren());
        detail.setStatus("Active");
        detail.setCreatedat(now);
        detail.setUpdatedat(now);

        bookingdetailRepository.save(detail);

        // Record upfront payment if any
        if (request.getPaidAmount() != null && request.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            Payment payment = new Payment();
            payment.setBookingid(savedBooking);
            payment.setAmount(request.getPaidAmount());
            payment.setPaymentmethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "Cash");
            payment.setPaymenttype("Booking Payment");
            payment.setTransactioncode("WALKIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            payment.setStatus("Completed");
            payment.setRefundedamount(BigDecimal.ZERO);
            payment.setPaymentdate(now);
            payment.setNotes("Walk-in payment recorded at creation.");
            paymentRepository.save(payment);

            savedBooking.setPaidamount(request.getPaidAmount());
            if (savedBooking.getPaidamount().compareTo(savedBooking.getFinalamount()) >= 0) {
                savedBooking.setStatus("Paid");
            } else {
                savedBooking.setStatus("Partially Paid");
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

        if (!isStaff && !actor.getId().equals(booking.getUserid().getId())) {
            throw new RuntimeException("Bạn không có quyền xem hóa đơn này");
        }

        Bookingdetail detail = bookingdetailRepository.findByBookingid_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));

        List<BookingRoomAccess> roomAccesses = bookingRoomAccessRepository
                .findByBookingid_IdOrderByRoomid_RoomnumberAsc(bookingId);
        List<String> assignedRooms = new ArrayList<>();
        if (!roomAccesses.isEmpty()) {
            assignedRooms = roomAccesses.stream()
                    .map(access -> access.getRoomid().getRoomnumber())
                    .toList();
        } else if (detail.getRoomid() != null) {
            assignedRooms = List.of(detail.getRoomid().getRoomnumber());
        }

        List<Bookingservice> bookingservices = bookingserviceRepository.findByBookingid_Id(bookingId);
        List<InvoiceResponse.ServiceChargeItem> serviceItems = bookingservices.stream()
                .map(bs -> InvoiceResponse.ServiceChargeItem.builder()
                        .usageId(bs.getId())
                        .serviceName(bs.getNote() != null && !bs.getNote().isBlank() ? bs.getNote() : "Dịch vụ phụ thu")
                        .quantity(bs.getQuantity())
                        .unitPrice(bs.getUnitprice())
                        .totalPrice(bs.getTotalprice())
                        .implementedAt(bs.getImplementedat())
                        .note(bs.getNote())
                        .build())
                .toList();

        BigDecimal serviceTotal = bookingservices.stream()
                .map(Bookingservice::getTotalprice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Payment> payments = paymentRepository.findByBookingid_Id(bookingId);
        List<InvoiceResponse.PaymentItem> paymentItems = payments.stream()
                .map(p -> InvoiceResponse.PaymentItem.builder()
                        .paymentId(p.getId())
                        .amount(p.getAmount())
                        .paymentMethod(p.getPaymentmethod())
                        .paymentType(p.getPaymenttype())
                        .transactionCode(p.getTransactioncode())
                        .status(p.getStatus())
                        .refundedAmount(p.getRefundedamount())
                        .paymentDate(p.getPaymentdate())
                        .notes(p.getNotes())
                        .build())
                .toList();

        long nights = ChronoUnit.DAYS.between(toLocalDate(detail.getExpectedcheckin()),
                toLocalDate(detail.getExpectedcheckout()));
        BigDecimal roomRate = detail.getPriceatbooking();
        BigDecimal roomTotal = roomRate.multiply(BigDecimal.valueOf(detail.getQuantity()))
                .multiply(BigDecimal.valueOf(nights));
        BigDecimal taxAmount = booking.getTaxamount();
        BigDecimal discountAmount = booking.getDiscountamount();
        BigDecimal finalAmount = booking.getFinalamount();
        BigDecimal paidAmount = booking.getPaidamount();
        BigDecimal dueAmount = finalAmount.subtract(paidAmount);

        return InvoiceResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingreference())
                .bookingType(booking.getBookingtype())
                .customerName(booking.getUserid().getFullname())
                .customerEmail(booking.getUserid().getEmail())
                .customerPhone(booking.getUserid().getPhonenumber())
                .checkInDate(toLocalDate(detail.getExpectedcheckin()))
                .checkOutDate(toLocalDate(detail.getExpectedcheckout()))
                .nights(nights)
                .roomTypeName(detail.getRoomtypeid().getName())
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

        if ("Cancelled".equalsIgnoreCase(booking.getStatus())
                || "Checked-out".equalsIgnoreCase(booking.getStatus())
                || "Completed".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Không thể thêm dịch vụ cho đơn đặt phòng ở trạng thái này");
        }

        com.example.hotelsmartbookingbackend.entity.Service service = serviceRepository.findById(request.getServiceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));

        if (Boolean.FALSE.equals(service.getIsactive())) {
            throw new RuntimeException("Dịch vụ hiện không hoạt động");
        }

        BigDecimal unitPrice = service.getPrice();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        Bookingservice usage = new Bookingservice();
        usage.setBookingid(booking);
        usage.setServiceid(service);
        usage.setImplementedby(staff);
        usage.setQuantity(request.getQuantity());
        usage.setUnitprice(unitPrice);
        usage.setTotalprice(totalPrice);
        usage.setImplementedat(Instant.now());
        usage.setNote(service.getName() + (request.getNote() != null && !request.getNote().isBlank()
                ? " (" + request.getNote() + ")"
                : ""));
        usage.setStatus("Active");

        bookingserviceRepository.save(usage);

        // Update booking service charge, tax, and final amounts
        BigDecimal serviceTax = totalPrice.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        booking.setServicechargeamount(booking.getServicechargeamount().add(totalPrice));
        booking.setTaxamount(booking.getTaxamount().add(serviceTax));
        booking.setFinalamount(booking.getFinalamount().add(totalPrice).add(serviceTax));

        if (booking.getPaidamount().compareTo(booking.getFinalamount()) < 0) {
            if ("Paid".equalsIgnoreCase(booking.getStatus())) {
                booking.setStatus("Partially Paid");
            }
        }

        booking.setUpdatedat(Instant.now());

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
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "createdat";
        Sort.Direction direction = criteria.getSortDirection() != null
                && criteria.getSortDirection().equalsIgnoreCase("DESC")
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        Page<Booking> bookings = bookingRepository.findAll(new BookingSpecification(criteria), pageable);

        List<Booking> bookingList = bookings.getContent();
        List<Integer> bookingIds = bookingList.stream().map(Booking::getId).toList();

        List<Bookingdetail> detailsList = bookingdetailRepository.findByBookingid_IdIn(bookingIds);
        java.util.Map<Integer, Bookingdetail> detailsMap = new java.util.HashMap<>();
        for (Bookingdetail detail : detailsList) {
            if (detail.getBookingid() != null) {
                detailsMap.put(detail.getBookingid().getId(), detail);
            }
        }

        List<BookingRoomAccess> accessesList = bookingRoomAccessRepository.findByBookingid_IdIn(bookingIds);
        java.util.Map<Integer, List<BookingRoomAccess>> accessesMap = new java.util.HashMap<>();
        for (BookingRoomAccess access : accessesList) {
            if (access.getBookingid() != null) {
                accessesMap.computeIfAbsent(access.getBookingid().getId(), k -> new java.util.ArrayList<>())
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

        if (booking.getStatus().equalsIgnoreCase("Cancelled") || booking.getStatus().equalsIgnoreCase("Checked Out")) {
            throw new RuntimeException("Không thể cập nhật đơn đặt phòng đã hủy hoặc đã trả phòng");
        }

        booking.setSpecialrequests(request.getSpecialRequests());

        if (request.getDiscountAmount() != null && request.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
            booking.setDiscountamount(request.getDiscountAmount());
            BigDecimal newFinalAmount = booking.getTotalamount()
                    .subtract(request.getDiscountAmount())
                    .subtract(booking.getTaxamount());
            booking.setFinalamount(newFinalAmount);
        }

        booking.setUpdatedat(Instant.now());
        Booking updatedBooking = bookingRepository.save(booking);

        return mapToBookingResponse(updatedBooking);
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Integer bookingId, CancelBookingRequest request, String staffEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng với ID: " + bookingId));

        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (booking.getStatus().equalsIgnoreCase("Cancelled")) {
            throw new RuntimeException("Đơn đặt phòng này đã được hủy trước đó");
        }

        if (booking.getStatus().equalsIgnoreCase("Checked Out")) {
            throw new RuntimeException("Không thể hủy đơn đặt phòng đã trả phòng");
        }

        booking.setStatus("Cancelled");
        booking.setCancellationreason(request.getCancellationReason());
        booking.setCancelledat(Instant.now());
        booking.setCancelledby(staff);
        booking.setUpdatedat(Instant.now());

        Bookingdetail detail = bookingdetailRepository
                .findByBookingid_Id(bookingId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy chi tiết đơn đặt phòng"));

        detail.setStatus("Cancelled");
        detail.setUpdatedat(Instant.now());
        bookingdetailRepository.save(detail);

        Booking cancelledBooking = bookingRepository.save(booking);

        return mapToBookingResponse(cancelledBooking);
    }

    private BookingHistoryResponse mapToBookingHistoryResponse(Booking booking) {
        Bookingdetail detail = bookingdetailRepository.findByBookingid_Id(booking.getId()).orElse(null);
        return mapToBookingHistoryResponse(booking, detail, null);
    }

    private BookingHistoryResponse mapToBookingHistoryResponse(Booking booking, Bookingdetail detail) {
        return mapToBookingHistoryResponse(booking, detail, null);
    }

    private BookingHistoryResponse mapToBookingHistoryResponse(Booking booking, Bookingdetail detail,
            List<BookingRoomAccess> accesses) {
        BookingHistoryResponse.BookingHistoryResponseBuilder builder = BookingHistoryResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingreference())
                .bookingDate(booking.getCreatedat())
                .status(booking.getStatus())
                .checkInMethod(booking.getCheckinmethod())
                .totalAmount(booking.getTotalamount())
                .paidAmount(booking.getPaidamount())
                .depositAmount(booking.getDepositamount())
                .discountAmount(booking.getDiscountamount())
                .taxAmount(booking.getTaxamount())
                .serviceChargeAmount(booking.getServicechargeamount())
                .finalAmount(booking.getFinalamount())
                .guestName(booking.getUserid() != null ? booking.getUserid().getFullname() : "Khách hàng Elysian")
                .guestEmail(booking.getUserid() != null ? booking.getUserid().getEmail() : "");

        if (detail != null) {
            Roomtype roomtype = detail.getRoomtypeid();
            Room assignedRoom = detail.getRoomid();
            LocalDate checkInDate = toLocalDate(detail.getExpectedcheckin());
            LocalDate checkOutDate = toLocalDate(detail.getExpectedcheckout());

            builder.roomTypeId(roomtype != null ? roomtype.getId() : null)
                    .roomType(roomtype != null ? roomtype.getName() : null)
                    .quantity(detail.getQuantity())
                    .numberOfAdults(detail.getNumberofadults())
                    .numberOfChildren(detail.getNumberofchildren())
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .nights(ChronoUnit.DAYS.between(checkInDate, checkOutDate))
                    .actualCheckIn(detail.getActualcheckin())
                    .actualCheckOut(detail.getActualcheckout())
                    .roomNumber(assignedRoom != null ? assignedRoom.getRoomnumber() : null)
                    .roomPassword(isRoomKeyUsable(booking, detail) ? detail.getRoomkeyaccess() : null)
                    .roomKeyStatus(detail.getRoomkeystatus())
                    .roomKeyExpiresAt(detail.getRoomkeyexpiredat())
                    .roomAccesses(mapRoomAccesses(booking, detail, accesses, true));
        }

        return builder.build();
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingreference())
                .bookingType(booking.getBookingtype())
                .checkInMethod(booking.getCheckinmethod())
                .totalAmount(booking.getTotalamount())
                .finalAmount(booking.getFinalamount())
                .paidAmount(booking.getPaidamount())
                .status(booking.getStatus())
                .specialRequests(booking.getSpecialrequests())
                .createdAt(booking.getCreatedat())
                .build();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0 VND";
        }
        return String.format("%,.0f VND", amount);
    }
}
