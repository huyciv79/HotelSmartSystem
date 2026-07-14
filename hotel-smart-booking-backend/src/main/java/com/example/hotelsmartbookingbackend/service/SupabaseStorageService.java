package com.example.hotelsmartbookingbackend.service;

public interface SupabaseStorageService {

    String uploadAvatar(byte[] fileBytes, String originalFilename, String contentType);

    String uploadEkycDocument(byte[] fileBytes, String originalFilename, String contentType);

    String getSignedUrl(String pathOrUrl);


    String uploadRoomTypeImage(byte[] fileBytes, String originalFilename, String contentType);

    void deleteRoomTypeImage(String imageUrl);

    void deleteAvatar(String avatarUrl);
}
