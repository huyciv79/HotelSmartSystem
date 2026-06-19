package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotNull(message = "Booking ID is required")
    private Integer bookingId;

    private BigDecimal amount; // Optional, defaults to remaining unpaid balance

    private String paymentOption; // "DEPOSIT" or "FULL"

    public CreateOrderRequest(Integer bookingId, BigDecimal amount) {
        this.bookingId = bookingId;
        this.amount = amount;
        this.paymentOption = "FULL";
    }
}
