package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateServiceRequest {

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 150)
    private String name;

    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal price;

    @NotBlank(message = "Đơn vị không được để trống")
    @Size(max = 50)
    private String unit;

    @NotNull(message = "Trạng thái hoạt động không được để trống")
    private Boolean isactive;
}