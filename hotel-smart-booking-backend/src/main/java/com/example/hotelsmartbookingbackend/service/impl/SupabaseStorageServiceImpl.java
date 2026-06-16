package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
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
        return uploadFile(fileBytes, originalFilename, contentType, supabaseBucket);
    }

    @Override
    public String uploadEkycDocument(byte[] fileBytes, String originalFilename, String contentType) {
        return uploadFile(fileBytes, originalFilename, contentType, "ekyc-documents");
    }

    private String uploadFile(byte[] fileBytes, String originalFilename, String contentType, String targetBucket) {
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

        String fileName = targetBucket.equals("avatars") ? "avatar_" : "ekyc_";
        fileName += System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        if (originalFilename != null && originalFilename.startsWith("ekyc_") && originalFilename.contains(".")) {
            fileName = originalFilename.substring(0, originalFilename.lastIndexOf(".")) + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        }

        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String uploadUrl = cleanUrl + "/storage/v1/object/" + targetBucket + "/" + fileName;

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

            log.info("Successfully uploaded file to Supabase storage. File: {}", fileName);
            return createSignedUrl(cleanUrl, targetBucket, fileName);

        } catch (Exception e) {
            log.error("Error occurred while uploading file to Supabase: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi tải ảnh lên hệ thống lưu trữ: " + e.getMessage());
        }
    }

    /**
     * Tạo Signed URL cho file vừa upload, có hiệu lực 3600 giây (1 giờ).
     * Cho phép AI Service tải ảnh từ bucket private mà không cần xác thực.
     */
    private String createSignedUrl(String cleanUrl, String bucket, String fileName) {
        String signedUrlEndpoint = cleanUrl + "/storage/v1/object/sign/" + bucket + "/" + fileName;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Supabase API yêu cầu body: {"expiresIn": 3600}
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(Map.of("expiresIn", 3600), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(signedUrlEndpoint, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = new ObjectMapper().readTree(response.getBody());
                String signedPath = json.path("signedURL").asText("");
                if (!signedPath.isBlank()) {
                    // Supabase trả về signedPath dạng "/object/sign/..." (thiếu "/storage/v1")
                    // Cần thêm "/storage/v1" để tạo URL đúng
                    String fullSignedUrl;
                    if (signedPath.startsWith("http")) {
                        fullSignedUrl = signedPath;
                    } else if (!signedPath.startsWith("/storage/")) {
                        fullSignedUrl = cleanUrl + "/storage/v1" + signedPath;
                    } else {
                        fullSignedUrl = cleanUrl + signedPath;
                    }
                    log.info("Created signed URL for file: {}", fileName);
                    return fullSignedUrl;
                }
            }
            log.warn("Không tạo được signed URL cho {}. Fallback về public URL.", fileName);
        } catch (Exception e) {
            log.warn("Lỗi khi tạo signed URL cho {}: {}. Fallback về public URL.", fileName, e.getMessage());
        }
        // Fallback: trả về public URL nếu tạo signed URL thất bại
        return cleanUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
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
