package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.service.impl.SupabaseStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SupabaseStorageServiceImplTest {

    @InjectMocks
    private SupabaseStorageServiceImpl supabaseStorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(supabaseStorageService, "supabaseUrl", "https://test.supabase.co");
        ReflectionTestUtils.setField(supabaseStorageService, "supabaseKey", "test-key-12345");
        ReflectionTestUtils.setField(supabaseStorageService, "avatarBucket", "avt-image");
        ReflectionTestUtils.setField(supabaseStorageService, "roomtypeBucket", "roomtype-image");
        ReflectionTestUtils.setField(supabaseStorageService, "ekycBucket", "ekyc-documents");
    }

    @Test
    @DisplayName("Should throw exception when supabaseUrl is missing during upload")
    void testUploadAvatar_MissingUrl() {
        ReflectionTestUtils.setField(supabaseStorageService, "supabaseUrl", "");
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            supabaseStorageService.uploadAvatar("test".getBytes(), "avatar.jpg", "image/jpeg")
        );
        
        assertTrue(exception.getMessage().contains("missing URL or Key"));
    }

    @Test
    @DisplayName("Should return null or fallback when pathOrUrl is invalid in getSignedUrl")
    void testGetSignedUrl_InvalidPath() {
        String result = supabaseStorageService.getSignedUrl(null);
        assertNull(result);

        String blankResult = supabaseStorageService.getSignedUrl("");
        assertNull(blankResult);
    }
}
