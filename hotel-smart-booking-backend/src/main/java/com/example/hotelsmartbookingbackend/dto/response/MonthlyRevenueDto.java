package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyRevenueDto {
    private String month;       // e.g. "T1", "T2", "T3" or "02/2026"
    private BigDecimal revenue; // Total revenue in VND
    private double percentage;  // Percentage relative to the maximum month's revenue (0-100)
}
