package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerEarlyCheckOutRequest {
    @NotNull(message = "Mã đặt phòng không được để trống")
    private Integer bookingId;

    @NotNull(message = "Ngày trả phòng mới không được để trống")
    private String newCheckOutDate;

    private String description;
}
