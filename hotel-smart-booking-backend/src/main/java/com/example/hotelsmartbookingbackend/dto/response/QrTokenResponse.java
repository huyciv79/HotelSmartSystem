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
public class QrTokenResponse {
    private Integer bookingId;
    private String bookingReference;
    private String token;
    private String qrPayload;
    private Instant generatedAt;
    private Instant expiresAt;
}
