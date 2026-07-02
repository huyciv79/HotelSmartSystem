package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.websocket.WebSocketNotificationDTO;
import com.example.hotelsmartbookingbackend.entity.Notification;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.websocket.WebSocketNotificationManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisherImpl {

    private final NotificationServiceImpl notificationService;
    private final WebSocketNotificationManager webSocketNotificationManager;

    @Async
    public void publishNotificationToUser(User user, String title, String message, String type, Integer referenceId) {
        try {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type != null ? type : "System");
            notification.setReferenceid(referenceId);
            notification.setIsread(false);

            Notification saved = notificationService.createNotification(notification);
            com.example.hotelsmartbookingbackend.dto.response.Notification notificationDTO = com.example.hotelsmartbookingbackend.dto.response.Notification.fromEntity(saved);
            
            webSocketNotificationManager.notifyUser(user.getId(), 
                    WebSocketNotificationDTO.ofNew(notificationDTO));
            
            log.info("Async notification published to user {}", user.getId());
        } catch (Exception e) {
            log.error("Error publishing notification to user {}", user.getId(), e);
        }
    }

    @Async
    public void publishBroadcastNotification(String title, String message, String type, Integer referenceId) {
        try {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type != null ? type : "System");
            notification.setReferenceid(referenceId);
            notification.setIsread(false);

            Notification saved = notificationService.createNotification(notification);
            com.example.hotelsmartbookingbackend.dto.response.Notification notificationDTO = com.example.hotelsmartbookingbackend.dto.response.Notification.fromEntity(saved);
            
            webSocketNotificationManager.notifyAllUsers(WebSocketNotificationDTO.ofNew(notificationDTO));
            
            log.info("Broadcast notification published");
        } catch (Exception e) {
            log.error("Error publishing broadcast notification", e);
        }
    }

    public void publishNotificationToUserSync(User user, String title, String message, String type, Integer referenceId) {
        try {
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type != null ? type : "System");
            notification.setReferenceid(referenceId);
            notification.setIsread(false);

            Notification saved = notificationService.createNotification(notification);
            com.example.hotelsmartbookingbackend.dto.response.Notification notificationDTO = com.example.hotelsmartbookingbackend.dto.response.Notification.fromEntity(saved);
            
            webSocketNotificationManager.notifyUser(user.getId(), 
                    WebSocketNotificationDTO.ofNew(notificationDTO));
            
            log.info("Sync notification published to user {}", user.getId());
        } catch (Exception e) {
            log.error("Error publishing notification to user {}", user.getId(), e);
        }
    }

    public void publishBroadcastNotificationSync(String title, String message, String type, Integer referenceId) {
        try {
            Notification notification = new Notification();
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type != null ? type : "System");
            notification.setReferenceid(referenceId);
            notification.setIsread(false);

            Notification saved = notificationService.createNotification(notification);
            com.example.hotelsmartbookingbackend.dto.response.Notification notificationDTO = com.example.hotelsmartbookingbackend.dto.response.Notification.fromEntity(saved);
            
            webSocketNotificationManager.notifyAllUsers(WebSocketNotificationDTO.ofNew(notificationDTO));
            
            log.info("Sync broadcast notification published");
        } catch (Exception e) {
            log.error("Error publishing sync broadcast notification", e);
        }
    }
}

