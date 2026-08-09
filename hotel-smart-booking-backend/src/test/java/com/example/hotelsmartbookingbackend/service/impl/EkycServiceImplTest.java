package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.response.AiEkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceEnrollmentResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceFrameValidationResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;
import com.example.hotelsmartbookingbackend.entity.EkycProfile;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.EkycProfileRepository;
import com.example.hotelsmartbookingbackend.repository.FaceEmbeddingAngleRepository;
import com.example.hotelsmartbookingbackend.repository.FaceEmbeddingRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.NotificationService;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EkycServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private EkycProfileRepository ekycProfileRepository;
    @Mock
    private FaceEmbeddingRepository faceEmbeddingRepository;
    @Mock
    private FaceEmbeddingAngleRepository faceEmbeddingAngleRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private WebClient webClient;
    @Mock
    private AesEncryptionService aesEncryptionService;
    @Mock
    private SupabaseStorageService supabaseStorageService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EkycServiceImpl ekycService;

    private User activeUser;
    private MockMultipartFile frontFile;
    private MockMultipartFile backFile;
    private MockMultipartFile selfieFile;
    private MockMultipartFile leftFile;
    private MockMultipartFile rightFile;
    private MockMultipartFile upFile;
    private MockMultipartFile downFile;

    @BeforeEach
    void setUp() {
        activeUser = new User();
        activeUser.setId(1);
        activeUser.setEmail("john@example.com");
        activeUser.setFullName("John Doe");
        activeUser.setIdCardNumber("123456789012");
        activeUser.setStatus("Active");
        activeUser.setRole(Role.customer);

        frontFile = new MockMultipartFile("frontImage", "front.jpg", "image/jpeg", new byte[]{1, 2, 3});
        backFile = new MockMultipartFile("backImage", "back.jpg", "image/jpeg", new byte[]{4, 5, 6});
        selfieFile = new MockMultipartFile("selfieImage", "selfie.jpg", "image/jpeg", new byte[]{7, 8, 9});
        leftFile = new MockMultipartFile("leftImage", "left.jpg", "image/jpeg", new byte[]{10});
        rightFile = new MockMultipartFile("rightImage", "right.jpg", "image/jpeg", new byte[]{11});
        upFile = new MockMultipartFile("upImage", "up.jpg", "image/jpeg", new byte[]{12});
        downFile = new MockMultipartFile("downImage", "down.jpg", "image/jpeg", new byte[]{13});
    }

    private List<Double> create512Embedding() {
        return IntStream.range(0, 512).mapToDouble(i -> 0.12345678).boxed().toList();
    }

    // ==========================================
    // 1. processEkyc Test Cases (UTCID01 - UTCID14, UTCID23 - UTCID30, UTCID38)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful processEkyc for new profile")
    void should_processEkycSuccessfully_when_validRequestAndVerifiedAiResponses() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        when(ekycProfileRepository.save(any(EkycProfile.class))).thenAnswer(invocation -> {
            EkycProfile p = invocation.getArgument(0);
            p.setId(10);
            return p;
        });

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setFullName("John Doe");
        ocrResponse.setDateOfBirth("1990-01-01");
        ocrResponse.setGender("Nam");
        ocrResponse.setHometown("Ha Noi");
        ocrResponse.setValidationPassed(true);

        AiFaceEnrollmentResponse faceResponse = new AiFaceEnrollmentResponse();
        faceResponse.setEnrolled(true);
        faceResponse.setLivenessPassed(true);
        faceResponse.setActiveLivenessPassed(true);
        faceResponse.setEmbedding(create512Embedding());

        AiFaceEnrollmentResponse.FaceAngleEmbedding angle = new AiFaceEnrollmentResponse.FaceAngleEmbedding();
        angle.setPose("center");
        angle.setEmbedding(create512Embedding());

        faceResponse.setAngleEmbeddings(List.of(angle));

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.just(faceResponse));

        when(aesEncryptionService.hmac("123456789012")).thenReturn("hmac_hash_123456");
        when(ekycProfileRepository.existsVerifiedByIdcardnumberhash("hmac_hash_123456")).thenReturn(false);
        when(aesEncryptionService.encrypt(anyString())).thenReturn("encrypted_value");
        when(faceEmbeddingRepository.findEmbeddingIdByUserId(1)).thenReturn(Optional.of(100));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        EkycResponse response = ekycService.processEkyc(
                "john@example.com", frontFile, backFile, selfieFile,
                leftFile, rightFile, upFile, downFile
        );

        assertNotNull(response);
        assertEquals("VERIFIED", response.getStatus());
        assertEquals("123456789012", response.getRecognizedIdNumber());
        verify(ekycProfileRepository).markAsVerified(eq(10), any(), any(), eq("Auto"));
        verify(faceEmbeddingRepository).upsertEmbedding(eq(1), anyString(), any());
        verify(faceEmbeddingAngleRepository).upsertAngle(eq(100), eq("center"), anyString(), any(), any(), any(), any(), anyBoolean(), any(), any());
        verify(valueOperations).set(eq("user:ekyc:1"), eq("VERIFIED"), any());
        verify(notificationService).sendNotification(eq(activeUser), anyString(), anyString(), eq("Ekyc"), eq(10));
    }

    @Test
    @DisplayName("UTCID02 - Throw exception when front image file is empty or null")
    void should_throwException_when_uploadFrontImageIsEmptyOrNull() {
        MockMultipartFile emptyFront = new MockMultipartFile("frontImage", "", "image/jpeg", new byte[0]);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ekycService.processEkyc("john@example.com", emptyFront, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("ekyc_front"));
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("UTCID03 - Throw exception when user email is not found")
    void should_throwException_when_userEmailNotFound() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("unknown@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertEquals("Không tìm thấy user với email: unknown@example.com", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID04 - Throw exception when user account status is Inactive or Banned")
    void should_throwException_when_userAccountStatusIsInactiveOrBanned() {
        activeUser.setStatus("Banned");
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertEquals("Tài khoản của bạn đã bị khóa hoặc không hoạt động.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID05 - Throw exception when user already has an EkycProfile with VERIFIED status")
    void should_throwException_when_userAlreadyHasVerifiedEkycProfile() {
        EkycProfile verifiedProfile = new EkycProfile();
        verifiedProfile.setStatus("VERIFIED");

        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.of(verifiedProfile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertEquals("Tài khoản của bạn đã được xác minh eKYC trước đó.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID06 - Throw exception when OCR AI microservice call fails")
    void should_throwException_when_ocrAiServiceCallFails() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenThrow(new RuntimeException("OCR service offline"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Không thể đọc thông tin CCCD"));
    }

    @Test
    @DisplayName("UTCID07 - Throw exception when OCR AI returns validationPassed = false")
    void should_throwException_when_ocrAiValidationPassedIsFalse() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(false);
        ocrResponse.setValidationErrors(List.of("Mặt trước bị mờ", "Mặt sau không hợp lệ"));

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Thông tin CCCD không nhất quán"));
    }

    @Test
    @DisplayName("UTCID08 - Throw exception when OCR fails to extract ID card number")
    void should_throwException_when_ocrFailsToExtractIdCardNumber() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber(null);
        ocrResponse.setValidationPassed(true);

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Không nhận diện được số CCCD trên ảnh mặt trước"));
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when OCR extracted ID card number does not match registered user ID")
    void should_throwException_when_ocrExtractedIdCardNumberDoesNotMatchRegisteredUser() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("987654321098");
        ocrResponse.setValidationPassed(true);

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertEquals("So CCCD tren anh khong khop voi so CCCD da dang ky tai khoan.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID10 - Throw exception when Face Enrollment AI liveness fails")
    void should_throwException_when_faceEnrollmentAiLivenessFails() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        AiFaceEnrollmentResponse faceResponse = new AiFaceEnrollmentResponse();
        faceResponse.setEnrolled(true);
        faceResponse.setLivenessPassed(false);

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.just(faceResponse));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Đăng ký khuôn mặt thất bại"));
    }

    @Test
    @DisplayName("UTCID11 - Throw exception when Face Enrollment embedding dimension is not 512")
    void should_throwException_when_faceEnrollmentEmbeddingDimensionIsNot512() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        AiFaceEnrollmentResponse faceResponse = new AiFaceEnrollmentResponse();
        faceResponse.setEnrolled(true);
        faceResponse.setLivenessPassed(true);
        faceResponse.setActiveLivenessPassed(true);
        faceResponse.setEmbedding(List.of(0.1, 0.2));

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.just(faceResponse));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Đăng ký khuôn mặt thất bại"));
    }

    @Test
    @DisplayName("UTCID12 - Throw exception when duplicate ID card HMAC hash exists for another user")
    void should_throwException_when_duplicateIdCardHmacHashExistsForAnotherUser() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        AiFaceEnrollmentResponse faceResponse = new AiFaceEnrollmentResponse();
        faceResponse.setEnrolled(true);
        faceResponse.setLivenessPassed(true);
        faceResponse.setActiveLivenessPassed(true);
        faceResponse.setEmbedding(create512Embedding());

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.just(faceResponse));
        when(aesEncryptionService.hmac("123456789012")).thenReturn("hmac_hash_123456");
        when(ekycProfileRepository.existsVerifiedByIdcardnumberhash("hmac_hash_123456")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertEquals("Số CCCD này đã được liên kết với một tài khoản khác.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID13 - Throw exception when face angle embedding pose is invalid")
    void should_throwException_when_faceAngleEmbeddingPoseIsInvalid() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        AiFaceEnrollmentResponse faceResponse = new AiFaceEnrollmentResponse();
        faceResponse.setEnrolled(true);
        faceResponse.setLivenessPassed(true);
        faceResponse.setActiveLivenessPassed(true);
        faceResponse.setEmbedding(create512Embedding());

        AiFaceEnrollmentResponse.FaceAngleEmbedding angle = new AiFaceEnrollmentResponse.FaceAngleEmbedding();
        angle.setPose("diagonal");
        angle.setEmbedding(create512Embedding());

        faceResponse.setAngleEmbeddings(List.of(angle));

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.just(faceResponse));
        when(aesEncryptionService.hmac("123456789012")).thenReturn("hmac_hash_123456");
        when(ekycProfileRepository.existsVerifiedByIdcardnumberhash("hmac_hash_123456")).thenReturn(false);
        when(faceEmbeddingRepository.findEmbeddingIdByUserId(1)).thenReturn(Optional.of(100));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Face API tra ve pose khong hop le"));
    }

    @Test
    @DisplayName("UTCID14 - Successful processEkyc for update flow of previously rejected profile")
    void should_processEkycSuccessfully_when_updateFlowForPreviouslyRejectedProfile() {
        EkycProfile existingProfile = new EkycProfile();
        existingProfile.setId(20);
        existingProfile.setUser(activeUser);
        existingProfile.setStatus("REJECTED");

        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.of(existingProfile));

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        AiFaceEnrollmentResponse faceResponse = new AiFaceEnrollmentResponse();
        faceResponse.setEnrolled(true);
        faceResponse.setLivenessPassed(true);
        faceResponse.setActiveLivenessPassed(true);
        faceResponse.setEmbedding(create512Embedding());
        AiFaceEnrollmentResponse.FaceAngleEmbedding angle = new AiFaceEnrollmentResponse.FaceAngleEmbedding();
        angle.setPose("center");
        angle.setEmbedding(create512Embedding());
        faceResponse.setAngleEmbeddings(List.of(angle));

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.just(faceResponse));
        when(aesEncryptionService.hmac("123456789012")).thenReturn("hmac_hash_123456");
        when(ekycProfileRepository.existsVerifiedByIdcardnumberhashAndUseridNot("hmac_hash_123456", activeUser)).thenReturn(false);
        when(faceEmbeddingRepository.findEmbeddingIdByUserId(1)).thenReturn(Optional.of(100));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        EkycResponse response = ekycService.processEkyc(
                "john@example.com", frontFile, backFile, selfieFile,
                leftFile, rightFile, upFile, downFile
        );

        assertNotNull(response);
        assertEquals("VERIFIED", response.getStatus());
        verify(ekycProfileRepository).markAsVerified(eq(20), any(), any(), eq("Auto"));
    }

    @Test
    @DisplayName("UTCID23 - Throw exception when OCR AI service returns null response")
    void should_throwException_when_ocrAiServiceReturnsNullResponse() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Không thể đọc thông tin CCCD"));
    }

    @Test
    @DisplayName("UTCID24 - Throw exception when OCR AI service throws WebClientResponseException")
    void should_throwException_when_ocrAiServiceThrowsWebClientResponseException() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);

        WebClientResponseException webEx = WebClientResponseException.create(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", HttpHeaders.EMPTY, "OCR Engine Down".getBytes(), null);
        when(headersSpec.retrieve()).thenThrow(webEx);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Không thể đọc thông tin CCCD"));
    }

    @Test
    @DisplayName("UTCID25 - Throw exception when Face Enrollment AI service returns null response")
    void should_throwException_when_faceEnrollmentAiReturnsNullResponse() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Đăng ký khuôn mặt thất bại"));
    }

    @Test
    @DisplayName("UTCID26 - Throw exception when Face Enrollment AI service throws WebClientResponseException with detail")
    void should_throwException_when_faceEnrollmentAiThrowsWebClientResponseExceptionWithDetail() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        when(headersSpec.retrieve()).thenReturn(responseSpec).thenThrow(WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", HttpHeaders.EMPTY,
                "{\"detail\":\"Nhiều khuôn mặt xuất hiện trong ảnh\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        ));

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertEquals("Nhiều khuôn mặt xuất hiện trong ảnh", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID27 - Throw exception when Face Enrollment AI service connection fails")
    void should_throwException_when_faceEnrollmentAiConnectionFails() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        when(headersSpec.retrieve()).thenReturn(responseSpec).thenThrow(new RuntimeException("Connection refused"));

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Connection refused"));
    }

    @Test
    @DisplayName("UTCID28 - Safely process eKYC when notificationService throws exception")
    void should_processEkycSafely_when_notificationServiceThrowsException() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        AiFaceEnrollmentResponse faceResponse = new AiFaceEnrollmentResponse();
        faceResponse.setEnrolled(true);
        faceResponse.setLivenessPassed(true);
        faceResponse.setActiveLivenessPassed(true);
        faceResponse.setEmbedding(create512Embedding());

        AiFaceEnrollmentResponse.FaceAngleEmbedding angle = new AiFaceEnrollmentResponse.FaceAngleEmbedding();
        angle.setPose("center");
        angle.setEmbedding(create512Embedding());
        faceResponse.setAngleEmbeddings(List.of(angle));

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.just(faceResponse));
        when(aesEncryptionService.hmac("123456789012")).thenReturn("hmac_hash_123456");
        when(ekycProfileRepository.existsVerifiedByIdcardnumberhash("hmac_hash_123456")).thenReturn(false);
        when(faceEmbeddingRepository.findEmbeddingIdByUserId(1)).thenReturn(Optional.of(100));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        doThrow(new RuntimeException("Notification server down")).when(notificationService)
                .sendNotification(any(), anyString(), anyString(), anyString(), any());

        EkycResponse response = ekycService.processEkyc(
                "john@example.com", frontFile, backFile, selfieFile,
                leftFile, rightFile, upFile, downFile
        );

        assertNotNull(response);
        assertEquals("VERIFIED", response.getStatus());
    }

    @Test
    @DisplayName("UTCID29 - Skip null element in face angle embedding list during saveAngleEmbeddings")
    void should_skipNullElementInAngleEmbeddingsList_when_savingAngleEmbeddings() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        AiFaceEnrollmentResponse faceResponse = new AiFaceEnrollmentResponse();
        faceResponse.setEnrolled(true);
        faceResponse.setLivenessPassed(true);
        faceResponse.setActiveLivenessPassed(true);
        faceResponse.setEmbedding(create512Embedding());

        List<AiFaceEnrollmentResponse.FaceAngleEmbedding> angles = new ArrayList<>();
        angles.add(null); // Null element to skip

        AiFaceEnrollmentResponse.FaceAngleEmbedding validAngle = new AiFaceEnrollmentResponse.FaceAngleEmbedding();
        validAngle.setPose("left");
        validAngle.setEmbedding(create512Embedding());
        angles.add(validAngle);

        faceResponse.setAngleEmbeddings(angles);

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.just(faceResponse));
        when(aesEncryptionService.hmac("123456789012")).thenReturn("hmac_hash_123456");
        when(ekycProfileRepository.existsVerifiedByIdcardnumberhash("hmac_hash_123456")).thenReturn(false);
        when(faceEmbeddingRepository.findEmbeddingIdByUserId(1)).thenReturn(Optional.of(100));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        EkycResponse response = ekycService.processEkyc(
                "john@example.com", frontFile, backFile, selfieFile,
                leftFile, rightFile, upFile, downFile
        );

        assertNotNull(response);
        verify(faceEmbeddingAngleRepository, times(1)).upsertAngle(eq(100), eq("left"), anyString(), any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("UTCID30 - Throw exception when FaceEmbedding ID is missing during saveAngleEmbeddings")
    void should_throwException_when_faceEmbeddingIdIsMissingForAngleEmbeddings() {
        when(supabaseStorageService.uploadEkycDocument(any(), anyString(), anyString())).thenReturn("http://storage.com/img.jpg");
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycProfile createdProfile = new EkycProfile();
        createdProfile.setId(10);
        when(ekycProfileRepository.save(any())).thenReturn(createdProfile);

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiEkycResponse ocrResponse = new AiEkycResponse();
        ocrResponse.setIdCardNumber("123456789012");
        ocrResponse.setValidationPassed(true);

        AiFaceEnrollmentResponse faceResponse = new AiFaceEnrollmentResponse();
        faceResponse.setEnrolled(true);
        faceResponse.setLivenessPassed(true);
        faceResponse.setActiveLivenessPassed(true);
        faceResponse.setEmbedding(create512Embedding());

        AiFaceEnrollmentResponse.FaceAngleEmbedding angle = new AiFaceEnrollmentResponse.FaceAngleEmbedding();
        angle.setPose("center");
        angle.setEmbedding(create512Embedding());
        faceResponse.setAngleEmbeddings(List.of(angle));

        when(responseSpec.bodyToMono(AiEkycResponse.class)).thenReturn(Mono.just(ocrResponse));
        when(responseSpec.bodyToMono(AiFaceEnrollmentResponse.class)).thenReturn(Mono.just(faceResponse));
        when(aesEncryptionService.hmac("123456789012")).thenReturn("hmac_hash_123456");
        when(ekycProfileRepository.existsVerifiedByIdcardnumberhash("hmac_hash_123456")).thenReturn(false);
        when(faceEmbeddingRepository.findEmbeddingIdByUserId(1)).thenReturn(Optional.empty()); // Missing ID!

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", frontFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertEquals("Khong tim thay FaceEmbedding vua luu", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID38 - Throw exception when file read fails with IOException during uploadImage")
    void should_throwException_when_fileReadFailsWithIOExceptionDuringUploadImage() throws Exception {
        MultipartFile badFile = mock(MultipartFile.class);
        when(badFile.isEmpty()).thenReturn(false);
        when(badFile.getContentType()).thenReturn("image/jpeg");
        when(badFile.getOriginalFilename()).thenReturn("front.jpg");
        when(badFile.getBytes()).thenThrow(new IOException("Disk read error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("john@example.com", badFile, backFile, selfieFile, leftFile, rightFile, upFile, downFile));

        assertTrue(ex.getMessage().contains("Lỗi khi đọc file upload"));
    }

    // ==========================================
    // 2. getEkycStatus Test Cases (UTCID15 - UTCID17, UTCID31 - UTCID34)
    // ==========================================

    @Test
    @DisplayName("UTCID15 - Successful getEkycStatus for verified customer profile")
    void should_getEkycStatusSuccessfully_when_verifiedProfileExists() {
        EkycProfile profile = new EkycProfile();
        profile.setId(10);
        profile.setStatus("VERIFIED");
        profile.setIdCardNumber("enc_123456789012");
        profile.setFullName("enc_John Doe");
        profile.setFrontImage("path/front.jpg");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.of(profile));
        when(aesEncryptionService.decrypt("enc_123456789012")).thenReturn("123456789012");
        when(aesEncryptionService.decrypt("enc_John Doe")).thenReturn("John Doe");
        when(supabaseStorageService.getSignedUrl("path/front.jpg")).thenReturn("http://signed.url/front.jpg");

        EkycStatusResponse response = ekycService.getEkycStatus("john@example.com");

        assertNotNull(response);
        assertEquals("VERIFIED", response.getStatus());
        assertEquals("John Doe", response.getFullName());
        assertEquals("12*******012", response.getIdNumber());
    }

    @Test
    @DisplayName("UTCID16 - Successful getEkycStatus when no profile exists in DB")
    void should_getEkycStatusSuccessfully_when_noEkycProfileExists() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.empty());

        EkycStatusResponse response = ekycService.getEkycStatus("john@example.com");

        assertNotNull(response);
        assertEquals("NOT_FOUND", response.getStatus());
        assertEquals("John Doe", response.getFullName());
    }

    @Test
    @DisplayName("UTCID17 - Throw exception when getting eKYC status for non-existent user email")
    void should_throwException_when_getEkycStatusWithUserEmailNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.getEkycStatus("unknown@example.com"));

        assertEquals("Không tìm thấy user với email: unknown@example.com", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID31 - Return AI_CHECKING status message when profile is in AI_CHECKING state")
    void should_returnAiCheckingMessage_when_profileIsInAiCheckingState() {
        EkycProfile profile = new EkycProfile();
        profile.setStatus("AI_CHECKING");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.of(profile));

        EkycStatusResponse response = ekycService.getEkycStatus("john@example.com");

        assertNotNull(response);
        assertEquals("AI_CHECKING", response.getStatus());
        assertEquals("AI dang trich xuat du lieu.", response.getMessage());
    }

    @Test
    @DisplayName("UTCID32 - Return normalized AI_CHECKING status when profile status is PENDING")
    void should_returnNormalizedAiCheckingStatus_when_profileStatusIsPending() {
        EkycProfile profile = new EkycProfile();
        profile.setStatus("PENDING");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.of(profile));

        EkycStatusResponse response = ekycService.getEkycStatus("john@example.com");

        assertNotNull(response);
        assertEquals("AI_CHECKING", response.getStatus());
    }

    @Test
    @DisplayName("UTCID33 - Return normalized SUBMITTED status when profile status is REJECTED")
    void should_returnNormalizedSubmittedStatus_when_profileStatusIsRejected() {
        EkycProfile profile = new EkycProfile();
        profile.setStatus("REJECTED");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.of(profile));

        EkycStatusResponse response = ekycService.getEkycStatus("john@example.com");

        assertNotNull(response);
        assertEquals("SUBMITTED", response.getStatus());
    }

    @Test
    @DisplayName("UTCID34 - Gracefully fallback to user defaults when AES decryption throws exception")
    void should_fallbackToUserDefaults_when_aesDecryptionThrowsException() {
        EkycProfile profile = new EkycProfile();
        profile.setStatus("SUBMITTED");
        profile.setFullName("bad_encrypted_string");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(ekycProfileRepository.findTopByUserOrderByCreatedAtDesc(activeUser)).thenReturn(Optional.of(profile));
        when(aesEncryptionService.decrypt("bad_encrypted_string")).thenThrow(new RuntimeException("Decryption error"));

        EkycStatusResponse response = ekycService.getEkycStatus("john@example.com");

        assertNotNull(response);
        assertEquals("John Doe", response.getFullName()); // Fallback to activeUser.getFullName()
    }

    // ==========================================
    // 3. validateLivenessFrame Test Cases (UTCID18 - UTCID22, UTCID35 - UTCID37)
    // ==========================================

    @Test
    @DisplayName("UTCID18 - Successful validateLivenessFrame for center step")
    void should_validateLivenessFrameSuccessfully_when_validCenterFrameImageAndStep() {
        MockMultipartFile frame = new MockMultipartFile("frameImage", "frame.jpg", "image/jpeg", new byte[]{1, 2});

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiFaceFrameValidationResponse validationResponse = new AiFaceFrameValidationResponse();
        validationResponse.setPassed(true);

        when(responseSpec.bodyToMono(AiFaceFrameValidationResponse.class)).thenReturn(Mono.just(validationResponse));

        AiFaceFrameValidationResponse result = ekycService.validateLivenessFrame(frame, null, null, "center");

        assertNotNull(result);
        assertTrue(result.getPassed());
    }

    @Test
    @DisplayName("UTCID19 - Throw exception when frameImage is null or empty")
    void should_throwException_when_validateLivenessFrameWithNullOrEmptyFrameImage() {
        MockMultipartFile emptyFrame = new MockMultipartFile("frameImage", "", "image/jpeg", new byte[0]);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.validateLivenessFrame(emptyFrame, null, null, "center"));

        assertEquals("Frame khuôn mặt không có dữ liệu.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID20 - Throw exception when pose step label is invalid")
    void should_throwException_when_validateLivenessFrameWithInvalidStepLabel() {
        MockMultipartFile frame = new MockMultipartFile("frameImage", "frame.jpg", "image/jpeg", new byte[]{1, 2});

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.validateLivenessFrame(frame, null, null, "diagonal"));

        assertEquals("Bước liveness không hợp lệ.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID21 - Throw exception when non-center step is missing reference image")
    void should_throwException_when_validateLivenessFrameWithNonCenterStepMissingReferenceImage() {
        MockMultipartFile frame = new MockMultipartFile("frameImage", "frame.jpg", "image/jpeg", new byte[]{1, 2});

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.validateLivenessFrame(frame, null, null, "left"));

        assertEquals("Thiếu ảnh chính diện tham chiếu.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID22 - Handle WebClientResponseException in validateLivenessFrame")
    void should_handleWebClientResponseException_when_validatingLivenessFrame() {
        MockMultipartFile frame = new MockMultipartFile("frameImage", "frame.jpg", "image/jpeg", new byte[]{1, 2});

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);

        WebClientResponseException webEx = WebClientResponseException.create(
                HttpStatus.BAD_REQUEST.value(), "Bad Request", HttpHeaders.EMPTY,
                "{\"detail\":\"Khuôn mặt quay quá xa\"}".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        );
        when(headersSpec.retrieve()).thenThrow(webEx);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.validateLivenessFrame(frame, null, null, "center"));

        assertEquals("Khuôn mặt quay quá xa", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID35 - Throw exception when Face validation AI returns null response")
    void should_throwException_when_faceValidationAiReturnsNullResponse() {
        MockMultipartFile frame = new MockMultipartFile("frameImage", "frame.jpg", "image/jpeg", new byte[]{1, 2});

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToMono(AiFaceFrameValidationResponse.class)).thenReturn(Mono.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.validateLivenessFrame(frame, null, null, "center"));

        assertEquals("Face validation API trả về phản hồi rỗng.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID36 - Throw exception when connection to Face validation service fails")
    void should_throwException_when_connectionToFaceValidationServiceFails() {
        MockMultipartFile frame = new MockMultipartFile("frameImage", "frame.jpg", "image/jpeg", new byte[]{1, 2});

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);

        when(headersSpec.retrieve()).thenThrow(new RuntimeException("Network timeout"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.validateLivenessFrame(frame, null, null, "center"));

        assertEquals("Network timeout", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID37 - Successful validateLivenessFrame for non-center step with reference and opposite images")
    void should_validateLivenessFrameSuccessfully_when_nonCenterStepWithReferenceAndOppositeImages() {
        MockMultipartFile frame = new MockMultipartFile("frameImage", "frame.jpg", "image/jpeg", new byte[]{1, 2});
        MockMultipartFile ref = new MockMultipartFile("referenceImage", "ref.jpg", "image/jpeg", new byte[]{3, 4});
        MockMultipartFile opp = new MockMultipartFile("oppositeImage", "opp.jpg", "image/jpeg", new byte[]{5, 6});

        WebClient.RequestBodyUriSpec bodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec bodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(nullable(String.class))).thenReturn(bodySpec);
        when(bodySpec.contentType(any())).thenReturn(bodySpec);
        when(bodySpec.body(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        AiFaceFrameValidationResponse validationResponse = new AiFaceFrameValidationResponse();
        validationResponse.setPassed(true);

        when(responseSpec.bodyToMono(AiFaceFrameValidationResponse.class)).thenReturn(Mono.just(validationResponse));

        AiFaceFrameValidationResponse result = ekycService.validateLivenessFrame(frame, ref, opp, "left");

        assertNotNull(result);
        assertTrue(result.getPassed());
    }

    @Test
    @DisplayName("Throw exception when uploadImage encounters IOException")
    void should_throwException_when_uploadImageEncountersIOException() throws IOException {
        MockMultipartFile corruptedFile = mock(MockMultipartFile.class);
        when(corruptedFile.isEmpty()).thenReturn(false);
        when(corruptedFile.getContentType()).thenReturn("image/jpeg");
        when(corruptedFile.getOriginalFilename()).thenReturn("front.jpg");
        when(corruptedFile.getBytes()).thenThrow(new IOException("Disk IO error"));

        MockMultipartFile validFile = new MockMultipartFile("front", "f.jpg", "image/jpeg", new byte[]{1});

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> ekycService.processEkyc("user@example.com", corruptedFile, validFile, validFile, null, null, null, null));

        assertTrue(ex.getMessage().contains("Lỗi khi đọc file upload: Disk IO error"));
    }
}

