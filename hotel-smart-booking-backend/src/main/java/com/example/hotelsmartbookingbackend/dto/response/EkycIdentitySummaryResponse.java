package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EkycIdentitySummaryResponse {
    private String status;
    private Integer userId;
    private String fullName;
    private String idNumber;
    private String dateOfBirth;
    private String gender;
    private String hometown;
    private String provinceCode;
    private String provinceName;
    private Instant verifiedAt;
    @JsonIgnore
    private String frontImage;
    @JsonIgnore
    private String backImage;
    @JsonIgnore
    private String faceImage;
}
