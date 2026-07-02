package com.example.hotelsmartbookingbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("message")
    private String message;

    @JsonProperty("type")
    private String type;

    @JsonProperty("referenceId")
    private Integer referenceId;

    @JsonProperty("isRead")
    private Boolean isRead;

    @JsonProperty("readAt")
    private Instant readAt;

    @JsonProperty("sentAt")
    private Instant sentAt;

    public static Notification fromEntity(com.example.hotelsmartbookingbackend.entity.Notification notification) {
        return Notification.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .referenceId(notification.getReferenceid())
                .isRead(notification.getIsread())
                .readAt(notification.getReadat())
                .sentAt(notification.getSentat())
                .build();
    }
}

