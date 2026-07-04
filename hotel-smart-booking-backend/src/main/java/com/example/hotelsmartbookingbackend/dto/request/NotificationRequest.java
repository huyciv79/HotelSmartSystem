package com.example.hotelsmartbookingbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    @NotBlank(message = "Title cannot be blank")
    @JsonProperty("title")
    private String title;

    @NotBlank(message = "Message cannot be blank")
    @JsonProperty("message")
    private String message;

    @JsonProperty("type")
    @Builder.Default
    private String type = "System";

    @JsonProperty("referenceId")
    private Integer referenceId;

    @JsonProperty("userId")
    private Integer userId;

    @JsonProperty("broadcastToAll")
    @Builder.Default
    private Boolean broadcastToAll = false;
}
