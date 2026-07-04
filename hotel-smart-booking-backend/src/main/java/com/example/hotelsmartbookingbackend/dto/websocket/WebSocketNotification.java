package com.example.hotelsmartbookingbackend.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.hotelsmartbookingbackend.dto.response.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketNotification {

    @JsonProperty("type")
    private String type;

    @JsonProperty("action")
    private String action;

    @JsonProperty("notification")
    private Notification notification;

    @JsonProperty("timestamp")
    private Instant timestamp;

    public static WebSocketNotification ofNew(Notification notification) {
        return WebSocketNotification.builder()
                .type("notification")
                .action("new")
                .notification(notification)
                .timestamp(Instant.now())
                .build();
    }

    public static WebSocketNotification ofRead(Notification notification) {
        return WebSocketNotification.builder()
                .type("notification")
                .action("read")
                .notification(notification)
                .timestamp(Instant.now())
                .build();
    }

    public static WebSocketNotification ofDelete(Integer notificationId) {
        return WebSocketNotification.builder()
                .type("notification")
                .action("delete")
                .notification(Notification.builder().id(notificationId).build())
                .timestamp(Instant.now())
                .build();
    }
}

