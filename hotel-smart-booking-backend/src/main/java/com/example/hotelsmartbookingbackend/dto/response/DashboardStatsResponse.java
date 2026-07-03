package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {
    private long totalRooms;
    private long occupiedRooms;
    private double occupancyRate;
    private BigDecimal expectedRevenue;
    private long confirmedCount;
    private long checkedInCount;
    private long totalCheckIns;
    private long totalCheckOuts;
    private long totalUniqueCustomers;
    private long returningCustomersCount;
    private double returningCustomerRate;
    private List<MonthlyRevenueDto> monthlyRevenue;
    private Map<String, Double> bookingTypePercentages;
}
