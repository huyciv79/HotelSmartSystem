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
public class EkycStatusResponse {
    private String status; // "SUBMITTED", "AI_CHECKING", "VERIFIED", "NOT_FOUND"
    private String message;
    private String fullName;
    private String idNumber;
    private String dateOfBirth;
    private String address;
    private String gender;
    private String hometown;
    private String provinceCode;
    private String provinceName;
    private Instant verifiedAt;
    private String rejectionReason;
    private String requestId;
    private String frontImage;
    private String backImage;
    private String faceImage;
}
