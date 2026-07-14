package com.example.hotelsmartbookingbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiEkycResponse {


    @JsonProperty("id_card_number")
    private String idCardNumber;

    @JsonProperty("full_name")
    private String fullName;

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
