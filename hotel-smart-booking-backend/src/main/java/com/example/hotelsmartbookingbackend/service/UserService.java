package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.UpdateProfileRequest;
import com.example.hotelsmartbookingbackend.dto.response.UserProfileDTO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    /**
     * Retrieve the user's profile details.
     *
     * @param email The email of the user.
     * @return UserProfileDTO containing complete details of the user.
     */
    UserProfileDTO getUserProfile(String email);

    /**
     * Update user profile information.
     *
     * @param email   The email of the user.
     * @param request The UpdateProfileRequest DTO containing fullName and address.
     * @return Updated UserProfileDTO details.
     */
    UserProfileDTO updateUserProfile(String email, UpdateProfileRequest request);

    /**
     * Upload and update user's avatar.
     *
     * @param email The email of the user.
     * @param file  The uploaded multipart file.
     * @return The public URL of the uploaded avatar.
     */
    String uploadAvatar(String email, MultipartFile file);
}
