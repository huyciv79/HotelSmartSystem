package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@Slf4j
public class SupabaseStorageServiceImpl implements SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.avatar-bucket}")
    private String avatarBucket;

    @Value("${supabase.roomtype-bucket}")
    private String roomtypeBucket;

    @Override
    public String uploadAvatar(byte[] fileBytes, String originalFilename, String contentType) {

        if (supabaseUrl == null || supabaseUrl.isBlank()
                || supabaseKey == null || supabaseKey.isBlank()) {
            throw new RuntimeException(
                    "Supabase storage is not configured properly (missing URL or Key)");
        }
        if (avatarBucket == null || avatarBucket.isBlank()) {
            throw new RuntimeException(
                    "Supabase storage is not configured properly (missing avatar bucket)");
        }

        String extension = resolveImageExtension(originalFilename, contentType);

        String fileName =
                "avatar_"
                        + System.currentTimeMillis()
                        + "_"
                        + UUID.randomUUID().toString().substring(0, 8)
                        + "."
                        + extension;

        String cleanUrl =
                supabaseUrl.endsWith("/")
                        ? supabaseUrl.substring(0, supabaseUrl.length() - 1)
                        : supabaseUrl;

        String uploadUrl =
                cleanUrl
                        + "/storage/v1/object/"
                        + avatarBucket
                        + "/"
                        + fileName;

        log.info("UPLOAD AVATAR CALLED");
        log.info("avatarBucket = {}", avatarBucket);
        log.info("roomtypeBucket = {}", roomtypeBucket);
        log.info("uploadUrl = {}", uploadUrl);

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(
                    MediaType.parseMediaType(
                            contentType != null ? contentType : "image/jpeg"));

            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            uploadUrl,
                            entity,
                            String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Failed to upload image to Supabase Storage: "
                                + response.getStatusCode());
            }

            log.info(
                    "Successfully uploaded avatar to bucket {}. File: {}",
                    avatarBucket,
                    fileName);

            return cleanUrl
                    + "/storage/v1/object/public/"
                    + avatarBucket
                    + "/"
                    + fileName;

        } catch (Exception e) {

            log.error(
                    "Error occurred while uploading avatar to Supabase: {}",
                    e.getMessage(),
                    e);

            throw new RuntimeException(
                    "Lỗi khi tải ảnh lên hệ thống lưu trữ: "
                            + e.getMessage());
        }
    }

    @Override
    public String uploadRoomTypeImage(byte[] fileBytes, String originalFilename, String contentType) {

        if (supabaseUrl == null || supabaseUrl.isBlank()
                || supabaseKey == null || supabaseKey.isBlank()) {
            throw new RuntimeException(
                    "Supabase storage is not configured properly (missing URL or Key)");
        }
        if (roomtypeBucket == null || roomtypeBucket.isBlank()) {
            throw new RuntimeException(
                    "Supabase storage is not configured properly (missing room type bucket)");
        }

        String fileName =
                "roomtype_"
                        + System.currentTimeMillis()
                        + "_"
                        + UUID.randomUUID().toString().substring(0, 8)
                        + "."
                        + resolveImageExtension(originalFilename, contentType);

        String cleanUrl =
                supabaseUrl.endsWith("/")
                        ? supabaseUrl.substring(0, supabaseUrl.length() - 1)
                        : supabaseUrl;

        String uploadUrl =
                cleanUrl
                        + "/storage/v1/object/"
                        + roomtypeBucket
                        + "/"
                        + fileName;

        log.info("UPLOAD ROOM TYPE IMAGE CALLED");
        log.info("roomtypeBucket = {}", roomtypeBucket);
        log.info("uploadUrl = {}", uploadUrl);

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);
            headers.setContentType(
                    MediaType.parseMediaType(
                            contentType != null ? contentType : "image/jpeg"));

            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            uploadUrl,
                            entity,
                            String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Failed to upload room type image to Supabase Storage: "
                                + response.getStatusCode());
            }

            log.info(
                    "Successfully uploaded room type image to bucket {}. File: {}",
                    roomtypeBucket,
                    fileName);

            return cleanUrl
                    + "/storage/v1/object/public/"
                    + roomtypeBucket
                    + "/"
                    + fileName;

        } catch (Exception e) {

            log.error(
                    "Error occurred while uploading room type image to Supabase: {}",
                    e.getMessage(),
                    e);

            throw new RuntimeException(
                    "Loi khi tai anh loai phong len he thong luu tru: "
                            + e.getMessage());
        }
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

            log.warn(
                    "Supabase configuration is missing. Skipping delete {}.",
                    objectLabel);

            return;
        }

        String cleanUrl =
                supabaseUrl.endsWith("/")
                        ? supabaseUrl.substring(0, supabaseUrl.length() - 1)
                        : supabaseUrl;

        String publicPrefix =
                cleanUrl
                        + "/storage/v1/object/public/"
                        + bucket
                        + "/";

        if (!publicUrl.startsWith(publicPrefix)) {

            log.info(
                    "{} URL {} is not hosted in bucket {}, skipping deletion.",
                    objectLabel,
                    publicUrl,
                    bucket);

            return;
        }

        String fileName = publicUrl.substring(publicPrefix.length());

        String deleteUrl =
                cleanUrl
                        + "/storage/v1/object/"
                        + bucket
                        + "/"
                        + fileName;

        try {

            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + supabaseKey);
            headers.set("apikey", supabaseKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    entity,
                    Void.class);

            log.info(
                    "Successfully deleted {} from bucket {}: {}",
                    objectLabel,
                    bucket,
                    fileName);

        } catch (Exception e) {

            log.error(
                    "Failed to delete {} from Supabase Storage: {}",
                    objectLabel,
                    e.getMessage());
        }
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
