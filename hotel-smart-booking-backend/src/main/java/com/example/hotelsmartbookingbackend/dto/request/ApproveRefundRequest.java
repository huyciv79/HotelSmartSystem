package com.example.hotelsmartbookingbackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApproveRefundRequest {
    private BigDecimal refundAmount;
}
