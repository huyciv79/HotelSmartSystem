package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.NotificationResponse;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Principal principal) {
        User user = resolveCurrentUser(principal);
        Pageable pageable = PageRequest.of(page, size);
        Page<NotificationResponse> result = notificationService.getNotifications(user, unreadOnly, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách thông báo thành công", result));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @PathVariable("id") Integer id,
            Principal principal) {
        User user = resolveCurrentUser(principal);
        notificationService.markAsRead(id, user);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu thông báo là đã đọc", "SUCCESS"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(Principal principal) {
        User user = resolveCurrentUser(principal);
        notificationService.markAllAsRead(user);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả thông báo là đã đọc", "SUCCESS"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Principal principal) {
        User user = resolveCurrentUser(principal);
        long count = notificationService.getUnreadCount(user);
        return ResponseEntity.ok(ApiResponse.success("Lấy số lượng thông báo chưa đọc thành công", count));
    }

    private User resolveCurrentUser(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Bạn cần đăng nhập để thực hiện chức năng này");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));
    }
}
