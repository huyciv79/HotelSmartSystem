package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomTypeDetailDTO {
    private Integer id;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer adultCapacity;
    private Integer childCapacity;
    private Integer totalCapacity;
    private BigDecimal area;
    private String bedType;
    private String amenities;
    private String status;
    private List<RoomTypeImageDTO> images;
    private Instant createdAt;
    private Instant updatedAt;
}
