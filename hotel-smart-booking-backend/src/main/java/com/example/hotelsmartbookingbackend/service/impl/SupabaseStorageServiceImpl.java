package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@Slf4j
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.key:}")
    private String supabaseKey;

    @Value("${supabase.bucket:avatars}")
    private String supabaseBucket;

    @Override
    public String uploadAvatar(byte[] fileBytes, String originalFilename, String contentType) {
        if (supabaseUrl == null || supabaseUrl.isBlank() || supabaseKey == null || supabaseKey.isBlank()) {
            throw new RuntimeException("Supabase storage is not configured properly (missing URL or Key)");
        }

        String extension = "jpg";
        if (contentType != null) {
            if (contentType.contains("png")) {
                extension = "png";
            } else if (contentType.contains("gif")) {
                extension = "gif";
            } else if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                extension = "jpg";
            }
        } else if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }

        String fileName = "avatar_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String uploadUrl = cleanUrl + "/storage/v1/object/" + supabaseBucket + "/" + fileName;

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"));

            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to upload image to Supabase Storage: status " + response.getStatusCode());
            }

            log.info("Successfully uploaded avatar to Supabase storage. File: {}", fileName);
            return cleanUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + fileName;

        } catch (Exception e) {
            log.error("Error occurred while uploading avatar to Supabase: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tải ảnh lên hệ thống lưu trữ: " + e.getMessage());
        }
    }

    @Override
    public void deleteAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            return;
        }

        if (supabaseUrl == null || supabaseUrl.isBlank() || supabaseKey == null || supabaseKey.isBlank()) {
            log.warn("Supabase configuration is missing. Skipping delete old avatar.");
            return;
        }

        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String publicPrefix = cleanUrl + "/storage/v1/object/public/" + supabaseBucket + "/";

        if (!avatarUrl.startsWith(publicPrefix)) {
            log.info("Avatar URL {} is not hosted in our bucket {}, skipping deletion.", avatarUrl, supabaseBucket);
            return;
        }

        String fileName = avatarUrl.substring(publicPrefix.length());
        String deleteUrl = cleanUrl + "/storage/v1/object/" + supabaseBucket + "/" + fileName;

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, Void.class);
            log.info("Successfully deleted old avatar from Supabase Storage: {}", fileName);
        } catch (Exception e) {
            // Log the error but do not propagate it to prevent breaking the profile update transaction
            log.error("Failed to delete old avatar from Supabase Storage: {}", e.getMessage());
        }
    }
}
