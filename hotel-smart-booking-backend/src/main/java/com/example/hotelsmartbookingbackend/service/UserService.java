package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.UpdateProfileRequest;
import com.example.hotelsmartbookingbackend.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    /**
     * Retrieve the user's profile details.
     *
     * @param email The email of the user.
     * @return UserProfileDTO containing complete details of the user.
     */
    UserProfileResponse getUserProfile(String email);

    /**
     * Update user profile information.
     *
     * @param email   The email of the user.
     * @param request The UpdateProfileRequest DTO containing fullName and address.
     * @return Updated UserProfileDTO details.
     */
    UserProfileResponse updateUserProfile(String email, UpdateProfileRequest request);

    /**
     * Upload and update user's avatar.
     *
     * @param email The email of the user.
     * @param file  The uploaded multipart file.
     * @return The public URL of the uploaded avatar.
     */
    String uploadAvatar(String email, MultipartFile file);
}
