package com.example.hotelsmartbookingbackend.service;

public interface SupabaseStorageService {

    String uploadAvatar(byte[] fileBytes, String originalFilename, String contentType);

    String uploadEkycDocument(byte[] fileBytes, String originalFilename, String contentType);

    String getSignedUrl(String pathOrUrl);

    /**
     * Uploads a room type image to Supabase Storage.
     *
     * @param fileBytes The image file bytes.
     * @param originalFilename The original file name.
     * @param contentType The image content type.
     * @return The public URL of the uploaded room type image.
     */
    String uploadRoomTypeImage(byte[] fileBytes, String originalFilename, String contentType);

    /**
     * Safely deletes a room type image from Supabase Storage.
     *
     * @param imageUrl The public URL of the room type image to delete.
     */
    void deleteRoomTypeImage(String imageUrl);

    /**
     * Safely deletes the old avatar image from Supabase Storage.
     *
     * @param avatarUrl The public URL of the avatar image to delete.
     */
    void deleteAvatar(String avatarUrl);
}
