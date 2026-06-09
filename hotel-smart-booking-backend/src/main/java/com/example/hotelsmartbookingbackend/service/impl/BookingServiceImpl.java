package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.CreateBookingRequest;
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
    private static final String CHECK_IN_METHOD_MANUAL = "Manual";
    private static final String BOOKING_STATUS_CONFIRMED = "Confirmed";
    private static final String DETAIL_STATUS_ACTIVE = "Active";
    private static final int DEFAULT_BOOKING_QUANTITY = 1;
    private static final List<String> INVENTORY_HOLDING_BOOKING_STATUSES =
            List.of("Pending", "Confirmed", "Checked In");
    private static final List<String> INVENTORY_HOLDING_DETAIL_STATUSES = List.of("Active");
    private static final DateTimeFormatter BOOKING_REFERENCE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(HOTEL_ZONE);

    private final BookingRepository bookingRepository;
    private final BookingdetailRepository bookingdetailRepository;
    private final RoomtypeRepository roomtypeRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request, String customerEmail) {
        validateRequest(request);

        User customer = userRepository.findByEmail(customerEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Roomtype roomtype = roomtypeRepository.findByIdForUpdate(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Room type not found"));

        if (!ACTIVE_STATUS.equalsIgnoreCase(roomtype.getStatus())) {
            throw new RuntimeException("Room type is not active");
        }

        assertAvailability(roomtype, request);

        Instant now = Instant.now();
        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = roomtype.getBaseprice()
                .multiply(BigDecimal.valueOf(DEFAULT_BOOKING_QUANTITY))
                .multiply(BigDecimal.valueOf(nights));

        Booking booking = new Booking();
        booking.setUserid(customer);
        booking.setBookingreference(generateBookingReference(now));
        booking.setBookingtype(BOOKING_TYPE_ONLINE);
        booking.setCheckinmethod(CHECK_IN_METHOD_MANUAL);
        booking.setTotalamount(totalAmount);
        booking.setPaidamount(BigDecimal.ZERO);
        booking.setDepositamount(BigDecimal.ZERO);
        booking.setDiscountamount(BigDecimal.ZERO);
        booking.setTaxamount(BigDecimal.ZERO);
        booking.setServicechargeamount(BigDecimal.ZERO);
        booking.setFinalamount(totalAmount);
        booking.setStatus(BOOKING_STATUS_CONFIRMED);
        booking.setSpecialrequests(request.getSpecialRequests());
        booking.setCreatedat(now);
        booking.setUpdatedat(now);

        Booking savedBooking = bookingRepository.save(booking);

        Bookingdetail detail = new Bookingdetail();
        detail.setBookingid(savedBooking);
        detail.setRoomtypeid(roomtype);
        detail.setQuantity(DEFAULT_BOOKING_QUANTITY);
        detail.setExpectedcheckin(toInstant(request.getCheckInDate()));
        detail.setExpectedcheckout(toInstant(request.getCheckOutDate()));
        detail.setPriceatbooking(roomtype.getBaseprice());
        detail.setNumberofadults(DEFAULT_BOOKING_QUANTITY);
        detail.setNumberofchildren(0);
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
                .orElseThrow(() -> new RuntimeException("User not found"));

        return bookingdetailRepository.findBookingHistory(customerEmail).stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    private void validateRequest(CreateBookingRequest request) {
        if (request.getCheckInDate() == null || request.getCheckOutDate() == null) {
            throw new RuntimeException("Check-in and check-out dates are required");
        }

        LocalDate today = LocalDate.now(HOTEL_ZONE);
        if (request.getCheckInDate().isBefore(today)) {
            throw new RuntimeException("Check-in date cannot be in the past");
        }

        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new RuntimeException("Check-out date must be after check-in date");
        }

    }

    private void assertAvailability(Roomtype roomtype, CreateBookingRequest request) {
        int totalRooms = Math.toIntExact(roomRepository.countByRoomtypeid_IdAndStatus(
                roomtype.getId(), AVAILABLE_ROOM_STATUS));

        if (totalRooms <= 0) {
            throw new RuntimeException("No active rooms found for this room type");
        }

        if (DEFAULT_BOOKING_QUANTITY > totalRooms) {
            throw new RuntimeException("No rooms available for this room type");
        }

        LocalDate stayDate = request.getCheckInDate();
        while (stayDate.isBefore(request.getCheckOutDate())) {
            Instant periodStart = toInstant(stayDate);
            Instant periodEnd = toInstant(stayDate.plusDays(1));

            long bookedRooms = bookingdetailRepository.sumBookedQuantity(
                    roomtype.getId(),
                    periodStart,
                    periodEnd,
                    INVENTORY_HOLDING_BOOKING_STATUSES,
                    INVENTORY_HOLDING_DETAIL_STATUSES);

            int availableRooms = totalRooms - Math.toIntExact(bookedRooms);
            if (availableRooms < DEFAULT_BOOKING_QUANTITY) {
                throw new RuntimeException("Not enough rooms available on " + stayDate
                        + ". Available: " + availableRooms);
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
        throw new RuntimeException("Cannot generate booking reference");
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

}
