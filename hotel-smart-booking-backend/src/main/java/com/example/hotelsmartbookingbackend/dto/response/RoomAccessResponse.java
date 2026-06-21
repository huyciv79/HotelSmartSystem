package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAccessResponse {
    private Integer roomId;
    private String roomNumber;
    private Integer floorNumber;
    private String roomPassword;
    private String roomKeyStatus;
    private Instant roomKeyExpiresAt;
}
