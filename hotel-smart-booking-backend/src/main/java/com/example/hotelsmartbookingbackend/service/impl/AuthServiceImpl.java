package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.config.JwtUtil;
import com.example.hotelsmartbookingbackend.dto.request.ForgotPasswordRequest;
import com.example.hotelsmartbookingbackend.dto.request.LoginRequest;
import com.example.hotelsmartbookingbackend.dto.request.RefreshTokenRequest;
import com.example.hotelsmartbookingbackend.dto.request.RegisterRequest;
import com.example.hotelsmartbookingbackend.dto.request.VerifyOtpRequest;
import com.example.hotelsmartbookingbackend.dto.request.VerifyForgotOtpRequest;
import com.example.hotelsmartbookingbackend.dto.request.ResetPasswordRequest;
import com.example.hotelsmartbookingbackend.dto.request.ChangePasswordRequest;
import com.example.hotelsmartbookingbackend.dto.response.LoginResponse;
import java.util.UUID;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.enums.Role;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.AuthService;
import com.example.hotelsmartbookingbackend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final String OTP_PREFIX = "register:otp:";
    private static final String FORGOT_PASSWORD_OTP_PREFIX = "FORGOT_PASSWORD_OTP:";
    private static final String RESET_TOKEN_PREFIX = "reset:token:";
    private static final String USER_REQ_PREFIX = "register:user:";
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    private static final long OTP_TTL_MINUTES = 5;

    @Override
    public void registerInit(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Save OTP and Request to Redis
        redisTemplate.opsForValue().set(OTP_PREFIX + request.getEmail(), otp, Duration.ofMinutes(OTP_TTL_MINUTES));
        redisTemplate.opsForValue().set(USER_REQ_PREFIX + request.getEmail(), request, Duration.ofMinutes(OTP_TTL_MINUTES));

        // Send Email
        emailService.sendOtpEmail(request.getEmail(), otp);
    }

    @Override
    public void verifyRegistrationOtp(VerifyOtpRequest request) {
        String cachedOtp = (String) redisTemplate.opsForValue().get(OTP_PREFIX + request.getEmail());

        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        // Retrieve user registration request from Redis
        Object cachedRequestObj = redisTemplate.opsForValue().get(USER_REQ_PREFIX + request.getEmail());
        if (cachedRequestObj == null) {
            throw new RuntimeException("Không tìm thấy thông tin đăng ký, vui lòng đăng ký lại");
        }

        // Depending on Jackson serializer, it might be deserialized as a Map or RegisterRequest
        RegisterRequest registerRequest;
        if (cachedRequestObj instanceof RegisterRequest) {
            registerRequest = (RegisterRequest) cachedRequestObj;
        } else {
            // Fallback (if generic Jackson serialization converts it to LinkedHashMap)
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            registerRequest = mapper.convertValue(cachedRequestObj, RegisterRequest.class);
        }

        // Create User
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordhash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullname(registerRequest.getFullName());
        user.setPhonenumber(registerRequest.getPhone());
        user.setRole(Role.customer); // Default role confirmed by user
        user.setStatus("Active");
        user.setCreatedat(Instant.now());

        userRepository.save(user);

        // Clear Redis cache
        redisTemplate.delete(OTP_PREFIX + request.getEmail());
        redisTemplate.delete(USER_REQ_PREFIX + request.getEmail());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordhash())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng");
        }

        if (!"Active".equals(user.getStatus())) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

        return generateTokenPair(user);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 1. Validate refresh token (chữ ký + hết hạn)
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        // 2. Kiểm tra loại token phải là REFRESH
        String tokenType = jwtUtil.extractTokenType(refreshToken);
        if (!"REFRESH".equals(tokenType)) {
            throw new RuntimeException("Token không phải là refresh token");
        }

        // 3. Kiểm tra refresh token có tồn tại trong Redis không (chống token bị thu hồi)
        String email = jwtUtil.extractEmail(refreshToken);
        String storedToken = (String) redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + email);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new RuntimeException("Refresh token đã bị thu hồi hoặc không hợp lệ");
        }

        // 4. Tìm user và tạo cặp token mới
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 5. Xóa refresh token cũ khỏi Redis (rotation)
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);

        return generateTokenPair(user);
    }

    @Override
    public void logout(String refreshToken) {
        if (jwtUtil.validateToken(refreshToken)) {
            String email = jwtUtil.extractEmail(refreshToken);
            redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);
        }
    }

    /**
     * Tạo cặp access token + refresh token, lưu refresh token vào Redis.
     */
    private LoginResponse generateTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Lưu refresh token vào Redis với TTL bằng thời gian sống của refresh token
        redisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + user.getEmail(),
                refreshToken,
                Duration.ofMillis(jwtUtil.getRefreshTokenExpirationMs())
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .fullName(user.getFullname())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }

        // Sinh mã OTP (6 chữ số)
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Lưu OTP vào Redis với TTL là 5 phút
        redisTemplate.opsForValue().set(
                FORGOT_PASSWORD_OTP_PREFIX + request.getEmail(),
                otp,
                Duration.ofMinutes(OTP_TTL_MINUTES)
        );

        // Gửi OTP qua email
        emailService.sendForgotPasswordOtpEmail(request.getEmail(), otp);
    }

    @Override
    public String verifyForgotOtp(VerifyForgotOtpRequest request) {
        String cachedOtp = (String) redisTemplate.opsForValue().get(FORGOT_PASSWORD_OTP_PREFIX + request.getEmail());

        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        // Tạo reset-token dạng UUID sau khi OTP được xác thực thành công
        String resetToken = UUID.randomUUID().toString();

        // Lưu reset-token vào Redis với thời gian hết hạn (TTL) là 3600000 giây
        redisTemplate.opsForValue().set(
                RESET_TOKEN_PREFIX + resetToken,
                request.getEmail(),
                Duration.ofSeconds(3600000)
        );

        // Xóa mã OTP đã sử dụng khỏi Redis
        redisTemplate.delete(FORGOT_PASSWORD_OTP_PREFIX + request.getEmail());

        return resetToken;
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // Lấy email từ resetToken trong Redis
        String email = (String) redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + request.getResetToken());
        if (email == null) {
            throw new RuntimeException("Reset token không hợp lệ hoặc đã hết hạn");
        }

        // Tìm người dùng theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Cập nhật mật khẩu mới bằng cách mã hóa bằng BCrypt trước khi lưu
        user.setPasswordhash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedat(java.time.Instant.now());
        userRepository.save(user);

        // Thu hồi (revoke) toàn bộ refresh token hiện có của người dùng
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);

        // Xóa reset-token khỏi Redis sau khi đã sử dụng thành công
        redisTemplate.delete(RESET_TOKEN_PREFIX + request.getResetToken());
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        String email = (String) org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        if (email == null || "anonymousUser".equals(email)) {
            throw new RuntimeException("Bạn cần đăng nhập để thực hiện chức năng này");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Xác thực mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordhash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        // Cập nhật mật khẩu mới bằng BCrypt hash
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordhash(newPasswordHash);
        user.setUpdatedat(Instant.now());
        userRepository.save(user);

        // Thu hồi các phiên đăng nhập hiện có để đảm bảo bảo mật
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + email);
    }
}
