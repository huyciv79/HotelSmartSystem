package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.response.NotificationResponse;
import com.example.hotelsmartbookingbackend.dto.response.RoomStatusUpdateMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceImplTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketServiceImpl webSocketService;

    private NotificationResponse sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = NotificationResponse.builder()
                .id(1)
                .title("Booking Confirmed")
                .message("Your booking #123 has been confirmed.")
                .type("BOOKING")
                .isread(false)
                .sentat(Instant.now())
                .build();
    }

    // ==========================================
    // 1. broadcastRoomStatus Test Cases (UTCID01 - UTCID02)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful broadcastRoomStatus publishing message to /topic/room-status")
    void should_broadcastRoomStatusSuccessfully_when_messagingTemplateSucceeds() {
        assertDoesNotThrow(() -> webSocketService.broadcastRoomStatus(10, "101", "Occupied"));

        verify(messagingTemplate).convertAndSend(eq("/topic/room-status"), any(RoomStatusUpdateMessage.class));
    }

    @Test
    @DisplayName("UTCID02 - Catch exception silently when broadcastRoomStatus encounters MessagingException")
    void should_catchExceptionSilently_when_broadcastRoomStatusEncounterMessagingException() {
        doThrow(new MessagingException("Broker unavailable"))
                .when(messagingTemplate).convertAndSend(eq("/topic/room-status"), any(RoomStatusUpdateMessage.class));

        assertDoesNotThrow(() -> webSocketService.broadcastRoomStatus(10, "101", "Occupied"));

        verify(messagingTemplate).convertAndSend(eq("/topic/room-status"), any(RoomStatusUpdateMessage.class));
    }

    // ==========================================
    // 2. sendNotification Test Cases (UTCID03 - UTCID04)
    // ==========================================

    @Test
    @DisplayName("UTCID03 - Successful sendNotification sending payload to user queue /queue/notifications")
    void should_sendNotificationSuccessfully_when_messagingTemplateSucceeds() {
        assertDoesNotThrow(() -> webSocketService.sendNotification("user@example.com", sampleNotification));

        verify(messagingTemplate).convertAndSendToUser(eq("user@example.com"), eq("/queue/notifications"), eq(sampleNotification));
    }

    @Test
    @DisplayName("UTCID04 - Catch exception silently when sendNotification encounters MessagingException")
    void should_catchExceptionSilently_when_sendNotificationEncounterMessagingException() {
        doThrow(new MessagingException("Broker connection reset"))
                .when(messagingTemplate).convertAndSendToUser(eq("user@example.com"), eq("/queue/notifications"), eq(sampleNotification));

        assertDoesNotThrow(() -> webSocketService.sendNotification("user@example.com", sampleNotification));

        verify(messagingTemplate).convertAndSendToUser(eq("user@example.com"), eq("/queue/notifications"), eq(sampleNotification));
    }
}
