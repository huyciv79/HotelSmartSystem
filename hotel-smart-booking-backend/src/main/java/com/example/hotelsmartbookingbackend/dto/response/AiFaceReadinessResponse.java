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
public class AiFaceReadinessResponse {

    private Boolean ready;
    private String reason;
    private String code;

    @JsonProperty("face_count")
    private Integer faceCount;

    @JsonProperty("face_width")
    private Integer faceWidth;

    @JsonProperty("face_height")
    private Integer faceHeight;

    private Double yaw;
}
