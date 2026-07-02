package com.example.hotelsmartbookingbackend.websocket;

import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final WebSocketNotificationManager notificationManager;
    private final UserRepository userRepository;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            Principal principal = event.getUser();
            if (principal != null) {
                String email = principal.getName();
                String sessionId = String.valueOf(event.getMessage().getHeaders().getId());
                
                try {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    
                    notificationManager.registerSession(user.getId(), sessionId);
                    log.info("WebSocket session connected - User: {} (ID: {}), Session: {}", 
                            email, user.getId(), sessionId);
                } catch (Exception e) {
                    log.warn("Error loading user with email: {}", email, e);
                }
            }
        } catch (Exception e) {
            log.error("Error in WebSocket connect listener", e);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            Principal principal = event.getUser();
            if (principal != null) {
                String email = principal.getName();
                String sessionId = event.getSessionId();
                
                try {
                    User user = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    
                    notificationManager.unregisterSession(user.getId(), sessionId);
                    log.info("WebSocket session disconnected - User: {} (ID: {}), Session: {}", 
                            email, user.getId(), sessionId);
                } catch (Exception e) {
                    log.warn("Error loading user with email: {}", email, e);
                }
            }
        } catch (Exception e) {
            log.error("Error in WebSocket disconnect listener", e);
        }
    }
}
