package com.example.hotelsmartbookingbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO trả về cho client sau khi xử lý eKYC.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EkycResponse {

    /** Kết quả tổng thể: "Verified" hoặc "Rejected" */
    private String status;

    /** Thông điệp mô tả kết quả */
    private String message;

    /** Số CCCD được OCR tự động nhận dạng từ ảnh mặt trước */
    private String recognizedIdNumber;
}
