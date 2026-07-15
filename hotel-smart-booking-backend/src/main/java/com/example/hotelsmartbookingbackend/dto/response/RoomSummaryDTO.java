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
public class RoomSummaryDTO {
    private Integer id;
    private String roomNumber;
    private Integer floorNumber;
    private String status;
    private Integer roomTypeId;
    private String roomTypeName;
    private String adminPasscode;
    private Instant createdAt;
    private Instant updatedAt;
}

