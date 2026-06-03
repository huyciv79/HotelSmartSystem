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
public class RoomTypeSummaryDTO {
    private Integer id;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer adultCapacity;
    private Integer childCapacity;
    private Integer totalCapacity;
    private BigDecimal area;
    private String bedType;
    private String primaryImageUrl;
    private String status;
}
