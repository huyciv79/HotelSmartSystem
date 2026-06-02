package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.UpdateProfileRequest;
import com.example.hotelsmartbookingbackend.dto.response.UserProfileDTO;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import com.example.hotelsmartbookingbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SupabaseStorageService supabaseStorageService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    @Override
    public UserProfileDTO getUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng với email: " + email));
        return mapToProfileDTO(user);
    }

    @Override
    @Transactional
    public UserProfileDTO updateUserProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng với email: " + email));

        user.setFullname(request.getFullName());
        user.setAddress(request.getAddress());
        user.setUpdatedat(Instant.now());

        userRepository.save(user);
        log.info("Successfully updated user profile for email: {}", email);

        return mapToProfileDTO(user);
    }

    @Override
    @Transactional
    public String uploadAvatar(String email, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Tệp tải lên không được để trống");
        }

        // Validate File Size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Kích thước tệp vượt quá giới hạn cho phép (tối đa 5MB)");
        }

        // Validate Content Type
        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg") || contentType.equals("image/jpg") ||
                contentType.equals("image/png") || contentType.equals("image/gif"))) {
            throw new RuntimeException("Định dạng tệp không hợp lệ. Chỉ chấp nhận các định dạng ảnh: JPG, JPEG, PNG, GIF");
        }

        try {
            // Resize Image using Thumbnailator to exactly 400x400 pixels
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String format = "jpg";
            if (contentType.contains("png")) {
                format = "png";
            } else if (contentType.contains("gif")) {
                format = "gif";
            }

            Thumbnails.of(file.getInputStream())
                    .size(400, 400)
                    .keepAspectRatio(false) // Force exact 400x400 square dimensions
                    .outputFormat(format)
                    .toOutputStream(outputStream);

            byte[] resizedImageBytes = outputStream.toByteArray();

            // Upload the resized avatar to Supabase
            String newAvatarUrl = supabaseStorageService.uploadAvatar(resizedImageBytes, file.getOriginalFilename(), contentType);

            // Update user in DB and retrieve old avatar URL
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng với email: " + email));

            String oldAvatarUrl = user.getAvatar();
            user.setAvatar(newAvatarUrl);
            user.setUpdatedat(Instant.now());
            userRepository.save(user);

            // Clean up old avatar from Supabase Storage
            if (oldAvatarUrl != null && !oldAvatarUrl.isBlank()) {
                log.info("Deleting old avatar: {}", oldAvatarUrl);
                supabaseStorageService.deleteAvatar(oldAvatarUrl);
            }

            return newAvatarUrl;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to process and upload avatar for email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Có lỗi xảy ra khi tải ảnh đại diện lên: " + e.getMessage());
        }
    }

    private UserProfileDTO mapToProfileDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .phoneNumber(user.getPhonenumber())
                .fullName(user.getFullname())
                .avatar(user.getAvatar())
                .address(user.getAddress())
                .status(user.getStatus())
                .createdAt(user.getCreatedat())
                .updatedAt(user.getUpdatedat())
                .build();
    }
}
