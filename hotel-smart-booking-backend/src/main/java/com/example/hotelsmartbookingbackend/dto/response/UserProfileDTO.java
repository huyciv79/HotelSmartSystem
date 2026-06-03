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
public class UserProfileDTO {
    private Integer id;
    private String email;
    private String role;
    private String phoneNumber;
    private String fullName;
    private String avatar;
    private String address;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
