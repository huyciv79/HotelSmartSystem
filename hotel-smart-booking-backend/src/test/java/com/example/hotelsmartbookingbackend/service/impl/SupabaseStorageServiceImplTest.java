package com.example.hotelsmartbookingbackend.service.impl;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

class SupabaseStorageServiceImplTest {

    private SupabaseStorageServiceImpl storageService;
    private HttpServer mockServer;
    private String serverUrl;
    private int responseStatusCode = 200;
    private String responseBody = "{\"signedURL\": \"/storage/v1/object/sign/ekyc-documents/doc.jpg?token=abc\"}";

    @BeforeEach
    void setUp() throws IOException {
        storageService = new SupabaseStorageServiceImpl();

        // Start lightweight in-memory HTTP server to mock Supabase REST API
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/", exchange -> {
            byte[] responseBytes = responseBody.getBytes();
            exchange.sendResponseHeaders(responseStatusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });
        mockServer.start();

        int port = mockServer.getAddress().getPort();
        serverUrl = "http://localhost:" + port;

        // Configure default Reflection properties
        ReflectionTestUtils.setField(storageService, "supabaseUrl", serverUrl);
        ReflectionTestUtils.setField(storageService, "supabaseKey", "test-api-key");
        ReflectionTestUtils.setField(storageService, "avatarBucket", "avatars");
        ReflectionTestUtils.setField(storageService, "roomtypeBucket", "roomtypes");
        ReflectionTestUtils.setField(storageService, "ekycBucket", "ekyc-documents");
        
        responseStatusCode = 200;
        responseBody = "{\"signedURL\": \"/storage/v1/object/sign/ekyc-documents/doc.jpg?token=abc\"}";
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    // ==========================================
    // 1. uploadAvatar Test Cases (UTCID01 - UTCID06, UTCID28)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful uploadAvatar with valid PNG file")
    void should_uploadAvatarSuccessfully_when_validPngFileProvided() {
        byte[] fileBytes = new byte[]{1, 2, 3};

        String avatarUrl = storageService.uploadAvatar(fileBytes, "avatar.png", "image/png");

        assertNotNull(avatarUrl);
        assertTrue(avatarUrl.startsWith(serverUrl + "/storage/v1/object/public/avatars/avatar_"));
        assertTrue(avatarUrl.endsWith(".png"));
    }

    @Test
    @DisplayName("UTCID02 - Successful uploadAvatar resolving .gif extension when contentType is null")
    void should_uploadAvatarSuccessfully_when_contentTypeIsNullAndExtensionResolvedFromFilename() {
        byte[] fileBytes = new byte[]{1, 2, 3};

        String avatarUrl = storageService.uploadAvatar(fileBytes, "user.gif", null);

        assertNotNull(avatarUrl);
        assertTrue(avatarUrl.endsWith(".gif"));
    }

    @Test
    @DisplayName("UTCID03 - Throw exception when uploadAvatar with missing supabaseUrl or supabaseKey")
    void should_throwException_when_uploadAvatarWithMissingSupabaseUrlOrKey() {
        ReflectionTestUtils.setField(storageService, "supabaseUrl", null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> storageService.uploadAvatar(new byte[]{1}, "a.jpg", "image/jpeg"));

        assertEquals("Supabase storage is not configured properly (missing URL or Key)", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID04 - Throw exception when uploadAvatar with missing avatarBucket")
    void should_throwException_when_uploadAvatarWithMissingAvatarBucket() {
        ReflectionTestUtils.setField(storageService, "avatarBucket", "   ");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> storageService.uploadAvatar(new byte[]{1}, "a.jpg", "image/jpeg"));

        assertEquals("Supabase storage is not configured properly (missing avatar bucket)", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID05 - Throw exception when uploadAvatar receives non-2xx response from Supabase")
    void should_throwException_when_uploadAvatarReceivesNon2xxStatusFromSupabase() {
        responseStatusCode = 500;
        responseBody = "Internal Error";

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> storageService.uploadAvatar(new byte[]{1}, "a.jpg", "image/jpeg"));

        assertTrue(ex.getMessage().startsWith("Lỗi khi tải ảnh lên hệ thống lưu trữ:"));
    }

    @Test
    @DisplayName("UTCID06 - Throw exception when uploadAvatar encounters network exception")
    void should_throwException_when_uploadAvatarEncountersNetworkException() {
        ReflectionTestUtils.setField(storageService, "supabaseUrl", "http://invalid-unreachable-host-99999.com");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> storageService.uploadAvatar(new byte[]{1}, "a.jpg", "image/jpeg"));

        assertTrue(ex.getMessage().startsWith("Lỗi khi tải ảnh lên hệ thống lưu trữ:"));
    }

    @Test
    @DisplayName("UTCID28 - Successful uploadAvatar resolving .jpg extension for image/jpeg contentType")
    void should_resolveJpgExtension_when_contentTypeIsImageJpeg() {
        String avatarUrl = storageService.uploadAvatar(new byte[]{1, 2}, "photo.jpeg", "image/jpeg");

        assertNotNull(avatarUrl);
        assertTrue(avatarUrl.endsWith(".jpg"));
    }

    // ==========================================
    // 2. uploadEkycDocument Test Cases (UTCID07 - UTCID11, UTCID25 - UTCID27)
    // ==========================================

    @Test
    @DisplayName("UTCID07 - Successful uploadEkycDocument returning signed URL for filename starting with ekyc_")
    void should_uploadEkycDocumentSuccessfully_when_validFileAndEkycPrefixFilenameProvided() {
        byte[] fileBytes = new byte[]{1, 2, 3};

        String signedUrl = storageService.uploadEkycDocument(fileBytes, "ekyc_front.jpg", "image/jpeg");

        assertNotNull(signedUrl);
        assertTrue(signedUrl.contains("/storage/v1/object/sign/ekyc-documents/doc.jpg?token=abc"));
    }

    @Test
    @DisplayName("UTCID08 - Fallback to public URL when createSignedUrl receives non-2xx status")
    void should_fallbackToPublicUrl_when_createSignedUrlFailsOrReturnsNon2xx() {
        responseStatusCode = 200;
        responseBody = "{}";

        byte[] fileBytes = new byte[]{1, 2, 3};

        String signedUrl = storageService.uploadEkycDocument(fileBytes, "card.png", "image/png");

        assertNotNull(signedUrl);
        assertTrue(signedUrl.contains("/storage/v1/object/public/ekyc-documents/ekyc_"));
    }

    @Test
    @DisplayName("UTCID09 - Return full signed URL when Supabase sign endpoint returns absolute HTTP URL")
    void should_returnFullSignedUrl_when_supabaseSignEndpointReturnsHttpSignedUrl() {
        responseBody = "{\"signedURL\": \"http://external-supabase-cdn.com/sign/doc.jpg\"}";

        String signedUrl = storageService.uploadEkycDocument(new byte[]{1}, "card.png", "image/png");

        assertEquals("http://external-supabase-cdn.com/sign/doc.jpg", signedUrl);
    }

    @Test
    @DisplayName("UTCID10 - Throw exception when uploadEkycDocument with missing ekycBucket")
    void should_throwException_when_uploadEkycDocumentWithMissingEkycBucket() {
        ReflectionTestUtils.setField(storageService, "ekycBucket", null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> storageService.uploadEkycDocument(new byte[]{1}, "card.png", "image/png"));

        assertEquals("Supabase storage is not configured properly (missing ekyc bucket)", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID11 - Throw exception when uploadEkycDocument receives 400 Bad Request on upload")
    void should_throwException_when_uploadEkycDocumentReceives400BadRequest() {
        responseStatusCode = 400;

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> storageService.uploadEkycDocument(new byte[]{1}, "card.png", "image/png"));

        assertTrue(ex.getMessage().startsWith("Lỗi khi tải ảnh eKYC lên hệ thống lưu trữ:"));
    }

    @Test
    @DisplayName("UTCID25 - Parse signedUrl key when signedURL key is absent in JSON response")
    void should_parseSignedUrlPropertyKey_when_signedURLKeyIsAbsentInJsonResponse() {
        responseBody = "{\"signedUrl\": \"/object/sign/ekyc-documents/doc.jpg\"}";

        String signedUrl = storageService.uploadEkycDocument(new byte[]{1}, "card.png", "image/png");

        assertEquals(serverUrl + "/storage/v1/object/sign/ekyc-documents/doc.jpg", signedUrl);
    }

    @Test
    @DisplayName("UTCID26 - Prepend /storage/v1 when signed path does not start with /storage/")
    void should_prependStorageV1_when_signedPathDoesNotStartWithStorage() {
        responseBody = "{\"signedURL\": \"/object/sign/ekyc-documents/doc.jpg\"}";

        String signedUrl = storageService.uploadEkycDocument(new byte[]{1}, "card.png", "image/png");

        assertEquals(serverUrl + "/storage/v1/object/sign/ekyc-documents/doc.jpg", signedUrl);
    }

    @Test
    @DisplayName("UTCID27 - Prepend cleanUrl only when signed path starts with /storage/")
    void should_prependCleanUrlOnly_when_signedPathStartsWithStorage() {
        responseBody = "{\"signedURL\": \"/storage/v1/object/sign/ekyc-documents/doc.jpg\"}";

        String signedUrl = storageService.uploadEkycDocument(new byte[]{1}, "card.png", "image/png");

        assertEquals(serverUrl + "/storage/v1/object/sign/ekyc-documents/doc.jpg", signedUrl);
    }

    // ==========================================
    // 3. uploadRoomTypeImage Test Cases (UTCID12 - UTCID16)
    // ==========================================

    @Test
    @DisplayName("UTCID12 - Successful uploadRoomTypeImage with WEBP format")
    void should_uploadRoomTypeImageSuccessfully_when_validWebpFileProvided() {
        byte[] fileBytes = new byte[]{1, 2, 3};

        String imageUrl = storageService.uploadRoomTypeImage(fileBytes, "room.webp", "image/webp");

        assertNotNull(imageUrl);
        assertTrue(imageUrl.startsWith(serverUrl + "/storage/v1/object/public/roomtypes/roomtype_"));
        assertTrue(imageUrl.endsWith(".webp"));
    }

    @Test
    @DisplayName("UTCID13 - Throw exception when uploadRoomTypeImage with missing roomtypeBucket")
    void should_throwException_when_uploadRoomTypeImageWithMissingRoomTypeBucket() {
        ReflectionTestUtils.setField(storageService, "roomtypeBucket", "");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> storageService.uploadRoomTypeImage(new byte[]{1}, "room.jpg", "image/jpeg"));

        assertEquals("Supabase storage is not configured properly (missing room type bucket)", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID14 - Throw exception when uploadRoomTypeImage receives 403 Forbidden")
    void should_throwException_when_uploadRoomTypeImageReceives403Forbidden() {
        responseStatusCode = 403;

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> storageService.uploadRoomTypeImage(new byte[]{1}, "room.jpg", "image/jpeg"));

        assertTrue(ex.getMessage().startsWith("Loi khi tai anh loai phong len he thong luu tru:"));
    }

    @Test
    @DisplayName("UTCID15 - Default to .jpg extension when contentType and originalFilename have no extension")
    void should_defaultToJpgExtension_when_contentTypeAndFilenameHaveNoExtension() {
        String imageUrl = storageService.uploadRoomTypeImage(new byte[]{1}, "blobfile", null);

        assertNotNull(imageUrl);
        assertTrue(imageUrl.endsWith(".jpg"));
    }

    @Test
    @DisplayName("UTCID16 - Throw exception when uploadRoomTypeImage with missing supabaseUrl or key")
    void should_throwException_when_uploadRoomTypeImageWithMissingSupabaseUrlOrKey() {
        ReflectionTestUtils.setField(storageService, "supabaseKey", "");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> storageService.uploadRoomTypeImage(new byte[]{1}, "room.jpg", "image/jpeg"));

        assertEquals("Supabase storage is not configured properly (missing URL or Key)", ex.getMessage());
    }

    // ==========================================
    // 4. deleteAvatar Test Cases (UTCID17 - UTCID19)
    // ==========================================

    @Test
    @DisplayName("UTCID17 - Successful deleteAvatar when valid public URL provided")
    void should_deleteAvatarSuccessfully_when_validAvatarUrlProvided() {
        String validUrl = serverUrl + "/storage/v1/object/public/avatars/avatar_123.jpg";

        assertDoesNotThrow(() -> storageService.deleteAvatar(validUrl));
    }

    @Test
    @DisplayName("UTCID18 - Return silently when deleteAvatar with null or blank URL")
    void should_returnSilently_when_deleteAvatarWithNullOrBlankUrl() {
        assertDoesNotThrow(() -> storageService.deleteAvatar("   "));
        assertDoesNotThrow(() -> storageService.deleteAvatar(null));
    }

    @Test
    @DisplayName("UTCID19 - Return silently when deleteAvatar URL does not match public prefix")
    void should_returnSilently_when_deleteAvatarUrlDoesNotMatchPublicPrefix() {
        String unmatchedUrl = "https://external-domain.com/storage/v1/object/public/avatars/avatar_123.jpg";

        assertDoesNotThrow(() -> storageService.deleteAvatar(unmatchedUrl));
    }

    // ==========================================
    // 5. deleteRoomTypeImage Test Cases (UTCID20 - UTCID21)
    // ==========================================

    @Test
    @DisplayName("UTCID20 - Successful deleteRoomTypeImage when valid public URL provided")
    void should_deleteRoomTypeImageSuccessfully_when_validImageUrlProvided() {
        String validUrl = serverUrl + "/storage/v1/object/public/roomtypes/roomtype_123.jpg";

        assertDoesNotThrow(() -> storageService.deleteRoomTypeImage(validUrl));
    }

    @Test
    @DisplayName("UTCID21 - Catch exception silently when deleteRoomTypeImage encounters HTTP error")
    void should_catchExceptionSilently_when_deleteRoomTypeImageEncounterHttpError() {
        responseStatusCode = 500;
        String validUrl = serverUrl + "/storage/v1/object/public/roomtypes/roomtype_123.jpg";

        assertDoesNotThrow(() -> storageService.deleteRoomTypeImage(validUrl));
    }

    // ==========================================
    // 6. getSignedUrl Test Cases (UTCID22 - UTCID24)
    // ==========================================

    @Test
    @DisplayName("UTCID22 - Successful getSignedUrl extracting filename from URL containing query params")
    void should_getSignedUrlSuccessfully_when_urlContainsQueryParams() {
        String urlWithQuery = serverUrl + "/storage/v1/object/ekyc-documents/doc123.jpg?token=xyz";

        String signedUrl = storageService.getSignedUrl(urlWithQuery);

        assertNotNull(signedUrl);
        assertTrue(signedUrl.contains("/storage/v1/object/sign/ekyc-documents/doc.jpg?token=abc"));
    }

    @Test
    @DisplayName("UTCID23 - Successful getSignedUrl extracting filename from public eKYC URL")
    void should_getSignedUrlSuccessfully_when_publicEkycUrlProvided() {
        String publicUrl = serverUrl + "/storage/v1/object/public/ekyc-documents/card_front.png";

        String signedUrl = storageService.getSignedUrl(publicUrl);

        assertNotNull(signedUrl);
    }

    @Test
    @DisplayName("UTCID24 - Return null when getSignedUrl called with null or blank path")
    void should_returnNull_when_getSignedUrlWithNullOrBlankPath() {
        assertNull(storageService.getSignedUrl(null));
        assertNull(storageService.getSignedUrl("   "));
    }
}
