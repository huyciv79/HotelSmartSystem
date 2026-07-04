package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.NotificationRequest;
import com.example.hotelsmartbookingbackend.dto.response.NotificationResponse;
import com.example.hotelsmartbookingbackend.entity.Notification;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl {

    private final NotificationRepository notificationRepository;

    public NotificationResponse getUserNotifications(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.example.hotelsmartbookingbackend.dto.response.Notification> notificationsPage = notificationRepository
                .findByUserOrderBySentatDesc(user, pageable)
                .map(com.example.hotelsmartbookingbackend.dto.response.Notification::fromEntity);

        long unreadCount = notificationRepository.countByUserAndIsread(user, false);

        return NotificationResponse.fromPage(notificationsPage, unreadCount);
    }

    public NotificationResponse getUnreadNotifications(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.example.hotelsmartbookingbackend.dto.response.Notification> notificationsPage = notificationRepository
                .findByUserAndIsreadOrderBySentatDesc(user, false, pageable)
                .map(com.example.hotelsmartbookingbackend.dto.response.Notification::fromEntity);

        long unreadCount = notificationRepository.countByUserAndIsread(user, false);

        return NotificationResponse.fromPage(notificationsPage, unreadCount);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByUserAndIsread(user, false);
    }

    @Transactional
    public com.example.hotelsmartbookingbackend.dto.response.Notification markAsRead(User user, Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notificationRepository.markAsRead(user, notificationId, Instant.now());
        notification = notificationRepository.findById(notificationId).orElseThrow();

        log.info("Notification {} marked as read for user {}", notificationId, user.getId());
        return com.example.hotelsmartbookingbackend.dto.response.Notification.fromEntity(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsRead(user, Instant.now());
        log.info("All notifications marked as read for user {}", user.getId());
    }

    @Transactional
    public void deleteNotification(User user, Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to notification");
        }

        notificationRepository.deleteByUserAndId(user, notificationId);
        log.info("Notification {} deleted for user {}", notificationId, user.getId());
    }

    @Transactional
    public void deleteAllNotifications(User user) {
        notificationRepository.findByUserOrderBySentatDesc(user).forEach(notification ->
                notificationRepository.deleteByUserAndId(user, notification.getId())
        );
        log.info("All notifications deleted for user {}", user.getId());
    }

    @Transactional
    public Notification createNotification(Notification notification) {
        notification.setSentat(Instant.now());
        notification.setIsread(false);
        notification.setReadat(null);

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created with id {} for user {}", saved.getId(),
                saved.getUser() != null ? saved.getUser().getId() : "broadcast");

        return saved;
    }

    @Transactional
    public Notification sendNotificationToUser(User user, NotificationRequest request) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(request.getTitle());
        notification.setMessage(request.getMessage());
        notification.setType(request.getType() != null ? request.getType() : "System");
        notification.setReferenceid(request.getReferenceId());
        notification.setIsread(false);
        notification.setSentat(Instant.now());

        return createNotification(notification);
    }
}
