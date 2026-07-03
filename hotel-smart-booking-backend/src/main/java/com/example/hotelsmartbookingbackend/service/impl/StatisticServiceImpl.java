package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.response.DashboardStatsResponse;
import com.example.hotelsmartbookingbackend.dto.response.MonthlyRevenueDto;
import com.example.hotelsmartbookingbackend.entity.Booking;
import com.example.hotelsmartbookingbackend.repository.BookingRepository;
import com.example.hotelsmartbookingbackend.repository.RoomRepository;
import com.example.hotelsmartbookingbackend.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticServiceImpl implements StatisticService {

    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        // 1. Room Occupancy
        long totalRooms = roomRepository.count();
        long occupiedRooms = roomRepository.countByStatus("Occupied");
        double occupancyRate = totalRooms > 0 ? ((double) occupiedRooms / totalRooms * 100.0) : 0.0;

        // 2. Expected Revenue
        BigDecimal expectedRevenue = bookingRepository.sumExpectedRevenue();
        if (expectedRevenue == null) {
            expectedRevenue = BigDecimal.ZERO;
        }

        // 3. Status counts
        long confirmedCount = bookingRepository.countConfirmedBookings();
        long checkedInCount = bookingRepository.countCheckedInBookings();

        // 4. Monthly Revenue for the last 6 months
        YearMonth currentMonth = YearMonth.now(ZoneId.systemDefault());
        List<YearMonth> last6Months = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            last6Months.add(currentMonth.minusMonths(i));
        }

        // Fetch active bookings since start of the oldest month in the 6-month window
        Instant startDate = last6Months.get(0).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Booking> monthlyBookings = bookingRepository.findActiveBookingsSince(startDate);

        Map<YearMonth, BigDecimal> revenueMap = new HashMap<>();
        for (YearMonth ym : last6Months) {
            revenueMap.put(ym, BigDecimal.ZERO);
        }

        for (Booking b : monthlyBookings) {
            YearMonth ym = YearMonth.from(b.getCreatedat().atZone(ZoneId.systemDefault()));
            if (revenueMap.containsKey(ym)) {
                revenueMap.put(ym, revenueMap.get(ym).add(b.getFinalamount()));
            }
        }

        BigDecimal maxRevenue = BigDecimal.ZERO;
        for (BigDecimal val : revenueMap.values()) {
            if (val.compareTo(maxRevenue) > 0) {
                maxRevenue = val;
            }
        }

        List<MonthlyRevenueDto> monthlyRevenueList = new ArrayList<>();
        for (YearMonth ym : last6Months) {
            BigDecimal rev = revenueMap.get(ym);
            double pct = 0.0;
            if (maxRevenue.compareTo(BigDecimal.ZERO) > 0) {
                pct = rev.multiply(BigDecimal.valueOf(100))
                        .divide(maxRevenue, 2, java.math.RoundingMode.HALF_UP)
                        .doubleValue();
            }
            String label = "T" + ym.getMonthValue();
            monthlyRevenueList.add(new MonthlyRevenueDto(label, rev, pct));
        }

        // 5. Booking Type Percentages
        List<Booking> allActiveBookings = bookingRepository.findAllActiveBookings();
        long totalActive = allActiveBookings.size();

        long onlineCount = 0;
        long groupCount = 0;
        long walkInCount = 0;

        for (Booking b : allActiveBookings) {
            String type = b.getBookingtype();
            if ("Online".equalsIgnoreCase(type)) {
                onlineCount++;
            } else if ("Group".equalsIgnoreCase(type)) {
                groupCount++;
            } else if ("Walk-in".equalsIgnoreCase(type)) {
                walkInCount++;
            }
        }

        Map<String, Double> bookingTypePercentages = new HashMap<>();
        if (totalActive > 0) {
            bookingTypePercentages.put("Online", (double) onlineCount / totalActive * 100.0);
            bookingTypePercentages.put("Group", (double) groupCount / totalActive * 100.0);
            bookingTypePercentages.put("Walk-in", (double) walkInCount / totalActive * 100.0);
        } else {
            bookingTypePercentages.put("Online", 0.0);
            bookingTypePercentages.put("Group", 0.0);
            bookingTypePercentages.put("Walk-in", 0.0);
        }

        // 6. Check-in/out stats & customer stats
        List<Booking> allBookings = bookingRepository.findAll();
        long totalCheckIns = 0;
        long totalCheckOuts = 0;
        Map<Integer, Integer> userBookingCounts = new HashMap<>();

        for (Booking b : allBookings) {
            String status = b.getStatus();
            if (status != null) {
                String statusLower = status.toLowerCase();
                if (statusLower.contains("checked-in") || statusLower.contains("checked in") 
                        || statusLower.equals("staying") || statusLower.contains("checked-out") 
                        || statusLower.contains("checked out") || statusLower.equals("completed")) {
                    totalCheckIns++;
                }
                if (statusLower.contains("checked-out") || statusLower.contains("checked out") 
                        || statusLower.equals("completed")) {
                    totalCheckOuts++;
                }
            }
            if (b.getUserid() != null) {
                Integer uId = b.getUserid().getId();
                userBookingCounts.put(uId, userBookingCounts.getOrDefault(uId, 0) + 1);
            }
        }

        long totalUniqueCustomers = userBookingCounts.size();
        long returningCustomersCount = 0;
        for (int count : userBookingCounts.values()) {
            if (count >= 2) {
                returningCustomersCount++;
            }
        }
        double returningCustomerRate = totalUniqueCustomers > 0 
                ? ((double) returningCustomersCount / totalUniqueCustomers * 100.0) 
                : 0.0;

        return DashboardStatsResponse.builder()
                .totalRooms(totalRooms)
                .occupiedRooms(occupiedRooms)
                .occupancyRate(occupancyRate)
                .expectedRevenue(expectedRevenue)
                .confirmedCount(confirmedCount)
                .checkedInCount(checkedInCount)
                .totalCheckIns(totalCheckIns)
                .totalCheckOuts(totalCheckOuts)
                .totalUniqueCustomers(totalUniqueCustomers)
                .returningCustomersCount(returningCustomersCount)
                .returningCustomerRate(returningCustomerRate)
                .monthlyRevenue(monthlyRevenueList)
                .bookingTypePercentages(bookingTypePercentages)
                .build();
    }
}
