package com.example.hotelsmartbookingbackend.service;

public interface WebSocketService {
    void broadcastRoomStatus(Integer roomId, String roomNumber, String status);
}
