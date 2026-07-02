package com.example.hotelsmartbookingbackend.websocket;

import com.example.hotelsmartbookingbackend.dto.websocket.WebSocketNotificationDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class WebSocketNotificationManager {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final Map<Integer, Set<String>> userSessions = new ConcurrentHashMap<>();

    public WebSocketNotificationManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void registerSession(Integer userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> new HashSet<>()).add(sessionId);
        log.info("Session {} registered for user {}", sessionId, userId);
    }

    public void unregisterSession(Integer userId, String sessionId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
                log.info("User {} has no active sessions", userId);
            } else {
                log.info("Session {} unregistered for user {}", sessionId, userId);
            }
        }
    }

    public void notifyUser(Integer userId, WebSocketNotificationDTO notification) {
        String destination = "/user/" + userId + "/queue/notifications";
        try {
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    "/queue/notifications",
                    notification
            );
            log.debug("Notification sent to user {}: {}", userId, notification.getAction());
        } catch (Exception e) {
            log.error("Failed to send notification to user {}", userId, e);
        }
    }

    public void notifyAllUsers(WebSocketNotificationDTO notification) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/broadcast",
                    notification
            );
            log.debug("Broadcast notification sent: {}", notification.getAction());
        } catch (Exception e) {
            log.error("Failed to send broadcast notification", e);
        }
    }

    public boolean isUserOnline(Integer userId) {
        return userSessions.containsKey(userId) && !userSessions.get(userId).isEmpty();
    }

    public int getActiveSessionCount(Integer userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null ? sessions.size() : 0;
    }

    public int getTotalActiveSessions() {
        return userSessions.values().stream().mapToInt(Set::size).sum();
    }

    public int getTotalActiveUsers() {
        return userSessions.size();
    }
}

