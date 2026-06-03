package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.UpdateProfileRequest;
import com.example.hotelsmartbookingbackend.dto.response.UserProfileDTO;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetUserProfile_Success() {
        User user = new User();
        user.setId(1);
        user.setEmail("test@example.com");
        user.setFullname("Test User");
        user.setRole(Role.customer);
        user.setPhonenumber("0987654321");
        user.setAvatar("http://avatar.url");
        user.setAddress("Hanoi");
        user.setStatus("Active");
        user.setCreatedat(Instant.now());

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserProfileDTO profile = userService.getUserProfile("test@example.com");

        assertNotNull(profile);
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("Test User", profile.getFullName());
        assertEquals("customer", profile.getRole());
    }

    @Test
    void testUpdateUserProfile_Success() {
        User user = new User();
        user.setId(1);
        user.setEmail("test@example.com");
        user.setFullname("Old Name");
        user.setAddress("Old Address");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("New Name");
        request.setAddress("New Address");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileDTO profile = userService.updateUserProfile("test@example.com", request);

        assertNotNull(profile);
        assertEquals("New Name", profile.getFullName());
        assertEquals("New Address", profile.getAddress());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testUploadAvatar_Success() throws IOException {
        User user = new User();
        user.setId(1);
        user.setEmail("test@example.com");
        user.setAvatar("http://supabase.com/old_avatar.png");

        // Generate a valid PNG image in-memory
        java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(bufferedImage, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                pngBytes
        );

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(supabaseStorageService.uploadAvatar(any(byte[].class), anyString(), anyString()))
                .thenReturn("http://supabase.com/new_avatar.png");

        String newUrl = userService.uploadAvatar("test@example.com", file);

        assertEquals("http://supabase.com/new_avatar.png", newUrl);
        assertEquals("http://supabase.com/new_avatar.png", user.getAvatar());

        // Verify that old avatar deletion was called
        verify(supabaseStorageService, times(1)).deleteAvatar("http://supabase.com/old_avatar.png");
        verify(userRepository, times(1)).save(user);
    }
}
