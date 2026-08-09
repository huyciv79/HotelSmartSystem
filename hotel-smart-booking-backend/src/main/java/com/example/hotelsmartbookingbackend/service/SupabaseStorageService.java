package com.example.hotelsmartbookingbackend.service;

import org.springframework.http.MediaType;

import java.util.Optional;

public interface SupabaseStorageService {

    String uploadAvatar(byte[] fileBytes, String originalFilename, String contentType);

    String uploadEkycDocument(byte[] fileBytes, String originalFilename, String contentType);

    String getSignedUrl(String pathOrUrl);

    Optional<StoredFile> downloadEkycDocument(String pathOrUrl);

    record StoredFile(byte[] bytes, MediaType contentType) {
    }


    String uploadRoomTypeImage(byte[] fileBytes, String originalFilename, String contentType);

    void deleteRoomTypeImage(String imageUrl);

    void deleteAvatar(String avatarUrl);
}
