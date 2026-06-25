package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookingRequest {
    @NotNull(message = "Special requests cannot be null")
    private String specialRequests;
    
    private BigDecimal discountAmount;
    
    private String notes;
}
