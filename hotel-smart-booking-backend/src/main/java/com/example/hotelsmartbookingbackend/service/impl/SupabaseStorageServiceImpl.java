package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Service
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.avatar-bucket}")
    private String avatarBucket;

    @Value("${supabase.roomtype-bucket}")
    private String roomtypeBucket;

    @Value("${supabase.ekyc-bucket:ekyc-documents}")
    private String ekycBucket;

    @Override
    public String uploadAvatar(byte[] fileBytes, String originalFilename, String contentType) {
        if (supabaseUrl == null || supabaseUrl.isBlank()
                || supabaseKey == null || supabaseKey.isBlank()) {
            throw new RuntimeException("Supabase storage is not configured properly (missing URL or Key)");
        }
        if (avatarBucket == null || avatarBucket.isBlank()) {
            throw new RuntimeException("Supabase storage is not configured properly (missing avatar bucket)");
        }

        String extension = resolveImageExtension(originalFilename, contentType);
        String fileName = "avatar_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String uploadUrl = cleanUrl + "/storage/v1/object/" + avatarBucket + "/" + fileName;

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"));

            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to upload avatar to Supabase Storage: " + response.getStatusCode());
            }

            return cleanUrl + "/storage/v1/object/public/" + avatarBucket + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải ảnh lên hệ thống lưu trữ: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadEkycDocument(byte[] fileBytes, String originalFilename, String contentType) {
        if (supabaseUrl == null || supabaseUrl.isBlank()
                || supabaseKey == null || supabaseKey.isBlank()) {
            throw new RuntimeException("Supabase storage is not configured properly (missing URL or Key)");
        }
        if (ekycBucket == null || ekycBucket.isBlank()) {
            throw new RuntimeException("Supabase storage is not configured properly (missing ekyc bucket)");
        }

        String extension = resolveImageExtension(originalFilename, contentType);
        String fileName = "ekyc_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        if (originalFilename != null && originalFilename.startsWith("ekyc_") && originalFilename.contains(".")) {
            fileName = originalFilename.substring(0, originalFilename.lastIndexOf(".")) + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;
        }

        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String uploadUrl = cleanUrl + "/storage/v1/object/" + ekycBucket + "/" + fileName;

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"));

            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to upload ekyc file to Supabase Storage: " + response.getStatusCode());
            }

            return createSignedUrl(cleanUrl, ekycBucket, fileName);

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tải ảnh eKYC lên hệ thống lưu trữ: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadRoomTypeImage(byte[] fileBytes, String originalFilename, String contentType) {
        if (supabaseUrl == null || supabaseUrl.isBlank()
                || supabaseKey == null || supabaseKey.isBlank()) {
            throw new RuntimeException("Supabase storage is not configured properly (missing URL or Key)");
        }
        if (roomtypeBucket == null || roomtypeBucket.isBlank()) {
            throw new RuntimeException("Supabase storage is not configured properly (missing room type bucket)");
        }

        String fileName = "roomtype_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + resolveImageExtension(originalFilename, contentType);
        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String uploadUrl = cleanUrl + "/storage/v1/object/" + roomtypeBucket + "/" + fileName;

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"));

            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to upload room type image to Supabase Storage: " + response.getStatusCode());
            }

            return cleanUrl + "/storage/v1/object/public/" + roomtypeBucket + "/" + fileName;

        } catch (Exception e) {
            throw new RuntimeException("Loi khi tai anh loai phong len he thong luu tru: " + e.getMessage(), e);
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
                    return fullSignedUrl;
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        // Fallback: trả về public URL nếu tạo signed URL thất bại
        return cleanUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
    }

    @Override
    public void deleteAvatar(String avatarUrl) {
        deleteStorageObject(avatarUrl, avatarBucket, "avatar");
    }

    @Override
    public void deleteRoomTypeImage(String imageUrl) {
        deleteStorageObject(imageUrl, roomtypeBucket, "room type image");
    }

    private void deleteStorageObject(String publicUrl, String bucket, String objectLabel) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }

        if (supabaseUrl == null || supabaseUrl.isBlank()
                || supabaseKey == null || supabaseKey.isBlank()
                || bucket == null || bucket.isBlank()) {
            return;
        }

        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        String publicPrefix = cleanUrl + "/storage/v1/object/public/" + bucket + "/";

        if (!publicUrl.startsWith(publicPrefix)) {
            return;
        }

        String fileName = publicUrl.substring(publicPrefix.length());
        String deleteUrl = cleanUrl + "/storage/v1/object/" + bucket + "/" + fileName;

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, entity, Void.class);
        } catch (Exception e) {
            // Thất bại khi xóa cũng bỏ qua
        }
    }

    @Override
    public String getSignedUrl(String pathOrUrl) {
        String fileName = extractFileName(pathOrUrl);
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        String cleanUrl = supabaseUrl.endsWith("/") ? supabaseUrl.substring(0, supabaseUrl.length() - 1) : supabaseUrl;
        return createSignedUrl(cleanUrl, ekycBucket, fileName);
    }

    private String extractFileName(String urlOrPath) {
        if (urlOrPath == null || urlOrPath.isBlank()) {
            return null;
        }
        String keyword = "/" + ekycBucket + "/";
        int index = urlOrPath.indexOf(keyword);
        if (index != -1) {
            String afterKeyword = urlOrPath.substring(index + keyword.length());
            int questionMarkIndex = afterKeyword.indexOf("?");
            if (questionMarkIndex != -1) {
                return afterKeyword.substring(0, questionMarkIndex);
            }
            return afterKeyword;
        }
        String publicKeyword = "/public/" + ekycBucket + "/";
        int pubIndex = urlOrPath.indexOf(publicKeyword);
        if (pubIndex != -1) {
            String afterKeyword = urlOrPath.substring(pubIndex + publicKeyword.length());
            int questionMarkIndex = afterKeyword.indexOf("?");
            if (questionMarkIndex != -1) {
                return afterKeyword.substring(0, questionMarkIndex);
            }
            return afterKeyword;
        }
        if (urlOrPath.startsWith("http")) {
            int lastSlash = urlOrPath.lastIndexOf("/");
            if (lastSlash != -1) {
                String afterSlash = urlOrPath.substring(lastSlash + 1);
                int questionMarkIndex = afterSlash.indexOf("?");
                if (questionMarkIndex != -1) {
                    return afterSlash.substring(0, questionMarkIndex);
                }
                return afterSlash;
            }
        }
        return urlOrPath;
    }

    private String resolveImageExtension(String originalFilename, String contentType) {
        if (contentType != null) {
            if (contentType.contains("png")) {
                return "png";
            }
            if (contentType.contains("gif")) {
                return "gif";
            }
            if (contentType.contains("webp")) {
                return "webp";
            }
            if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                return "jpg";
            }
        }
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        }
        return "jpg";
    }
}
