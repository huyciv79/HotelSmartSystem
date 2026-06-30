package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddServiceRequest {
    @NotNull(message = "Vui lòng chọn dịch vụ")
    private Integer serviceId;

    @NotNull(message = "Vui lòng nhập số lượng")
    @Positive(message = "Số lượng phải lớn hơn 0")
    private Integer quantity;

    private String note;
}
