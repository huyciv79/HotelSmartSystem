package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.UpdateProfileRequest;
import com.example.hotelsmartbookingbackend.dto.response.UserProfileResponse;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private UserServiceImpl userService;

    private User sampleUser;
    private MultipartFile validPngFile;
    private MultipartFile validGifFile;
    private MultipartFile validJpgFile;
    private MultipartFile validJpegFile;

    // 1x1 Pixel Valid PNG ByteArray for Thumbnailator testing
    private static final byte[] VALID_PNG_BYTES = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D,
        0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08,
        0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89, 0x00, 0x00, 0x00,
        0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
        0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49,
        0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
    };

    // 1x1 Pixel Valid GIF89a ByteArray for Thumbnailator GIF writer testing
    private static final byte[] VALID_GIF_BYTES = new byte[] {
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00, (byte) 0x80, 0x00,
        0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00, 0x21, (byte) 0xF9,
        0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2C, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00,
        0x01, 0x00, 0x00, 0x02, 0x02, 0x44, 0x01, 0x00, 0x3B
    };

    @BeforeEach
    void setUp() throws IOException {
        sampleUser = new User();
        sampleUser.setId(1);
        sampleUser.setEmail("user@example.com");
        sampleUser.setFullName("Nguyen Van A");
        sampleUser.setRole(Role.customer);
        sampleUser.setPhoneNumber("0912345678");
        sampleUser.setIdCardNumber("123456789012");
        sampleUser.setAddress("Hanoi, Vietnam");
        sampleUser.setAvatar("https://cdn.supabase.co/old_avatar.jpg");
        sampleUser.setStatus("Active");
        sampleUser.setCreatedAt(Instant.now());
        sampleUser.setUpdatedAt(Instant.now());

        validPngFile = mock(MultipartFile.class);
        lenient().when(validPngFile.isEmpty()).thenReturn(false);
        lenient().when(validPngFile.getSize()).thenReturn(1024L);
        lenient().when(validPngFile.getContentType()).thenReturn("image/png");
        lenient().when(validPngFile.getOriginalFilename()).thenReturn("avatar.png");
        lenient().when(validPngFile.getInputStream()).thenAnswer(i -> new ByteArrayInputStream(VALID_PNG_BYTES));

        validGifFile = mock(MultipartFile.class);
        lenient().when(validGifFile.isEmpty()).thenReturn(false);
        lenient().when(validGifFile.getSize()).thenReturn(1024L);
        lenient().when(validGifFile.getContentType()).thenReturn("image/gif");
        lenient().when(validGifFile.getOriginalFilename()).thenReturn("user.gif");
        lenient().when(validGifFile.getInputStream()).thenAnswer(i -> new ByteArrayInputStream(VALID_GIF_BYTES));

        validJpgFile = mock(MultipartFile.class);
        lenient().when(validJpgFile.isEmpty()).thenReturn(false);
        lenient().when(validJpgFile.getSize()).thenReturn(1024L);
        lenient().when(validJpgFile.getContentType()).thenReturn("image/jpg");
        lenient().when(validJpgFile.getOriginalFilename()).thenReturn("user.jpg");
        lenient().when(validJpgFile.getInputStream()).thenAnswer(i -> new ByteArrayInputStream(VALID_PNG_BYTES));

        validJpegFile = mock(MultipartFile.class);
        lenient().when(validJpegFile.isEmpty()).thenReturn(false);
        lenient().when(validJpegFile.getSize()).thenReturn(1024L);
        lenient().when(validJpegFile.getContentType()).thenReturn("image/jpeg");
        lenient().when(validJpegFile.getOriginalFilename()).thenReturn("user.jpeg");
        lenient().when(validJpegFile.getInputStream()).thenAnswer(i -> new ByteArrayInputStream(VALID_PNG_BYTES));
    }

    // ==========================================
    // 1. getUserProfile Test Cases (UTCID01 - UTCID03)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful getUserProfile when email exists and role is populated")
    void should_getUserProfileSuccessfully_when_emailExistsWithRole() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser));

        UserProfileResponse response = userService.getUserProfile("user@example.com");

        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("customer", response.getRole());
        assertEquals("Nguyen Van A", response.getFullName());
    }

    @Test
    @DisplayName("UTCID02 - Successful getUserProfile when email exists and role is null")
    void should_getUserProfileSuccessfully_when_emailExistsWithNullRole() {
        sampleUser.setRole(null);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser));

        UserProfileResponse response = userService.getUserProfile("user@example.com");

        assertNotNull(response);
        assertNull(response.getRole());
    }

    @Test
    @DisplayName("UTCID03 - Throw exception when getUserProfile with non-existing email")
    void should_throwException_when_getUserProfileWithEmailNotFound() {
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.getUserProfile("notfound@example.com"));

        assertEquals("Không tìm thấy thông tin người dùng với email: notfound@example.com", ex.getMessage());
    }

    // ==========================================
    // 2. updateUserProfile Test Cases (UTCID04 - UTCID05)
    // ==========================================

    @Test
    @DisplayName("UTCID04 - Successful updateUserProfile updating fullName and address")
    void should_updateUserProfileSuccessfully_when_emailExists() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Nguyen Van B");
        request.setAddress("Da Nang, Vietnam");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileResponse response = userService.updateUserProfile("user@example.com", request);

        assertNotNull(response);
        assertEquals("Nguyen Van B", sampleUser.getFullName());
        assertEquals("Da Nang, Vietnam", sampleUser.getAddress());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("UTCID05 - Throw exception when updateUserProfile with non-existing email")
    void should_throwException_when_updateUserProfileWithEmailNotFound() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.updateUserProfile("notfound@example.com", request));

        assertEquals("Không tìm thấy thông tin người dùng với email: notfound@example.com", ex.getMessage());
    }

    // ==========================================
    // 3. uploadAvatar Test Cases (UTCID06 - UTCID15)
    // ==========================================

    @Test
    @DisplayName("UTCID06 - Successful uploadAvatar for PNG file, updating DB and deleting old avatar")
    void should_uploadAvatarSuccessfully_when_validPngFileAndOldAvatarExists() {
        when(supabaseStorageService.uploadAvatar(any(), eq("avatar.png"), eq("image/png")))
                .thenReturn("https://cdn.supabase.co/new_avatar.png");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser));

        String newAvatarUrl = userService.uploadAvatar("user@example.com", validPngFile);

        assertEquals("https://cdn.supabase.co/new_avatar.png", newAvatarUrl);
        assertEquals("https://cdn.supabase.co/new_avatar.png", sampleUser.getAvatar());
        verify(userRepository).save(sampleUser);
        verify(supabaseStorageService).deleteAvatar("https://cdn.supabase.co/old_avatar.jpg");
    }

    @Test
    @DisplayName("UTCID07 - Successful uploadAvatar for GIF file skipping old avatar deletion when oldAvatar is null")
    void should_uploadAvatarSuccessfully_when_validGifFileAndOldAvatarIsNull() {
        sampleUser.setAvatar(null);

        when(supabaseStorageService.uploadAvatar(any(), eq("user.gif"), eq("image/gif")))
                .thenReturn("https://cdn.supabase.co/new_avatar.gif");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser));

        String newAvatarUrl = userService.uploadAvatar("user@example.com", validGifFile);

        assertEquals("https://cdn.supabase.co/new_avatar.gif", newAvatarUrl);
        verify(supabaseStorageService, never()).deleteAvatar(any());
    }

    @Test
    @DisplayName("UTCID08 - Successful uploadAvatar for JPG file")
    void should_uploadAvatarSuccessfully_when_validJpgFileProvided() {
        sampleUser.setAvatar(""); // Blank old avatar

        when(supabaseStorageService.uploadAvatar(any(), eq("user.jpg"), eq("image/jpg")))
                .thenReturn("https://cdn.supabase.co/new_avatar.jpg");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser));

        String newAvatarUrl = userService.uploadAvatar("user@example.com", validJpgFile);

        assertEquals("https://cdn.supabase.co/new_avatar.jpg", newAvatarUrl);
        verify(supabaseStorageService, never()).deleteAvatar(any());
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when uploadAvatar with null or empty file")
    void should_throwException_when_uploadAvatarWithNullOrEmptyFile() {
        MultipartFile emptyFile = mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);

        RuntimeException ex1 = assertThrows(RuntimeException.class,
                () -> userService.uploadAvatar("user@example.com", null));
        assertEquals("Tệp tải lên không được để trống", ex1.getMessage());

        RuntimeException ex2 = assertThrows(RuntimeException.class,
                () -> userService.uploadAvatar("user@example.com", emptyFile));
        assertEquals("Tệp tải lên không được để trống", ex2.getMessage());
    }

    @Test
    @DisplayName("UTCID10 - Throw exception when uploadAvatar file size exceeds 5MB limit")
    void should_throwException_when_uploadAvatarExceeds5MbSizeLimit() {
        MultipartFile largeFile = mock(MultipartFile.class);
        when(largeFile.isEmpty()).thenReturn(false);
        when(largeFile.getSize()).thenReturn(6 * 1024 * 1024L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.uploadAvatar("user@example.com", largeFile));

        assertEquals("Kích thước tệp vượt quá giới hạn cho phép (tối đa 5MB)", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID11 - Throw exception when uploadAvatar with invalid content type")
    void should_throwException_when_uploadAvatarWithInvalidContentType() {
        MultipartFile pdfFile = mock(MultipartFile.class);
        when(pdfFile.isEmpty()).thenReturn(false);
        when(pdfFile.getSize()).thenReturn(1024L);
        when(pdfFile.getContentType()).thenReturn("application/pdf");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.uploadAvatar("user@example.com", pdfFile));

        assertEquals("Định dạng tệp không hợp lệ. Chỉ chấp nhận các định dạng ảnh: JPG, JPEG, PNG, GIF", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID12 - Throw exception when uploadAvatar user email not found in DB")
    void should_throwException_when_uploadAvatarWithEmailNotFound() {
        when(supabaseStorageService.uploadAvatar(any(), any(), any())).thenReturn("https://cdn.supabase.co/new.png");
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.uploadAvatar("notfound@example.com", validPngFile));

        assertEquals("Không tìm thấy thông tin người dùng với email: notfound@example.com", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID13 - Throw exception when Thumbnailator encounters IOException during image processing")
    void should_throwException_when_thumbnailatorFailsToProcessImage() throws IOException {
        MultipartFile corruptedFile = mock(MultipartFile.class);
        when(corruptedFile.isEmpty()).thenReturn(false);
        when(corruptedFile.getSize()).thenReturn(1024L);
        when(corruptedFile.getContentType()).thenReturn("image/png");
        when(corruptedFile.getInputStream()).thenThrow(new IOException("Disk read failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.uploadAvatar("user@example.com", corruptedFile));

        assertTrue(ex.getMessage().startsWith("Có lỗi xảy ra khi tải ảnh đại diện lên: Disk read failure"));
    }

    @Test
    @DisplayName("UTCID14 - Rethrow RuntimeException when Supabase Storage upload throws RuntimeException")
    void should_rethrowException_when_supabaseStorageUploadFails() {
        when(supabaseStorageService.uploadAvatar(any(), any(), any()))
                .thenThrow(new RuntimeException("Storage upload failure"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.uploadAvatar("user@example.com", validPngFile));

        assertEquals("Storage upload failure", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID15 - Successful uploadAvatar for JPEG file format")
    void should_uploadAvatarSuccessfully_when_validJpegFileProvided() {
        when(supabaseStorageService.uploadAvatar(any(), eq("user.jpeg"), eq("image/jpeg")))
                .thenReturn("https://cdn.supabase.co/new_avatar.jpeg");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(sampleUser));

        String newAvatarUrl = userService.uploadAvatar("user@example.com", validJpegFile);

        assertEquals("https://cdn.supabase.co/new_avatar.jpeg", newAvatarUrl);
    }
}
