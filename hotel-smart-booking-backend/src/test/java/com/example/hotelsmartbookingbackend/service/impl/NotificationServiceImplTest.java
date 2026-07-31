package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.response.NotificationResponse;
import com.example.hotelsmartbookingbackend.entity.Notification;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.NotificationRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User targetUser;
    private User secondUser;

    @BeforeEach
    void setUp() {
        targetUser = new User();
        targetUser.setId(1);
        targetUser.setEmail("user1@example.com");
        targetUser.setFullName("User One");
        targetUser.setRole(Role.customer);

        secondUser = new User();
        secondUser.setId(2);
        secondUser.setEmail("user2@example.com");
        secondUser.setFullName("User Two");
        secondUser.setRole(Role.receptionist);
    }

    // ==========================================
    // 1. sendNotification Test Cases (UTCID01 - UTCID03)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful sendNotification with WebSocket push")
    void should_sendNotificationSuccessfully_when_validUserAndContent() {
        Notification savedNotification = new Notification();
        savedNotification.setId(10);
        savedNotification.setUser(targetUser);
        savedNotification.setTitle("Booking Confirmation");
        savedNotification.setMessage("Your booking is confirmed");
        savedNotification.setType("BOOKING");
        savedNotification.setReferenceId(100);
        savedNotification.setIsRead(false);
        savedNotification.setSentAt(Instant.now());

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.sendNotification(targetUser, "Booking Confirmation", "Your booking is confirmed", "BOOKING", 100);

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendNotification(eq("user1@example.com"), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("UTCID02 - Catch WebSocket exception silently and keep notification saved")
    void should_catchExceptionAndLog_when_webSocketServiceThrowsException() {
        Notification savedNotification = new Notification();
        savedNotification.setId(10);
        savedNotification.setUser(targetUser);
        savedNotification.setTitle("Booking Confirmation");
        savedNotification.setMessage("Message");
        savedNotification.setType("BOOKING");
        savedNotification.setReferenceId(100);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);
        doThrow(new RuntimeException("WebSocket connection failed"))
                .when(webSocketService).sendNotification(anyString(), any());

        assertDoesNotThrow(() -> notificationService.sendNotification(targetUser, "Booking Confirmation", "Message", "BOOKING", 100));

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendNotification(eq("user1@example.com"), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("UTCID03 - Successful sendNotification when referenceId is null")
    void should_sendNotificationSuccessfully_when_referenceIdIsNull() {
        Notification savedNotification = new Notification();
        savedNotification.setId(11);
        savedNotification.setUser(targetUser);
        savedNotification.setTitle("System Update");
        savedNotification.setMessage("Maintenance tonight");
        savedNotification.setType("SYSTEM");
        savedNotification.setReferenceId(null);

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        notificationService.sendNotification(targetUser, "System Update", "Maintenance tonight", "SYSTEM", null);

        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).sendNotification(eq("user1@example.com"), any(NotificationResponse.class));
    }

    // ==========================================
    // 2. sendNotificationToRoles Test Cases (UTCID04 - UTCID05)
    // ==========================================

    @Test
    @DisplayName("UTCID04 - Successful sendNotificationToRoles when target users exist")
    void should_sendNotificationToRoles_when_targetUsersExist() {
        List<Role> roles = List.of(Role.receptionist, Role.manager);
        when(userRepository.findAllByRoleIn(roles)).thenReturn(List.of(targetUser, secondUser));

        Notification saved = new Notification();
        saved.setId(1);
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        notificationService.sendNotificationToRoles(roles, "Role Alert", "Broadcast message", "ROLE", 50);

        verify(userRepository).findAllByRoleIn(roles);
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(webSocketService, times(2)).sendNotification(anyString(), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("UTCID05 - Do nothing when no users are found for specified roles")
    void should_doNothing_when_noUsersFoundForRoles() {
        List<Role> roles = List.of(Role.customer);
        when(userRepository.findAllByRoleIn(roles)).thenReturn(List.of());

        notificationService.sendNotificationToRoles(roles, "Role Alert", "Message", "ROLE", 50);

        verify(userRepository).findAllByRoleIn(roles);
        verify(notificationRepository, never()).save(any());
        verifyNoInteractions(webSocketService);
    }

    // ==========================================
    // 3. createNotification Test Cases (UTCID06 - UTCID08, UTCID18)
    // ==========================================

    @Test
    @DisplayName("UTCID06 - Populate default sentAt and isRead when both are null")
    void should_populateDefaultsAndSave_when_sentAtAndIsReadAreNull() {
        Notification notification = new Notification();
        notification.setTitle("Test Title");
        notification.setSentAt(null);
        notification.setIsRead(null);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(notification);

        assertNotNull(result.getSentAt());
        assertFalse(result.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("UTCID07 - Preserve existing sentAt and isRead=true when present")
    void should_preserveSentAtAndIsRead_when_alreadyPresent() {
        Instant presetInstant = Instant.parse("2026-07-01T10:00:00Z");
        Notification notification = new Notification();
        notification.setTitle("Preset Title");
        notification.setSentAt(presetInstant);
        notification.setIsRead(true);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(notification);

        assertEquals(presetInstant, result.getSentAt());
        assertTrue(result.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("UTCID08 - Preserve sentAt and populate isRead=false when isRead is null")
    void should_populateIsReadDefault_when_sentAtIsPresetAndIsReadIsNull() {
        Instant presetInstant = Instant.parse("2026-07-01T10:00:00Z");
        Notification notification = new Notification();
        notification.setTitle("Preset Time");
        notification.setSentAt(presetInstant);
        notification.setIsRead(null);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(notification);

        assertEquals(presetInstant, result.getSentAt());
        assertFalse(result.getIsRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("UTCID18 - Populate sentAt default and preserve pre-populated isRead=true")
    void should_populateSentAtDefaultAndPreserveIsReadTrue_when_sentAtIsNullAndIsReadIsTrue() {
        Notification notification = new Notification();
        notification.setTitle("SentAt Null Read True");
        notification.setSentAt(null);
        notification.setIsRead(true);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        Notification result = notificationService.createNotification(notification);

        assertNotNull(result.getSentAt());
        assertTrue(result.getIsRead());
        verify(notificationRepository).save(notification);
    }

    // ==========================================
    // 4. getNotifications Test Cases (UTCID09 - UTCID10)
    // ==========================================

    @Test
    @DisplayName("UTCID09 - Get unread notifications when unreadOnly is true")
    void should_getUnreadNotifications_when_unreadOnlyIsTrue() {
        Pageable pageable = PageRequest.of(0, 10);
        Notification n = new Notification();
        n.setId(1);
        n.setUser(targetUser);
        n.setTitle("Unread Item");
        n.setIsRead(false);
        Page<Notification> page = new PageImpl<>(List.of(n), pageable, 1);

        when(notificationRepository.findByUserAndIsReadOrderBySentAtDesc(targetUser, false, pageable)).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getNotifications(targetUser, true, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Unread Item", result.getContent().get(0).getTitle());
        verify(notificationRepository).findByUserAndIsReadOrderBySentAtDesc(targetUser, false, pageable);
        verify(notificationRepository, never()).findByUserOrderBySentAtDesc(any(), any());
    }

    @Test
    @DisplayName("UTCID10 - Get all notifications when unreadOnly is false")
    void should_getAllNotifications_when_unreadOnlyIsFalse() {
        Pageable pageable = PageRequest.of(0, 10);
        Notification n = new Notification();
        n.setId(1);
        n.setUser(targetUser);
        n.setTitle("All Item");
        n.setIsRead(true);
        Page<Notification> page = new PageImpl<>(List.of(n), pageable, 1);

        when(notificationRepository.findByUserOrderBySentAtDesc(targetUser, pageable)).thenReturn(page);

        Page<NotificationResponse> result = notificationService.getNotifications(targetUser, false, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("All Item", result.getContent().get(0).getTitle());
        verify(notificationRepository).findByUserOrderBySentAtDesc(targetUser, pageable);
        verify(notificationRepository, never()).findByUserAndIsReadOrderBySentAtDesc(any(), anyBoolean(), any());
    }

    // ==========================================
    // 5. markAsRead Test Cases (UTCID11 - UTCID14)
    // ==========================================

    @Test
    @DisplayName("UTCID11 - Mark unread notification as read successfully")
    void should_markAsReadSuccessfully_when_validNotificationAndOwner() {
        Notification unreadNotification = new Notification();
        unreadNotification.setId(10);
        unreadNotification.setUser(targetUser);
        unreadNotification.setIsRead(false);

        when(notificationRepository.findById(10)).thenReturn(Optional.of(unreadNotification));

        notificationService.markAsRead(10, targetUser);

        assertTrue(unreadNotification.getIsRead());
        assertNotNull(unreadNotification.getReadAt());
        verify(notificationRepository).save(unreadNotification);
    }

    @Test
    @DisplayName("UTCID12 - Throw exception when notification ID is not found for markAsRead")
    void should_throwException_when_markAsReadWithNotificationNotFound() {
        when(notificationRepository.findById(999)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead(999, targetUser));

        assertEquals("Không tìm thấy thông báo", ex.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID13 - Throw exception when user is not notification owner for markAsRead")
    void should_throwException_when_markAsReadWithUserNotOwner() {
        Notification otherNotification = new Notification();
        otherNotification.setId(10);
        otherNotification.setUser(secondUser);

        when(notificationRepository.findById(10)).thenReturn(Optional.of(otherNotification));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> notificationService.markAsRead(10, targetUser));

        assertEquals("Bạn không có quyền đánh dấu đã đọc thông báo này", ex.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID14 - Skip save when notification is already read")
    void should_skipSave_when_notificationIsAlreadyRead() {
        Notification readNotification = new Notification();
        readNotification.setId(10);
        readNotification.setUser(targetUser);
        readNotification.setIsRead(true);
        readNotification.setReadAt(Instant.parse("2026-07-01T10:00:00Z"));

        when(notificationRepository.findById(10)).thenReturn(Optional.of(readNotification));

        notificationService.markAsRead(10, targetUser);

        assertTrue(readNotification.getIsRead());
        verify(notificationRepository, never()).save(any());
    }

    // ==========================================
    // 6. markAllAsRead Test Cases (UTCID15 - UTCID16)
    // ==========================================

    @Test
    @DisplayName("UTCID15 - Mark all unread notifications as read when unread list is non-empty")
    void should_markAllAsReadSuccessfully_when_unreadNotificationsExist() {
        Notification n1 = new Notification();
        n1.setId(1);
        n1.setIsRead(false);

        Notification n2 = new Notification();
        n2.setId(2);
        n2.setIsRead(false);

        List<Notification> unreadList = List.of(n1, n2);
        when(notificationRepository.findByUserAndIsRead(targetUser, false)).thenReturn(unreadList);

        notificationService.markAllAsRead(targetUser);

        assertTrue(n1.getIsRead());
        assertNotNull(n1.getReadAt());
        assertTrue(n2.getIsRead());
        assertNotNull(n2.getReadAt());
        verify(notificationRepository).saveAll(unreadList);
    }

    @Test
    @DisplayName("UTCID16 - Save empty list when no unread notifications exist")
    void should_saveEmptyList_when_noUnreadNotificationsExist() {
        when(notificationRepository.findByUserAndIsRead(targetUser, false)).thenReturn(List.of());

        notificationService.markAllAsRead(targetUser);

        verify(notificationRepository).saveAll(List.of());
    }

    // ==========================================
    // 7. getUnreadCount Test Cases (UTCID17, UTCID19)
    // ==========================================

    @Test
    @DisplayName("UTCID17 - Get unread count successfully")
    void should_getUnreadCountSuccessfully_when_userHasUnreadNotifications() {
        when(notificationRepository.countByUserAndIsRead(targetUser, false)).thenReturn(5L);

        long count = notificationService.getUnreadCount(targetUser);

        assertEquals(5L, count);
        verify(notificationRepository).countByUserAndIsRead(targetUser, false);
    }

    @Test
    @DisplayName("UTCID19 - Get unread count returns zero when no unread notifications exist")
    void should_returnZero_when_userHasNoUnreadNotifications() {
        when(notificationRepository.countByUserAndIsRead(targetUser, false)).thenReturn(0L);

        long count = notificationService.getUnreadCount(targetUser);

        assertEquals(0L, count);
        verify(notificationRepository).countByUserAndIsRead(targetUser, false);
    }
}
