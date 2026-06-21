package com.example.hotelsmartbookingbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response returned by POST /api/v1/face/enroll.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFaceEnrollmentResponse {

    private Boolean enrolled;
    private Boolean matched;
    private String message;
    private Double distance;
    private Double threshold;

    @JsonProperty("similarity_percentage")
    private Double similarityPercentage;

    @JsonProperty("model_used")
    private String modelUsed;

    @JsonProperty("detector_used")
    private String detectorUsed;

    private List<Double> embedding;
}
