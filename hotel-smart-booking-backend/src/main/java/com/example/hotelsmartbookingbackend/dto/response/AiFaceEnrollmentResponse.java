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

    @JsonProperty("liveness_passed")
    private Boolean livenessPassed;

    @JsonProperty("active_liveness_passed")
    private Boolean activeLivenessPassed;

    private String message;

    @JsonProperty("model_used")
    private String modelUsed;

    @JsonProperty("detector_used")
    private String detectorUsed;

    private List<Double> embedding;
}
