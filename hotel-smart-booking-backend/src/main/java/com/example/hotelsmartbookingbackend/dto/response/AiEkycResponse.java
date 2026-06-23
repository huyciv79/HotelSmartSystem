package com.example.hotelsmartbookingbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO ánh xạ phản hồi OCR CCCD từ Python AI Service.
 * Endpoint: POST http://localhost:8000/api/v1/ai/verify-ekyc
 *
 * <p>Cấu trúc JSON từ AI Service:
 * <pre>
 * {
 *   "id_card_number": "012345678901",
 *   "full_name": "NGUYEN VAN A",
 *   "date_of_birth": "01/01/2000"
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEkycResponse {

    /**
     * Số CCCD/CMND đọc từ vùng YOLO id_number bằng VietOCR.
     * Null nếu OCR không nhận dạng được (ảnh mờ, thiếu sáng, ...).
     */
    @JsonProperty("id_card_number")
    private String idCardNumber;

    /**
     * Họ tên bóc tách tự động từ ảnh mặt trước CCCD.
     */
    @JsonProperty("full_name")
    private String fullName;

    /**
     * Ngày sinh bóc tách tự động từ ảnh mặt trước CCCD (định dạng dd/mm/yyyy).
     */
    @JsonProperty("date_of_birth")
    private String dateOfBirth;

}
