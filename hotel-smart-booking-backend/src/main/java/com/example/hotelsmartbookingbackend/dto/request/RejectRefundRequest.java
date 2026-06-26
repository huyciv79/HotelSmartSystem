package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RejectRefundRequest {
    @NotBlank(message = "Vui lòng nhập lý do từ chối")
    private String rejectionReason;
}
