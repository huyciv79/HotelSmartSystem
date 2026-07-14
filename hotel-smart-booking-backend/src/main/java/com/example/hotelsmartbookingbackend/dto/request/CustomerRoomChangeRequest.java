package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class CustomerRoomChangeRequest {
    @NotNull(message = "Booking ID không được để trống")
    private Integer bookingId;

    @NotNull(message = "ID phòng mới không được để trống")
    private Integer newRoomId;

    private String reason;
}
