package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
import com.example.hotelsmartbookingbackend.dto.request.CreateGroupBookingRequest;
import com.example.hotelsmartbookingbackend.dto.response.BookingHistoryResponse;
import com.example.hotelsmartbookingbackend.dto.response.BookingResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.Bookingdetail;
import com.example.hotelsmartbookingbackend.entity.Roomtype;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.BookingdetailRepository;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.repository.RoomtypeRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");
    private static final String ACTIVE_STATUS = "Active";
    private static final String AVAILABLE_ROOM_STATUS = "Available";
    private static final String BOOKING_TYPE_ONLINE = "Online";
    private static final String BOOKING_TYPE_GROUP = "Group";
    private static final String CHECK_IN_METHOD_MANUAL = "Manual";
    private static final String BOOKING_STATUS_CONFIRMED = "Confirmed";
    private static final String DETAIL_STATUS_ACTIVE = "Active";
    private static final int DEFAULT_SINGLE_BOOKING_QUANTITY = 1;
    private static final int DEFAULT_SINGLE_BOOKING_ADULTS = 1;
    private static final int DEFAULT_SINGLE_BOOKING_CHILDREN = 0;
    private static final List<String> INVENTORY_HOLDING_BOOKING_STATUSES =
            List.of("Pending", "Confirmed", "Checked In");
    private static final List<String> INVENTORY_HOLDING_DETAIL_STATUSES = List.of("Active");
    private static final DateTimeFormatter BOOKING_REFERENCE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(HOTEL_ZONE);
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final BookingRepository bookingRepository;
    private final BookingdetailRepository bookingdetailRepository;
    private final RoomtypeRepository roomtypeRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String customerEmail) {
        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        return createBookingInternal(
                customerEmail,
                request.getRoomTypeId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                DEFAULT_SINGLE_BOOKING_QUANTITY,
                DEFAULT_SINGLE_BOOKING_ADULTS,
                DEFAULT_SINGLE_BOOKING_CHILDREN,
                request.getSpecialRequests(),
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

        Booking booking = new Booking();
        booking.setUserid(customer);
        booking.setBookingreference(generateBookingReference(now));
        booking.setBookingtype(bookingType);
        booking.setCheckinmethod(CHECK_IN_METHOD_MANUAL);
        booking.setTotalamount(totalAmount);
        booking.setPaidamount(BigDecimal.ZERO);
        booking.setDepositamount(BigDecimal.ZERO);
        booking.setDiscountamount(BigDecimal.ZERO);
        booking.setTaxamount(BigDecimal.ZERO);
        booking.setServicechargeamount(BigDecimal.ZERO);
        booking.setFinalamount(totalAmount);
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
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingDetail(Integer bookingId, String customerEmail) {
        Bookingdetail detail = bookingdetailRepository.findBookingDetail(bookingId, customerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng hoặc bạn không có quyền xem đặt phòng này"));

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

        if (request.getNumberOfAdults() == null || request.getNumberOfAdults() <= 0) {
            throw new RuntimeException("Số người lớn phải lớn hơn 0");
        }

        if (request.getNumberOfChildren() == null || request.getNumberOfChildren() < 0) {
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

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingreference())
                .bookingType(booking.getBookingtype())
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
                .status(booking.getStatus())
                .specialRequests(booking.getSpecialrequests())
                .createdAt(booking.getCreatedat())
                .build();
    }

    private BookingHistoryResponse mapToHistoryResponse(Bookingdetail detail) {
        Booking booking = detail.getBookingid();
        Roomtype roomtype = detail.getRoomtypeid();
        LocalDate checkInDate = toLocalDate(detail.getExpectedcheckin());
        LocalDate checkOutDate = toLocalDate(detail.getExpectedcheckout());

        return BookingHistoryResponse.builder()
                .bookingId(booking.getId())
                .bookingNumber(booking.getBookingreference())
                .bookingDate(booking.getCreatedat())
                .roomTypeId(roomtype.getId())
                .roomType(roomtype.getName())
                .quantity(detail.getQuantity())
                .numberOfAdults(detail.getNumberofadults())
                .numberOfChildren(detail.getNumberofchildren())
                .checkInDate(checkInDate)
                .checkOutDate(checkOutDate)
                .nights(ChronoUnit.DAYS.between(checkInDate, checkOutDate))
                .totalAmount(booking.getTotalamount())
                .status(booking.getStatus())
                .build();
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

}
