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
public class CustomerRequestResponse {
    private Integer requestId;
    private Integer bookingId;
    private String bookingReference;
    private String requestType;
    private String description;
    private String oldValue;
    private String newValue;
    private String status;
    private Instant resolvedAt;
    private String rejectionReason;
    private Instant createdAt;
}
