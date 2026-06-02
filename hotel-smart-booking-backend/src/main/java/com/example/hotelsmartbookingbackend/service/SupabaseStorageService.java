package com.example.hotelsmartbookingbackend.service;

public interface SupabaseStorageService {
    /**
     * Uploads the avatar image to Supabase Storage.
     *
     * @param fileBytes        The byte array of the image.
     * @param originalFilename The original filename.
     * @param contentType      The MIME content type of the image.
     * @return The public URL of the uploaded image.
     */
    String uploadAvatar(byte[] fileBytes, String originalFilename, String contentType);

    /**
     * Safely deletes the old avatar image from Supabase Storage.
     *
     * @param avatarUrl The public URL of the avatar image to delete.
     */
    void deleteAvatar(String avatarUrl);
}
