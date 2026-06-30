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
    private String roomnumber;
    private Integer floornumber;
    private String status;
    private Integer roomtypeid;
    private String roomtypename;
    private String adminpasscode;
    private Instant createdAt;
    private Instant updatedAt;
}

