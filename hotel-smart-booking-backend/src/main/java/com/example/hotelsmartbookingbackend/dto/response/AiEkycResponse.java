package com.example.hotelsmartbookingbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

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

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("hometown")
    private String hometown;

    @JsonProperty("validation_passed")
    private Boolean validationPassed;

    @JsonProperty("validation_errors")
    private List<String> validationErrors;

    @JsonProperty("validation_warnings")
    private List<String> validationWarnings;

    @JsonProperty("logic_gender")
    private String logicGender;

    @JsonProperty("logic_birth_year")
    private Integer logicBirthYear;

    @JsonProperty("province_code")
    private String provinceCode;

    @JsonProperty("province_name")
    private String provinceName;

    @JsonProperty("corrected_fields")
    private Map<String, String> correctedFields;

}
