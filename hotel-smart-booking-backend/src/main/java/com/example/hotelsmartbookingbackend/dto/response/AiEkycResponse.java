package com.example.hotelsmartbookingbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO ánh xạ phản hồi JSON từ Python AI Service (phiên bản tự động hóa).
 * Endpoint: POST http://localhost:8000/api/v1/ai/verify-ekyc
 *
 * <p>Cấu trúc JSON từ AI Service:
 * <pre>
 * {
 *   "id_card_number": "012345678901",  // null nếu OCR không đọc được
 *   "embedding": [0.12, -0.43, ...]    // 512 phần tử float
 * }
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEkycResponse {

    /**
     * Số CCCD/CMND bóc tách tự động từ ảnh mặt trước CCCD bằng EasyOCR.
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

    /**
     * Mảng vector embedding khuôn mặt 512 chiều từ ảnh selfie (DeepFace.represent).
     */
    @JsonProperty("embedding")
    private List<Double> embedding;
}
