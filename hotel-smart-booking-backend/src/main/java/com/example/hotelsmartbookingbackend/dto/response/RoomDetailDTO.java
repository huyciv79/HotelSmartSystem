package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomDetailDTO {
    private Integer id;
    private String roomNumber;
    private Integer floorNumber;
    private String status;
    private String note;
    private String adminPasscode;
    private Integer roomTypeId;
    private String roomTypeName;
    private Integer adultCapacity;
    private Integer childCapacity;
    private Integer totalCapacity;
    private java.math.BigDecimal area;
    private String bedType;
    private Integer bedCount;
    private Instant createdAt;
    private Instant updatedAt;
}

