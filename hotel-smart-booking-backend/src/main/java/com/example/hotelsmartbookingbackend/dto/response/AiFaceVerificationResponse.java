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

    @JsonProperty("liveness_passed")
    private Boolean livenessPassed;

    @JsonProperty("active_liveness_passed")
    private Boolean activeLivenessPassed;

    @JsonProperty("is_real")
    private Boolean real;

    @JsonProperty("liveness_score")
    private Double livenessScore;

    @JsonProperty("liveness_threshold")
    private Double livenessThreshold;

    @JsonProperty("anti_spoofing_model")
    private String antiSpoofingModel;

    @JsonProperty("center_yaw")
    private Double centerYaw;

    @JsonProperty("first_turn_yaw")
    private Double firstTurnYaw;

    @JsonProperty("second_turn_yaw")
    private Double secondTurnYaw;

    @JsonProperty("center_pitch")
    private Double centerPitch;

    @JsonProperty("up_pitch")
    private Double upPitch;

    @JsonProperty("down_pitch")
    private Double downPitch;

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
