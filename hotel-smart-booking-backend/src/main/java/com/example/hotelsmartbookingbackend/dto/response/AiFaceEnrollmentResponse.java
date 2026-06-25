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

    @JsonProperty("angle_embeddings")
    private List<FaceAngleEmbedding> angleEmbeddings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaceAngleEmbedding {
        private String pose;

        private List<Double> embedding;

        @JsonProperty("yaw_score")
        private Double yawScore;

        @JsonProperty("pitch_score")
        private Double pitchScore;

        @JsonProperty("quality_score")
        private Double qualityScore;

        @JsonProperty("liveness_score")
        private Double livenessScore;

        private Boolean available;

        @JsonProperty("fail_reason")
        private String failReason;
    }
}
