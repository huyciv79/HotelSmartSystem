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
public class AiFaceFrameValidationResponse {

    private Boolean passed;
    private String step;
    private String code;
    private String reason;

    @JsonProperty("yaw_score")
    private Double yawScore;

    @JsonProperty("pitch_score")
    private Double pitchScore;

    @JsonProperty("reference_yaw")
    private Double referenceYaw;

    @JsonProperty("reference_pitch")
    private Double referencePitch;

    @JsonProperty("liveness_score")
    private Double livenessScore;

    @JsonProperty("face_distance")
    private Double faceDistance;

    @JsonProperty("face_threshold")
    private Double faceThreshold;
}
