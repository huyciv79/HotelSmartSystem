package com.example.hotelsmartbookingbackend.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.hotelsmartbookingbackend.dto.response.NotificationResponse;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketNotificationDTO {

    @JsonProperty("type")
    private String type;

    @JsonProperty("action")
    private String action;

    @JsonProperty("notification")
    private NotificationResponse notification;

    @JsonProperty("timestamp")
    private Instant timestamp;


}
