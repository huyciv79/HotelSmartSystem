package com.example.hotelsmartbookingbackend.websocket;

import com.example.hotelsmartbookingbackend.dto.websocket.WebSocketNotificationDTO;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketController {

    private final WebSocketNotificationManager notificationManager;
    private final UserRepository userRepository;

    @MessageMapping("/notifications/subscribe")
    public void subscribeToNotifications(Principal principal) {
        if (principal != null) {
            String email = principal.getName();
            try {
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                log.info("User {} (ID: {}) subscribed to notifications", email, user.getId());
            } catch (Exception e) {
                log.warn("Error subscribing user {}: {}", email, e.getMessage());
            }
        }
    }

    @MessageMapping("/notifications/ping")
    @SendTo("/user/queue/pong")
    public String ping(Principal principal) {
        if (principal != null) {
            log.debug("Ping received from user {}", principal.getName());
        }
        return "pong";
    }

    @MessageMapping("/notifications/broadcast")
    public void broadcastNotification(@Payload WebSocketNotificationDTO notification, Principal principal) {
        if (principal != null) {
            String email = principal.getName();
            try {
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                log.info("Broadcasting notification from user {} (ID: {})", email, user.getId());
                notificationManager.notifyAllUsers(notification);
            } catch (Exception e) {
                log.warn("Error broadcasting notification from user {}: {}", email, e.getMessage());
            }
        }
    }
}

