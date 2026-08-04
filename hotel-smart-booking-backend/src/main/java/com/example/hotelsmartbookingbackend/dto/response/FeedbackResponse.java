package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Integer feedbackId;
    private Integer bookingId;
    private String bookingReference;
    private Integer rating;
    private String comment;
    private String pros;
    private String cons;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private String customerName;
    private String customerEmail;
    private String customerAvatar;
    private List<String> images;
}
