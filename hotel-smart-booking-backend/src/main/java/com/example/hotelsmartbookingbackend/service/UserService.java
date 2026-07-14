package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.UpdateProfileRequest;
import com.example.hotelsmartbookingbackend.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserProfileResponse getUserProfile(String email);

    UserProfileResponse updateUserProfile(String email, UpdateProfileRequest request);

    String uploadAvatar(String email, MultipartFile file);
}
