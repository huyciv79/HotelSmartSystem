package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.config.Auth.JwtUtil;
import com.example.hotelsmartbookingbackend.dto.request.ForgotPasswordRequest;
import com.example.hotelsmartbookingbackend.dto.request.LoginRequest;

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

import org.springframework.web.util.HtmlUtils;
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

    private static final long OTP_TTL_MINUTES = 5;

    @Override
    public void registerInit(RegisterRequest request) {
        if (request.getEmail() != null) {
            request.setEmail(request.getEmail().trim().toLowerCase());
        }
        if (request.getPhone() != null) {
            request.setPhone(request.getPhone().trim());
        }
        if (request.getIdCardNumber() != null) {
            request.setIdCardNumber(request.getIdCardNumber().trim());
        }
        if (request.getFullName() != null) {
            request.setFullName(HtmlUtils.htmlEscape(request.getFullName().trim()));
        }

        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
        if (userRepository.existsByPhoneNumber(request.getPhone())) {
            throw new RuntimeException("Số điện thoại này đã tồn tại trong hệ thống");
        }
        if (userRepository.existsByIdCardNumber(request.getIdCardNumber())) {
            throw new RuntimeException("Số CCCD này đã tồn tại trong hệ thống");
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
        if (request.getEmail() != null) {
            request.setEmail(request.getEmail().trim().toLowerCase());
        }

        String cachedOtp = (String) redisTemplate.opsForValue().get(OTP_PREFIX + request.getEmail());

        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        Object cachedRequestObj = redisTemplate.opsForValue().get(USER_REQ_PREFIX + request.getEmail());
        if (cachedRequestObj == null) {
            throw new RuntimeException("Không tìm thấy thông tin đăng ký, vui lòng đăng ký lại");
        }

        RegisterRequest registerRequest;
        if (cachedRequestObj instanceof RegisterRequest) {
            registerRequest = (RegisterRequest) cachedRequestObj;
        } else {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            registerRequest = mapper.convertValue(cachedRequestObj, RegisterRequest.class);
        }

        if (registerRequest.getEmail() != null) {
            registerRequest.setEmail(registerRequest.getEmail().trim().toLowerCase());
        }
        if (registerRequest.getPhone() != null) {
            registerRequest.setPhone(registerRequest.getPhone().trim());
        }
        if (registerRequest.getIdCardNumber() != null) {
            registerRequest.setIdCardNumber(registerRequest.getIdCardNumber().trim());
        }
        if (registerRequest.getFullName() != null) {
            registerRequest.setFullName(HtmlUtils.htmlEscape(registerRequest.getFullName().trim()));
        }

        if (registerRequest.getIdCardNumber() == null || !registerRequest.getIdCardNumber().matches("^[0-9]{12}$")) {
            throw new RuntimeException("Số CCCD phải gồm đúng 12 chữ số");
        }
        if (userRepository.existsByEmailIgnoreCase(registerRequest.getEmail())) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
        if (userRepository.existsByPhoneNumber(registerRequest.getPhone())) {
            throw new RuntimeException("Số điện thoại này đã tồn tại trong hệ thống");
        }
        if (userRepository.existsByIdCardNumber(registerRequest.getIdCardNumber())) {
            throw new RuntimeException("Số CCCD này đã tồn tại trong hệ thống");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFullName(registerRequest.getFullName());
        user.setPhoneNumber(registerRequest.getPhone());
        user.setIdCardNumber(registerRequest.getIdCardNumber());
        user.setRole(Role.customer); // Default role confirmed by user
        user.setStatus("Active");
        user.setCreatedAt(Instant.now());

        userRepository.save(user);

        // Clear Redis cache
        redisTemplate.delete(OTP_PREFIX + request.getEmail());
        redisTemplate.delete(USER_REQ_PREFIX + request.getEmail());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng");
        }

        if (!"Active".equals(user.getStatus())) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

        return generateLoginResponse(user);
    }

    /**
     * Tạo LoginResponse chứa access token (không dùng refresh token).
     */
    private LoginResponse generateLoginResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(null)
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        request.setEmail(email);

        if (!userRepository.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("Email không tồn tại trong hệ thống");
        }

        // Sinh mã OTP (6 chữ số)
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Lưu OTP vào Redis với TTL là 5 phút
        redisTemplate.opsForValue().set(
                FORGOT_PASSWORD_OTP_PREFIX + email,
                otp,
                Duration.ofMinutes(OTP_TTL_MINUTES)
        );

        // Gửi OTP qua email
        emailService.sendForgotPasswordOtpEmail(email, otp);
    }

    @Override
    public String verifyForgotOtp(VerifyForgotOtpRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        request.setEmail(email);

        String cachedOtp = (String) redisTemplate.opsForValue().get(FORGOT_PASSWORD_OTP_PREFIX + email);

        if (cachedOtp == null || !cachedOtp.equals(request.getOtp())) {
            throw new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn");
        }

        // Tạo reset-token dạng UUID sau khi OTP được xác thực thành công
        String resetToken = UUID.randomUUID().toString();

        // Lưu reset-token vào Redis với thời gian hết hạn (TTL) là 3600000 giây
        redisTemplate.opsForValue().set(
                RESET_TOKEN_PREFIX + resetToken,
                email,
                Duration.ofSeconds(3600000)
        );

        // Xóa mã OTP đã sử dụng khỏi Redis
        redisTemplate.delete(FORGOT_PASSWORD_OTP_PREFIX + email);

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
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Cập nhật mật khẩu mới bằng cách mã hóa bằng BCrypt trước khi lưu
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(java.time.Instant.now());
        userRepository.save(user);



        // Xóa reset-token khỏi Redis sau khi đã sử dụng thành công
        redisTemplate.delete(RESET_TOKEN_PREFIX + request.getResetToken());
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String email = (authentication != null && authentication.getPrincipal() != null)
                ? authentication.getPrincipal().toString()
                : null;

        if (email == null || "anonymousUser".equals(email)) {
            throw new RuntimeException("Bạn cần đăng nhập để thực hiện chức năng này");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Xác thực mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        // Cập nhật mật khẩu mới bằng BCrypt hash
        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

    }
}
