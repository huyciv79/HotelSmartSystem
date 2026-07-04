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
public class NotificationResponse {
    private Integer id;
    private String title;
    private String message;
    private String type;
    private Integer referenceid;
    private Boolean isread;
    private Instant readat;
    private Instant sentat;
}
