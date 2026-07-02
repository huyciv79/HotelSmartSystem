package com.example.hotelsmartbookingbackend.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.hotelsmartbookingbackend.dto.response.Notification;

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
    private Notification notification;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public static WebSocketNotificationDTO ofNew(Notification notification) {
        return WebSocketNotificationDTO.builder()
                .type("notification")
                .action("new")
                .notification(notification)
                .timestamp(Instant.now())
                .build();
    }

    public static WebSocketNotificationDTO ofRead(Notification notification) {
        return WebSocketNotificationDTO.builder()
                .type("notification")
                .action("read")
                .notification(notification)
                .timestamp(Instant.now())
                .build();
    }

    public static WebSocketNotificationDTO ofDelete(Integer notificationId) {
        return WebSocketNotificationDTO.builder()
                .type("notification")
                .action("delete")
                .notification(Notification.builder().id(notificationId).build())
                .timestamp(Instant.now())
                .build();
    }
}


