package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaypalCaptureResponse {
    private String paypalOrderId;
    private String transactionId;
    private String status;
    private BigDecimal amount;
    private String currency;
}
