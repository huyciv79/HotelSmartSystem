package com.example.hotelsmartbookingbackend.service;

public interface WebSocketService {
    void broadcastRoomStatus(Integer roomId, String roomNumber, String status);
    void sendNotification(String email, com.example.hotelsmartbookingbackend.dto.response.NotificationResponse notification);
}
