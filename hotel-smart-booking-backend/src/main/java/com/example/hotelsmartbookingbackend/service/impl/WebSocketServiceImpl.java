package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.response.RoomStatusUpdateMessage;
import com.example.hotelsmartbookingbackend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastRoomStatus(Integer roomId, String roomNumber, String status) {
        RoomStatusUpdateMessage message = RoomStatusUpdateMessage.builder()
                .roomId(roomId)
                .roomNumber(roomNumber)
                .status(status)
                .build();
        try {
            log.info("Broadcasting room status update via WS: Room {} -> {}", roomNumber, status);
            messagingTemplate.convertAndSend("/topic/room-status", message);
        } catch (Exception e) {
            log.error("Failed to broadcast room status update via WS: ", e);
        }
    }
}
