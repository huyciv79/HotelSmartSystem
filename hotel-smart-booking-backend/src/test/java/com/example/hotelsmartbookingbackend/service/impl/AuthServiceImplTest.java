package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.config.Auth.JwtUtil;
import com.example.hotelsmartbookingbackend.dto.request.ChangePasswordRequest;
import com.example.hotelsmartbookingbackend.dto.request.ForgotPasswordRequest;
import com.example.hotelsmartbookingbackend.dto.request.LoginRequest;
import com.example.hotelsmartbookingbackend.dto.request.RegisterRequest;
import com.example.hotelsmartbookingbackend.dto.request.ResetPasswordRequest;
import com.example.hotelsmartbookingbackend.dto.request.VerifyForgotOtpRequest;
import com.example.hotelsmartbookingbackend.dto.request.VerifyOtpRequest;
import com.example.hotelsmartbookingbackend.dto.response.LoginResponse;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContextPrincipal(Object principal) {
        if (principal == null) {
            SecurityContextHolder.clearContext();
            return;
        }
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    // ==========================================
    // 1. registerInit Test Cases (UTCID01 - UTCID03)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful registration init when email and CCCD do not exist")
    void should_registerInitSuccessfully_when_emailAndIdCardDoNotExist() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("john@example.com");
        request.setPassword("Pass123!");
        request.setFullName("John Doe");
        request.setPhone("0901234567");
        request.setIdCardNumber("123456789012");

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userRepository.existsByIdCardNumber("123456789012")).thenReturn(false);

        // Act
        authService.registerInit(request);

        // Assert
        verify(valueOperations).set(eq("register:otp:john@example.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(valueOperations).set(eq("register:user:john@example.com"), eq(request), eq(Duration.ofMinutes(5)));
        verify(emailService).sendOtpEmail(eq("john@example.com"), anyString());
    }

    @Test
    @DisplayName("UTCID02 - Throw exception when email already exists during registration init")
    void should_throwException_when_registerInitWithExistingEmail() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setIdCardNumber("123456789012");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.registerInit(request));
        assertEquals("Email đã tồn tại trong hệ thống", exception.getMessage());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("UTCID03 - Throw exception when CCCD already exists during registration init")
    void should_throwException_when_registerInitWithExistingIdCard() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setIdCardNumber("123456789012");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber(any())).thenReturn(false);
        when(userRepository.existsByIdCardNumber("123456789012")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.registerInit(request));
        assertEquals("Số CCCD này đã tồn tại trong hệ thống", exception.getMessage());
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Throw exception when phone number already exists during registration init")
    void should_throwException_when_registerInitWithExistingPhoneNumber() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPhone("0901234567");
        request.setIdCardNumber("123456789012");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.registerInit(request));
        assertEquals("Số điện thoại này đã tồn tại trong hệ thống", exception.getMessage());
        verifyNoInteractions(emailService);
    }

    // ==========================================
    // 2. verifyRegistrationOtp Test Cases (UTCID04 - UTCID12)
    // ==========================================

    @Test
    @DisplayName("UTCID04 - Successful registration verification with typed RegisterRequest in Redis")
    void should_verifyRegistrationOtpSuccessfully_when_validOtpAndTypedRegisterRequestInRedis() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        RegisterRequest cachedRequest = new RegisterRequest();
        cachedRequest.setEmail("john@example.com");
        cachedRequest.setPassword("rawPass123");
        cachedRequest.setFullName("John Doe");
        cachedRequest.setPhone("0901234567");
        cachedRequest.setIdCardNumber("123456789012");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(cachedRequest);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userRepository.existsByIdCardNumber("123456789012")).thenReturn(false);
        when(passwordEncoder.encode("rawPass123")).thenReturn("encodedPass123");

        // Act
        authService.verifyRegistrationOtp(request);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("john@example.com", savedUser.getEmail());
        assertEquals("encodedPass123", savedUser.getPasswordHash());
        assertEquals("John Doe", savedUser.getFullName());
        assertEquals("0901234567", savedUser.getPhoneNumber());
        assertEquals("123456789012", savedUser.getIdCardNumber());
        assertEquals(Role.customer, savedUser.getRole());
        assertEquals("Active", savedUser.getStatus());
        assertNotNull(savedUser.getCreatedAt());

        verify(redisTemplate).delete("register:otp:john@example.com");
        verify(redisTemplate).delete("register:user:john@example.com");
    }

    @Test
    @DisplayName("UTCID05 - Successful registration verification with LinkedHashMap in Redis")
    void should_verifyRegistrationOtpSuccessfully_when_validOtpAndLinkedHashMapInRedis() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        Map<String, Object> cachedMap = new HashMap<>();
        cachedMap.put("email", "john@example.com");
        cachedMap.put("password", "rawPass123");
        cachedMap.put("fullName", "John Doe");
        cachedMap.put("phone", "0901234567");
        cachedMap.put("idCardNumber", "123456789012");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(cachedMap);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userRepository.existsByIdCardNumber("123456789012")).thenReturn(false);
        when(passwordEncoder.encode("rawPass123")).thenReturn("encodedPass123");

        // Act
        authService.verifyRegistrationOtp(request);

        // Assert
        verify(userRepository).save(any(User.class));
        verify(redisTemplate).delete("register:otp:john@example.com");
        verify(redisTemplate).delete("register:user:john@example.com");
    }

    @Test
    @DisplayName("UTCID06 - Throw exception when OTP is invalid or expired")
    void should_throwException_when_verifyRegistrationOtpWithInvalidOrExpiredOtp() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("999999");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyRegistrationOtp(request));
        assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID07 - Throw exception when cached registration info is missing in Redis")
    void should_throwException_when_verifyRegistrationOtpWithNullCachedRequest() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyRegistrationOtp(request));
        assertEquals("Không tìm thấy thông tin đăng ký, vui lòng đăng ký lại", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID08 - Throw exception when CCCD in cached request is null")
    void should_throwException_when_verifyRegistrationOtpWithNullIdCardNumber() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        RegisterRequest cachedRequest = new RegisterRequest();
        cachedRequest.setEmail("john@example.com");
        cachedRequest.setIdCardNumber(null);

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(cachedRequest);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyRegistrationOtp(request));
        assertEquals("Số CCCD phải gồm đúng 12 chữ số", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID09 - Throw exception when CCCD length is 11 digits")
    void should_throwException_when_verifyRegistrationOtpWith11DigitIdCardNumber() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        RegisterRequest cachedRequest = new RegisterRequest();
        cachedRequest.setEmail("john@example.com");
        cachedRequest.setIdCardNumber("12345678901");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(cachedRequest);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyRegistrationOtp(request));
        assertEquals("Số CCCD phải gồm đúng 12 chữ số", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID10 - Throw exception when CCCD length is 13 digits")
    void should_throwException_when_verifyRegistrationOtpWith13DigitIdCardNumber() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        RegisterRequest cachedRequest = new RegisterRequest();
        cachedRequest.setEmail("john@example.com");
        cachedRequest.setIdCardNumber("1234567890123");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(cachedRequest);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyRegistrationOtp(request));
        assertEquals("Số CCCD phải gồm đúng 12 chữ số", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID11 - Throw exception when CCCD contains non-numeric characters")
    void should_throwException_when_verifyRegistrationOtpWithNonNumericIdCardNumber() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        RegisterRequest cachedRequest = new RegisterRequest();
        cachedRequest.setEmail("john@example.com");
        cachedRequest.setIdCardNumber("12345678901A");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(cachedRequest);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyRegistrationOtp(request));
        assertEquals("Số CCCD phải gồm đúng 12 chữ số", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID12 - Throw exception when CCCD already exists in DB at verification time")
    void should_throwException_when_verifyRegistrationOtpWithExistingIdCardNumberInDb() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        RegisterRequest cachedRequest = new RegisterRequest();
        cachedRequest.setEmail("john@example.com");
        cachedRequest.setIdCardNumber("123456789012");
        cachedRequest.setPhone("0901234567");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(cachedRequest);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(false);
        when(userRepository.existsByIdCardNumber("123456789012")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyRegistrationOtp(request));
        assertEquals("Số CCCD này đã tồn tại trong hệ thống", exception.getMessage());
    }

    @Test
    @DisplayName("Throw exception when phone number already exists in DB at verification time")
    void should_throwException_when_verifyRegistrationOtpWithExistingPhoneNumberInDb() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        RegisterRequest cachedRequest = new RegisterRequest();
        cachedRequest.setEmail("john@example.com");
        cachedRequest.setIdCardNumber("123456789012");
        cachedRequest.setPhone("0901234567");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(cachedRequest);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("0901234567")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyRegistrationOtp(request));
        assertEquals("Số điện thoại này đã tồn tại trong hệ thống", exception.getMessage());
    }

    @Test
    @DisplayName("Throw exception when email already exists in DB at verification time")
    void should_throwException_when_verifyRegistrationOtpWithExistingEmailInDb() {
        // Arrange
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("123456");

        RegisterRequest cachedRequest = new RegisterRequest();
        cachedRequest.setEmail("john@example.com");
        cachedRequest.setIdCardNumber("123456789012");
        cachedRequest.setPhone("0901234567");

        when(valueOperations.get("register:otp:john@example.com")).thenReturn("123456");
        when(valueOperations.get("register:user:john@example.com")).thenReturn(cachedRequest);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyRegistrationOtp(request));
        assertEquals("Email đã tồn tại trong hệ thống", exception.getMessage());
    }

    // ==========================================
    // 3. login Test Cases (UTCID13 - UTCID17)
    // ==========================================

    @Test
    @DisplayName("UTCID13 - Successful login for active user with valid credentials")
    void should_loginSuccessfully_when_validCredentialsAndActiveUser() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Pass123!");

        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("hashedPass");
        user.setFullName("John Doe");
        user.setRole(Role.customer);
        user.setStatus("Active");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass123!", "hashedPass")).thenReturn(true);
        when(jwtUtil.generateAccessToken(user)).thenReturn("mock.jwt.token");

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getAccessToken());
        assertNull(response.getRefreshToken());
        assertEquals("user@example.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());
        assertEquals("customer", response.getRole());
    }

    @Test
    @DisplayName("UTCID14 - Throw exception when user email is not found during login")
    void should_throwException_when_loginWithNonExistentEmail() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("Pass123!");

        when(userRepository.findByEmailIgnoreCase("unknown@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(request));
        assertEquals("Email hoặc mật khẩu không đúng", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID15 - Throw exception when password is incorrect during login")
    void should_throwException_when_loginWithIncorrectPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("WrongPass");

        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("hashedPass");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "hashedPass")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(request));
        assertEquals("Email hoặc mật khẩu không đúng", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID16 - Throw exception when user account status is Inactive")
    void should_throwException_when_loginWithInactiveUserStatus() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Pass123!");

        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("hashedPass");
        user.setStatus("Inactive");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass123!", "hashedPass")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(request));
        assertEquals("Tài khoản đã bị vô hiệu hóa", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID17 - Throw exception when user account status is Banned")
    void should_throwException_when_loginWithBannedUserStatus() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Pass123!");

        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash("hashedPass");
        user.setStatus("Banned");

        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Pass123!", "hashedPass")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.login(request));
        assertEquals("Tài khoản đã bị vô hiệu hóa", exception.getMessage());
    }

    // ==========================================
    // 4. forgotPassword Test Cases (UTCID18 - UTCID19)
    // ==========================================

    @Test
    @DisplayName("UTCID18 - Successful forgot password request when email exists")
    void should_forgotPasswordSuccessfully_when_emailExists() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("john@example.com");

        when(userRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(true);

        // Act
        authService.forgotPassword(request);

        // Assert
        verify(valueOperations).set(eq("FORGOT_PASSWORD_OTP:john@example.com"), anyString(), eq(Duration.ofMinutes(5)));
        verify(emailService).sendForgotPasswordOtpEmail(eq("john@example.com"), anyString());
    }

    @Test
    @DisplayName("UTCID19 - Throw exception when email does not exist during forgot password")
    void should_throwException_when_forgotPasswordWithNonExistentEmail() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("notfound@example.com");

        when(userRepository.existsByEmailIgnoreCase("notfound@example.com")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.forgotPassword(request));
        assertEquals("Email không tồn tại trong hệ thống", exception.getMessage());
        verifyNoInteractions(emailService);
    }

    // ==========================================
    // 5. verifyForgotOtp Test Cases (UTCID20 - UTCID22)
    // ==========================================

    @Test
    @DisplayName("UTCID20 - Successful verification of forgot password OTP")
    void should_verifyForgotOtpSuccessfully_when_validOtpInRedis() {
        // Arrange
        VerifyForgotOtpRequest request = new VerifyForgotOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("654321");

        when(valueOperations.get("FORGOT_PASSWORD_OTP:john@example.com")).thenReturn("654321");

        // Act
        String resetToken = authService.verifyForgotOtp(request);

        // Assert
        assertNotNull(resetToken);
        assertFalse(resetToken.isEmpty());
        verify(valueOperations).set(eq("reset:token:" + resetToken), eq("john@example.com"), eq(Duration.ofSeconds(3600000)));
        verify(redisTemplate).delete("FORGOT_PASSWORD_OTP:john@example.com");
    }

    @Test
    @DisplayName("UTCID21 - Throw exception when forgot password OTP is null or expired in Redis")
    void should_throwException_when_verifyForgotOtpWithNullOrExpiredOtp() {
        // Arrange
        VerifyForgotOtpRequest request = new VerifyForgotOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("654321");

        when(valueOperations.get("FORGOT_PASSWORD_OTP:john@example.com")).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyForgotOtp(request));
        assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID22 - Throw exception when forgot password OTP is mismatched")
    void should_throwException_when_verifyForgotOtpWithMismatchedOtp() {
        // Arrange
        VerifyForgotOtpRequest request = new VerifyForgotOtpRequest();
        request.setEmail("john@example.com");
        request.setOtp("000000");

        when(valueOperations.get("FORGOT_PASSWORD_OTP:john@example.com")).thenReturn("654321");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.verifyForgotOtp(request));
        assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn", exception.getMessage());
    }

    // ==========================================
    // 6. resetPassword Test Cases (UTCID23 - UTCID25)
    // ==========================================

    @Test
    @DisplayName("UTCID23 - Successful password reset with valid token and user")
    void should_resetPasswordSuccessfully_when_validResetTokenAndUserExists() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("valid-uuid-token");
        request.setNewPassword("NewSecret123!");

        User user = new User();
        user.setEmail("john@example.com");
        user.setPasswordHash("oldEncodedHash");

        when(valueOperations.get("reset:token:valid-uuid-token")).thenReturn("john@example.com");
        when(userRepository.findByEmailIgnoreCase("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewSecret123!")).thenReturn("newEncodedHash");

        // Act
        authService.resetPassword(request);

        // Assert
        assertEquals("newEncodedHash", user.getPasswordHash());
        assertNotNull(user.getUpdatedAt());
        verify(userRepository).save(user);
        verify(redisTemplate).delete("reset:token:valid-uuid-token");
    }

    @Test
    @DisplayName("UTCID24 - Throw exception when reset token is invalid or expired")
    void should_throwException_when_resetPasswordWithInvalidOrExpiredResetToken() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("bad-token");
        request.setNewPassword("NewSecret123!");

        when(valueOperations.get("reset:token:bad-token")).thenReturn(null);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.resetPassword(request));
        assertEquals("Reset token không hợp lệ hoặc đã hết hạn", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID25 - Throw exception when user email from reset token is not found in DB")
    void should_throwException_when_resetPasswordWithUserNotFound() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setResetToken("valid-token");
        request.setNewPassword("NewSecret123!");

        when(valueOperations.get("reset:token:valid-token")).thenReturn("deleted@example.com");
        when(userRepository.findByEmailIgnoreCase("deleted@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.resetPassword(request));
        assertEquals("Không tìm thấy người dùng", exception.getMessage());
    }

    // ==========================================
    // 7. changePassword Test Cases (UTCID26 - UTCID30)
    // ==========================================

    @Test
    @DisplayName("UTCID26 - Successful password change for authenticated user with valid current password")
    void should_changePasswordSuccessfully_when_authenticatedUserAndCorrectCurrentPassword() {
        // Arrange
        setSecurityContextPrincipal("john@example.com");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        User user = new User();
        user.setEmail("john@example.com");
        user.setPasswordHash("hashedOldPass");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("OldPass123!", "hashedOldPass")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("hashedNewPass");

        // Act
        authService.changePassword(request);

        // Assert
        assertEquals("hashedNewPass", user.getPasswordHash());
        assertNotNull(user.getUpdatedAt());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("UTCID27 - Throw exception when security context principal is null")
    void should_throwException_when_changePasswordWithNullAuthenticationPrincipal() {
        // Arrange
        setSecurityContextPrincipal(null);

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.changePassword(request));
        assertEquals("Bạn cần đăng nhập để thực hiện chức năng này", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID28 - Throw exception when security context principal is anonymousUser")
    void should_throwException_when_changePasswordWithAnonymousUser() {
        // Arrange
        setSecurityContextPrincipal("anonymousUser");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.changePassword(request));
        assertEquals("Bạn cần đăng nhập để thực hiện chức năng này", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID29 - Throw exception when authenticated user is not found in DB")
    void should_throwException_when_changePasswordWithUserNotFoundInDb() {
        // Arrange
        setSecurityContextPrincipal("john@example.com");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.changePassword(request));
        assertEquals("Không tìm thấy người dùng", exception.getMessage());
    }

    @Test
    @DisplayName("UTCID30 - Throw exception when current password is incorrect")
    void should_throwException_when_changePasswordWithIncorrectCurrentPassword() {
        // Arrange
        setSecurityContextPrincipal("john@example.com");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongPass123!");
        request.setNewPassword("NewPass123!");

        User user = new User();
        user.setEmail("john@example.com");
        user.setPasswordHash("hashedOldPass");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass123!", "hashedOldPass")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.changePassword(request));
        assertEquals("Mật khẩu hiện tại không đúng", exception.getMessage());
    }
}
