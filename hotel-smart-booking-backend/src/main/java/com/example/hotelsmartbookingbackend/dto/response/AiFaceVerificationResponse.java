package com.example.hotelsmartbookingbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFaceVerificationResponse {

    private Boolean verified;
    private Boolean matched;

    private String message;
    private Double distance;
    private Double threshold;

    @JsonProperty("similarity_percentage")
    private Double similarityPercentage;

    private String metric;

    @JsonProperty("model_used")
    private String modelUsed;

    @JsonProperty("detector_used")
    private String detectorUsed;
}
