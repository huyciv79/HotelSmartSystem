package com.example.hotelsmartbookingbackend.dto.request;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RoomTypeFilterCriteria {
    private String keyword;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minAdults;
    private Integer minTotalCapacity;
    private String status;
}
