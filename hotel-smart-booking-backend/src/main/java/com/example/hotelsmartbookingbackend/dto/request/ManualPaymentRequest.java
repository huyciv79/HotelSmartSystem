package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualPaymentRequest {
    @NotNull(message = "Vui lòng chọn đơn đặt phòng")
    private Integer bookingId;

    @NotNull(message = "Vui lòng nhập số tiền thanh toán")
    @Positive(message = "Số tiền phải lớn hơn 0")
    private BigDecimal amount;

    @NotBlank(message = "Vui lòng nhập phương thức thanh toán")
    private String paymentMethod;

    private String paymentType;

    private String notes;
}
