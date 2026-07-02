package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.websocket.WebSocketNotificationDTO;
import com.example.hotelsmartbookingbackend.entity.Notification;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.impl.AuthenticationServiceImpl;
import com.example.hotelsmartbookingbackend.service.impl.NotificationServiceImpl;
import com.example.hotelsmartbookingbackend.dto.request.NotificationRequest;
import com.example.hotelsmartbookingbackend.dto.response.NotificationResponse;
import com.example.hotelsmartbookingbackend.websocket.WebSocketNotificationManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationServiceImpl notificationService;
    private final WebSocketNotificationManager webSocketNotificationManager;
    private final AuthenticationServiceImpl authenticationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<NotificationResponse> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = authenticationService.getUserFromAuthentication(authentication);
        NotificationResponse response = notificationService.getUserNotifications(user, page, size);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/unread")
    public ResponseEntity<NotificationResponse> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = authenticationService.getUserFromAuthentication(authentication);
        NotificationResponse response = notificationService.getUnreadNotifications(user, page, size);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/count/unread")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = authenticationService.getUserFromAuthentication(authentication);
        long unreadCount = notificationService.getUnreadCount(user);
        
        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", unreadCount);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<com.example.hotelsmartbookingbackend.dto.response.Notification> markAsRead(
            @PathVariable Integer id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = authenticationService.getUserFromAuthentication(authentication);
        com.example.hotelsmartbookingbackend.dto.response.Notification notificationDto = notificationService.markAsRead(user, id);
        
        webSocketNotificationManager.notifyUser(user.getId(), 
                WebSocketNotificationDTO.ofRead(notificationDto));
        
        return ResponseEntity.ok(notificationDto);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = authenticationService.getUserFromAuthentication(authentication);
        notificationService.markAllAsRead(user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications marked as read");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Integer id,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = authenticationService.getUserFromAuthentication(authentication);
        notificationService.deleteNotification(user, id);
        
        webSocketNotificationManager.notifyUser(user.getId(), 
                WebSocketNotificationDTO.ofDelete(id));
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification deleted successfully");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete-all")
    public ResponseEntity<Map<String, String>> deleteAllNotifications(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = authenticationService.getUserFromAuthentication(authentication);
        notificationService.deleteAllNotifications(user);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "All notifications deleted successfully");
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send")
    public ResponseEntity<com.example.hotelsmartbookingbackend.dto.response.Notification> sendNotification(
            @Valid @RequestBody NotificationRequest request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User currentUser = authenticationService.getUserFromAuthentication(authentication);
        
        if (request.getBroadcastToAll() != null && request.getBroadcastToAll()) {
            Notification notification = new Notification();
            notification.setTitle(request.getTitle());
            notification.setMessage(request.getMessage());
            notification.setType(request.getType() != null ? request.getType() : "System");
            notification.setReferenceid(request.getReferenceId());
            notification.setIsread(false);
            
            Notification saved = notificationService.createNotification(notification);
            webSocketNotificationManager.notifyAllUsers(WebSocketNotificationDTO.ofNew(
                    com.example.hotelsmartbookingbackend.dto.response.Notification.fromEntity(saved)));
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(com.example.hotelsmartbookingbackend.dto.response.Notification.fromEntity(saved));
        } else if (request.getUserId() != null) {
            User targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Target user not found"));
            
            Notification notification = notificationService.sendNotificationToUser(targetUser, request);
            webSocketNotificationManager.notifyUser(targetUser.getId(), 
                    WebSocketNotificationDTO.ofNew(com.example.hotelsmartbookingbackend.dto.response.Notification.fromEntity(notification)));
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(com.example.hotelsmartbookingbackend.dto.response.Notification.fromEntity(notification));
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = authenticationService.getUserFromAuthentication(authentication);
        
        Map<String, Object> status = new HashMap<>();
        status.put("userId", user.getId());
        status.put("email", user.getEmail());
        status.put("isOnline", webSocketNotificationManager.isUserOnline(user.getId()));
        status.put("activeSessionCount", webSocketNotificationManager.getActiveSessionCount(user.getId()));
        status.put("totalActiveSessions", webSocketNotificationManager.getTotalActiveSessions());
        status.put("totalActiveUsers", webSocketNotificationManager.getTotalActiveUsers());
        
        return ResponseEntity.ok(status);
    }
}

