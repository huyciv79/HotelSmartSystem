package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankTransferRequest {
    @NotNull(message = "Booking ID is required")
    private Integer bookingId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String transactionCode;
}
