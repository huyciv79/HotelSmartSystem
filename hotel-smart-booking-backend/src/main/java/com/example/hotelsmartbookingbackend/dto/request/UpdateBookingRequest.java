package com.example.hotelsmartbookingbackend.dto.request;

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
    private String specialRequests;
    
    private BigDecimal discountAmount;
    


    private String checkInDate;

    private String checkOutDate;

    private Integer roomTypeId;

    private Integer quantity;

    private Integer numberOfAdults;

    private Integer numberOfChildren;
}
