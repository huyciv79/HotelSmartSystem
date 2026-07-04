package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.response.NotificationResponse;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    
    void sendNotification(User user, String title, String message, String type, Integer referenceId);
    
    void sendNotificationToRoles(List<Role> roles, String title, String message, String type, Integer referenceId);
    
    Page<NotificationResponse> getNotifications(User user, boolean unreadOnly, Pageable pageable);
    
    void markAsRead(Integer id, User user);
    
    void markAllAsRead(User user);
    
    long getUnreadCount(User user);
}
