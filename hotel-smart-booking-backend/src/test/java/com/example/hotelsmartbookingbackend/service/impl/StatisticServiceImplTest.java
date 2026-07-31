package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.response.DashboardStatsResponse;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.BookingStatus;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticServiceImplTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private StatisticServiceImpl statisticService;

    private User user1;
    private User user2;
    private Booking booking1;
    private Booking booking2;
    private Booking booking3;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(101);
        user1.setEmail("user1@example.com");

        user2 = new User();
        user2.setId(102);
        user2.setEmail("user2@example.com");

        Instant now = Instant.now();

        booking1 = new Booking();
        booking1.setId(1);
        booking1.setFinalAmount(new BigDecimal("1000000.00"));
        booking1.setStatus(BookingStatus.COMPLETED);
        booking1.setBookingType("Online");
        booking1.setUser(user1);
        booking1.setCreatedAt(now);

        booking2 = new Booking();
        booking2.setId(2);
        booking2.setFinalAmount(new BigDecimal("1500000.00"));
        booking2.setStatus(BookingStatus.CHECKED_IN);
        booking2.setBookingType("Group");
        booking2.setUser(user1);
        booking2.setCreatedAt(now);

        booking3 = new Booking();
        booking3.setId(3);
        booking3.setFinalAmount(new BigDecimal("800000.00"));
        booking3.setStatus(BookingStatus.CONFIRMED);
        booking3.setBookingType("Walk-in");
        booking3.setUser(user2);
        booking3.setCreatedAt(now);
    }

    // ==========================================
    // 1. getDashboardStats Test Cases (UTCID01 - UTCID11)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful getDashboardStats calculating all normal metrics")
    void should_getDashboardStatsSuccessfully_when_allMetricsAreNormal() {
        when(roomRepository.count()).thenReturn(10L);
        when(roomRepository.countByStatus("Occupied")).thenReturn(4L);
        when(bookingRepository.sumExpectedRevenue()).thenReturn(new BigDecimal("3300000.00"));
        when(bookingRepository.countConfirmedBookings()).thenReturn(1L);
        when(bookingRepository.countCheckedInBookings()).thenReturn(1L);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of(booking1, booking2, booking3));
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of(booking1, booking2, booking3));
        when(bookingRepository.findAll()).thenReturn(List.of(booking1, booking2, booking3));

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(10L, stats.getTotalRooms());
        assertEquals(4L, stats.getOccupiedRooms());
        assertEquals(40.0, stats.getOccupancyRate(), 0.01);
        assertEquals(new BigDecimal("3300000.00"), stats.getExpectedRevenue());
        assertEquals(1L, stats.getConfirmedCount());
        assertEquals(1L, stats.getCheckedInCount());
        assertEquals(2L, stats.getTotalCheckIns());
        assertEquals(1L, stats.getTotalCheckOuts());
        assertEquals(2L, stats.getTotalUniqueCustomers());
        assertEquals(1L, stats.getReturningCustomersCount());
        assertEquals(50.0, stats.getReturningCustomerRate(), 0.01);
        assertEquals(6, stats.getMonthlyRevenue().size());
        assertEquals(3, stats.getBookingTypePercentages().size());
    }

    @Test
    @DisplayName("UTCID02 - Calculate 0.0 occupancy rate when totalRooms is 0")
    void should_calculateZeroOccupancyRate_when_totalRoomsIsZero() {
        when(roomRepository.count()).thenReturn(0L);
        when(roomRepository.countByStatus("Occupied")).thenReturn(0L);
        when(bookingRepository.sumExpectedRevenue()).thenReturn(BigDecimal.ZERO);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of());

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalRooms());
        assertEquals(0.0, stats.getOccupancyRate());
    }

    @Test
    @DisplayName("UTCID03 - Default expectedRevenue to ZERO when sumExpectedRevenue returns null")
    void should_defaultExpectedRevenueToZero_when_sumExpectedRevenueReturnsNull() {
        when(roomRepository.count()).thenReturn(5L);
        when(roomRepository.countByStatus("Occupied")).thenReturn(1L);
        when(bookingRepository.sumExpectedRevenue()).thenReturn(null);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of());

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(BigDecimal.ZERO, stats.getExpectedRevenue());
    }

    @Test
    @DisplayName("UTCID04 - Calculate 0.0 monthly revenue percentages when maxRevenue across 6 months is zero")
    void should_calculateZeroMonthlyPercentages_when_maxRevenueIsZero() {
        when(roomRepository.count()).thenReturn(5L);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of());

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(6, stats.getMonthlyRevenue().size());
        stats.getMonthlyRevenue().forEach(dto -> {
            assertEquals(BigDecimal.ZERO, dto.getRevenue());
            assertEquals(0.0, dto.getPercentage());
        });
    }

    @Test
    @DisplayName("UTCID05 - Calculate 0.0 booking type percentages when total active bookings is 0")
    void should_calculateZeroBookingTypePercentages_when_totalActiveBookingsIsZero() {
        when(roomRepository.count()).thenReturn(5L);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of());

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(0.0, stats.getBookingTypePercentages().get("Online"));
        assertEquals(0.0, stats.getBookingTypePercentages().get("Group"));
        assertEquals(0.0, stats.getBookingTypePercentages().get("Walk-in"));
    }

    @Test
    @DisplayName("UTCID06 - Handle null status and null user safely without NullPointerException")
    void should_handleNullStatusAndNullUserSafely_when_calculatingCheckInAndCustomerStats() {
        Booking nullStatusBooking = new Booking();
        nullStatusBooking.setId(10);
        nullStatusBooking.setStatus(null);
        nullStatusBooking.setUser(null);

        when(roomRepository.count()).thenReturn(5L);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of(nullStatusBooking));

        assertDoesNotThrow(() -> {
            DashboardStatsResponse stats = statisticService.getDashboardStats();
            assertEquals(0L, stats.getTotalCheckIns());
            assertEquals(0L, stats.getTotalCheckOuts());
            assertEquals(0L, stats.getTotalUniqueCustomers());
        });
    }

    @Test
    @DisplayName("UTCID07 - Calculate 0.0 returningCustomerRate when totalUniqueCustomers is 0")
    void should_calculateZeroReturningCustomerRate_when_totalUniqueCustomersIsZero() {
        when(roomRepository.count()).thenReturn(5L);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of());

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalUniqueCustomers());
        assertEquals(0.0, stats.getReturningCustomerRate());
    }

    @Test
    @DisplayName("UTCID08 - Classify booking types case-insensitively when mixed case booking types provided")
    void should_classifyBookingTypesCaseInsensitively_when_mixedCaseBookingTypesProvided() {
        Booking b1 = new Booking();
        b1.setBookingType("online");

        Booking b2 = new Booking();
        b2.setBookingType("GROUP");

        Booking b3 = new Booking();
        b3.setBookingType("Walk-In");

        when(roomRepository.count()).thenReturn(5L);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of(b1, b2, b3));
        when(bookingRepository.findAll()).thenReturn(List.of());

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(33.33, stats.getBookingTypePercentages().get("Online"), 0.01);
        assertEquals(33.33, stats.getBookingTypePercentages().get("Group"), 0.01);
        assertEquals(33.33, stats.getBookingTypePercentages().get("Walk-in"), 0.01);
    }

    @Test
    @DisplayName("UTCID09 - Increment totalCheckIns for STAYING status without incrementing totalCheckOuts")
    void should_incrementCheckInsOnly_when_bookingStatusIsStaying() {
        Booking stayingBooking = new Booking();
        stayingBooking.setId(20);
        stayingBooking.setStatus(BookingStatus.STAYING);
        stayingBooking.setUser(user1);

        when(roomRepository.count()).thenReturn(5L);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of(stayingBooking));

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(1L, stats.getTotalCheckIns());
        assertEquals(0L, stats.getTotalCheckOuts());
    }

    @Test
    @DisplayName("UTCID10 - Do not count user as returning customer when booking count is 1")
    void should_notCountAsReturningCustomer_when_userHasSingleBooking() {
        Booking singleBooking = new Booking();
        singleBooking.setId(30);
        singleBooking.setStatus(BookingStatus.CONFIRMED);
        singleBooking.setUser(user1);

        when(roomRepository.count()).thenReturn(5L);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of());
        when(bookingRepository.findAll()).thenReturn(List.of(singleBooking));

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(1L, stats.getTotalUniqueCustomers());
        assertEquals(0L, stats.getReturningCustomersCount());
        assertEquals(0.0, stats.getReturningCustomerRate());
    }

    @Test
    @DisplayName("UTCID11 - Safely ignore unknown or null booking types during percentage breakdown")
    void should_ignoreUnknownOrNullBookingTypesSafely_when_calculatingPercentages() {
        Booking unknownTypeBooking = new Booking();
        unknownTypeBooking.setBookingType("VIP_CUSTOM");

        Booking nullTypeBooking = new Booking();
        nullTypeBooking.setBookingType(null);

        when(roomRepository.count()).thenReturn(5L);
        when(bookingRepository.findActiveBookingsSince(any())).thenReturn(List.of());
        when(bookingRepository.findAllActiveBookings()).thenReturn(List.of(unknownTypeBooking, nullTypeBooking));
        when(bookingRepository.findAll()).thenReturn(List.of());

        DashboardStatsResponse stats = statisticService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(0.0, stats.getBookingTypePercentages().get("Online"));
        assertEquals(0.0, stats.getBookingTypePercentages().get("Group"));
        assertEquals(0.0, stats.getBookingTypePercentages().get("Walk-in"));
    }
}
