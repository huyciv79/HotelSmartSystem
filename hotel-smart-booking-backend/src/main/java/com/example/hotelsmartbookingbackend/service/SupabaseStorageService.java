package com.example.hotelsmartbookingbackend.service;

public interface SupabaseStorageService {

    String uploadAvatar(byte[] fileBytes, String originalFilename, String contentType);

    String uploadEkycDocument(byte[] fileBytes, String originalFilename, String contentType);

    void deleteAvatar(String avatarUrl);
}
