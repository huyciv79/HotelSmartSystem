package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomAvailabilityResponse {
    private Integer roomTypeId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer totalRooms;
    private Integer availableRooms;
    private LocalDate lowestAvailabilityDate;
}
