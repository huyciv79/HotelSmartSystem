package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.response.NotificationResponse;
import com.example.hotelsmartbookingbackend.entity.Notification;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.NotificationRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.NotificationService;
import com.example.hotelsmartbookingbackend.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;

    @Override
    @Transactional
    public void sendNotification(User user, String title, String message, String type, Integer referenceId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setIsRead(false);
        notification.setSentAt(Instant.now());

        Notification saved = notificationRepository.save(notification);

        try {
            webSocketService.sendNotification(user.getEmail(), mapToResponse(saved));
        } catch (Exception e) {
            log.error("Failed to send real-time notification to user {}: ", user.getEmail(), e);
        }
    }

    @Override
    @Transactional
    public void sendNotificationToRoles(List<Role> roles, String title, String message, String type, Integer referenceId) {
        List<User> targetUsers = userRepository.findAllByRoleIn(roles);
        for (User user : targetUsers) {
            sendNotification(user, title, message, type, referenceId);
        }
    }

    @Transactional
    public Notification createNotification(Notification notification) {
        if (notification.getSentAt() == null) {
            notification.setSentAt(Instant.now());
        }
        if (notification.getIsRead() == null) {
            notification.setIsRead(false);
        }
        return notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(User user, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page;
        if (unreadOnly) {
            page = notificationRepository.findByUserAndIsReadOrderBySentAtDesc(user, false, pageable);
        } else {
            page = notificationRepository.findByUserOrderBySentAtDesc(user, pageable);
        }
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void markAsRead(Integer id, User user) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Bạn không có quyền đánh dấu đã đọc thông báo này");
        }
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserAndIsRead(user, false);
        Instant now = Instant.now();
        for (Notification notification : unread) {
            notification.setIsRead(true);
            notification.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType())
                .referenceid(n.getReferenceId())
                .isread(n.getIsRead())
                .readat(n.getReadAt())
                .sentat(n.getSentAt())
                .build();
    }
}
