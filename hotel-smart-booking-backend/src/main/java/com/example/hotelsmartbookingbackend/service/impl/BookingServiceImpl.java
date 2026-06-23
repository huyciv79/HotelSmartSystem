package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.BookingFilter;
import com.example.hotelsmartbookingbackend.dto.request.UpdateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CancelBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceVerificationResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.dto.response.PageResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomAccessResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.BookingRoomAccess;
import com.example.hotelsmartbookingbackend.entity.Bookingdetail;
import com.example.hotelsmartbookingbackend.entity.Room;
import com.example.hotelsmartbookingbackend.entity.Roomtype;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.BookingRoomAccessRepository;
import com.example.hotelsmartbookingbackend.repository.BookingdetailRepository;
import com.example.hotelsmartbookingbackend.repository.EkycProfileRepository;
import com.example.hotelsmartbookingbackend.repository.FaceembeddingRepository;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomtypeRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.BookingService;
import com.example.hotelsmartbookingbackend.specification.BookingSpecification;
import lombok.RequiredArgsConstructor;
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
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final List<String> INVENTORY_HOLDING_BOOKING_STATUSES =
            List.of("Pending", "Confirmed", "Checked In");
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
    private final WebClient webClient;

    @Value("${ai.service.face-verify-url:http://localhost:8000/api/v1/face/verify}")
    private String aiFaceVerifyUrl;

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
        booking.setCheckinmethod(normalizeCheckInMethod(checkInMethod));
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
                .createdAt(booking.getCreatedat())
                .build();
    }

    private BookingHistoryResponse mapToHistoryResponse(
            Bookingdetail detail,
            boolean includeRoomPassword
    ) {
        Booking booking = detail.getBookingid();
        Roomtype roomtype = detail.getRoomtypeid();
        Room assignedRoom = detail.getRoomid();
        LocalDate checkInDate = toLocalDate(detail.getExpectedcheckin());
        LocalDate checkOutDate = toLocalDate(detail.getExpectedcheckout());
        boolean roomKeyUsable = includeRoomPassword && isRoomKeyUsable(booking, detail);
        List<RoomAccessResponse> roomAccesses = mapRoomAccesses(
                booking,
                detail,
                includeRoomPassword
        );

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
                .build();
    }

    @Override
    @Transactional
    public BookingResponse performCheckIn(Integer bookingId, String staffEmail) {
        User staff = userRepository.findByEmail(staffEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name()) && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        if (!"Confirmed".equalsIgnoreCase(booking.getStatus())) {
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
            MultipartFile leftImage,
            MultipartFile rightImage,
            MultipartFile upImage,
            MultipartFile downImage,
            String actorEmail
    ) {
        validateSelfie(selfieImage);
        validateSelfie(leftImage);
        validateSelfie(rightImage);
        validateSelfie(upImage);
        validateSelfie(downImage);

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

        if (!"Confirmed".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Đơn đặt phòng không ở trạng thái có thể nhận phòng");
        }
        if (!"Face Recognition".equalsIgnoreCase(booking.getCheckinmethod())
                && !"FaceID".equalsIgnoreCase(booking.getCheckinmethod())) {
            throw new RuntimeException("Đơn đặt phòng này không sử dụng phương thức FaceID");
        }
        validateFaceCheckInDate(detail);

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
                leftImage,
                rightImage,
                upImage,
                downImage
        );
        if (!Boolean.TRUE.equals(verification.getLivenessPassed())) {
            String livenessMessage = verification.getMessage() != null
                    && !verification.getMessage().isBlank()
                    ? verification.getMessage()
                    : "Không vượt qua kiểm tra liveness";
            throw new RuntimeException(
                    livenessMessage
                            + " Vui lòng dùng khuôn mặt thật trước camera, không dùng ảnh hoặc màn hình");
        }
        if (!Boolean.TRUE.equals(verification.getVerified())
                || !Boolean.TRUE.equals(verification.getMatched())) {
            throw new RuntimeException("Khuôn mặt không khớp với hồ sơ eKYC đã đăng ký");
        }

        return completeCheckIn(booking, detail, actor);
    }

    private BookingResponse completeCheckIn(
            Booking booking,
            Bookingdetail detail,
            User checkedInBy
    ) {
        int quantity = detail.getQuantity() == null
                ? DEFAULT_SINGLE_BOOKING_QUANTITY
                : detail.getQuantity();
        if (quantity <= 0) {
            throw new RuntimeException("Số lượng phòng của booking không hợp lệ");
        }

        List<BookingRoomAccess> existingAccesses =
                bookingRoomAccessRepository.findByBookingid_IdOrderByRoomid_RoomnumberAsc(
                        booking.getId()
                );
        if (!existingAccesses.isEmpty()) {
            throw new RuntimeException("Booking này đã được cấp quyền truy cập phòng");
        }

        List<Room> availableRooms = roomRepository
                .findByRoomtypeid_IdAndStatusOrderByRoomnumberAsc(
                        detail.getRoomtypeid().getId(),
                        AVAILABLE_ROOM_STATUS
                );

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
                            + quantity + " phòng nhưng chỉ có " + selectedRooms.size()
            );
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

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name()) && !"manager".equalsIgnoreCase(staff.getRole().name())) {
            throw new RuntimeException("Bạn không có quyền thực hiện chức năng này");
        }

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt phòng"));

        if (!"Checked-in".equalsIgnoreCase(booking.getStatus()) && !"Checked In".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Đơn đặt phòng chưa được check-in");
        }

        booking.setStatus("Checked-out");
        booking.setUpdatedat(Instant.now());
        bookingRepository.save(booking);

        Bookingdetail detail = bookingdetailRepository.findByBookingid_Id(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đặt phòng"));
        Instant now = Instant.now();
        detail.setActualcheckout(now);
        detail.setCheckedoutat(now);
        detail.setCheckedoutby(staff);
        List<BookingRoomAccess> roomAccesses =
                bookingRoomAccessRepository.findByBookingid_IdOrderByRoomid_RoomnumberAsc(bookingId);
        for (BookingRoomAccess access : roomAccesses) {
            Room room = access.getRoomid();
            room.setStatus(AVAILABLE_ROOM_STATUS);
            room.setUpdatedat(now);
            access.setRoomkeyaccess(null);
            access.setRoomkeyexpiredat(now);
            access.setRoomkeystatus(ROOM_KEY_STATUS_EXPIRED);
            access.setUpdatedat(now);
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
            room.setStatus(AVAILABLE_ROOM_STATUS);
            room.setUpdatedat(now);
            roomRepository.save(room);
        }
        bookingdetailRepository.save(detail);

        return mapToResponse(booking, detail, detail.getRoomtypeid());
    }

    private AiFaceVerificationResponse callFaceVerificationService(
            String registeredEmbedding,
            MultipartFile selfieImage,
            MultipartFile leftImage,
            MultipartFile rightImage,
            MultipartFile upImage,
            MultipartFile downImage
    ) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("registered_embedding", registeredEmbedding)
                .contentType(MediaType.TEXT_PLAIN);

        addFaceImagePart(bodyBuilder, "selfie_image", selfieImage, "face-center.jpg");
        addFaceImagePart(bodyBuilder, "left_image", leftImage, "face-left.jpg");
        addFaceImagePart(bodyBuilder, "right_image", rightImage, "face-right.jpg");
        addFaceImagePart(bodyBuilder, "up_image", upImage, "face-up.jpg");
        addFaceImagePart(bodyBuilder, "down_image", downImage, "face-down.jpg");

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
                    ex
            );
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Không thể kết nối tới dịch vụ xác minh khuôn mặt",
                    ex
            );
        }
    }

    private void addFaceImagePart(
            MultipartBodyBuilder bodyBuilder,
            String partName,
            MultipartFile image,
            String fallbackFilename
    ) {
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

    private void validateFaceCheckInDate(Bookingdetail detail) {
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
            boolean includeRoomPassword
    ) {
        List<BookingRoomAccess> accesses =
                bookingRoomAccessRepository.findByBookingid_IdOrderByRoomid_RoomnumberAsc(
                        booking.getId()
                );

        if (!accesses.isEmpty()) {
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

        if (detail.getRoomid() == null) {
            return List.of();
        }

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

    private boolean isRoomAccessUsable(
            Booking booking,
            BookingRoomAccess access
    ) {
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

        if (!"receptionist".equalsIgnoreCase(staff.getRole().name()) && !"manager".equalsIgnoreCase(staff.getRole().name())) {
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
        return checkInMethod;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingHistoryResponse> filterBookings(BookingFilter criteria) {
        int page = criteria.getPage() != null ? criteria.getPage() : 0;
        int pageSize = criteria.getPageSize() != null ? criteria.getPageSize() : 10;
        String sortBy = criteria.getSortBy() != null ? criteria.getSortBy() : "createdat";
        Sort.Direction direction = criteria.getSortDirection() != null && criteria.getSortDirection().equalsIgnoreCase("DESC") 
            ? Sort.Direction.DESC : Sort.Direction.ASC;
    
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));
    
        Page<Booking> bookings = bookingRepository.findAll(new BookingSpecification(criteria), pageable);
    
        List<BookingHistoryResponse> content = bookings.getContent().stream()
            .map(this::mapToBookingHistoryResponse)
            .collect(Collectors.toList());
    
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
        return BookingHistoryResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingreference())
                .bookingDate(booking.getCreatedat())
                .status(booking.getStatus())
                .checkInMethod(booking.getCheckinmethod())
                .totalAmount(booking.getTotalamount())
                .build();
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
}
