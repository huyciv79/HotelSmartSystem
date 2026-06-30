package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CustomerStayExtensionRequest {
    @NotNull(message = "ID đặt phòng không được để trống")
    private Integer bookingId;

    @NotNull(message = "Ngày trả phòng mới không được để trống")
    private String newCheckOutDate;

    @NotNull(message = "Lý do gia hạn không được để trống")
    private String description;
}
