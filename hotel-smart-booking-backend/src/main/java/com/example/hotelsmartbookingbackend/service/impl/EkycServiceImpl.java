package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.EkycRequest;
import com.example.hotelsmartbookingbackend.dto.response.AiEkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceEnrollmentResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceFrameValidationResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import com.example.hotelsmartbookingbackend.entity.EkycProfile;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.EkycProfileRepository;
import com.example.hotelsmartbookingbackend.repository.FaceembeddingAngleRepository;
import com.example.hotelsmartbookingbackend.repository.FaceembeddingRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.EkycService;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Triển khai luồng xử lý eKYC (Electronic Know Your Customer) – Tự động hóa hoàn toàn.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EkycServiceImpl implements EkycService {

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final UserRepository userRepository;
    private final EkycProfileRepository ekycProfileRepository;
    private final FaceembeddingRepository faceembeddingRepository;
    private final FaceembeddingAngleRepository faceembeddingAngleRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient webClient;
    private final AesEncryptionService aesEncryptionService;
    private final SupabaseStorageService supabaseStorageService;
    private final com.example.hotelsmartbookingbackend.service.NotificationService notificationService;

    // ── Config ────────────────────────────────────────────────────────────────
    @Value("${ai.service.ekyc-url:http://localhost:8000/api/v1/ai/verify-ekyc}")
    private String aiEkycUrl;

    @Value("${ai.service.face-enroll-url:http://localhost:8000/api/v1/face/enroll}")
    private String aiFaceEnrollUrl;

    @Value("${ai.service.face-frame-validation-url:http://localhost:8000/api/v1/face/validate-frame}")
    private String aiFaceFrameValidationUrl;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String EKYC_CACHE_PREFIX = "user:ekyc:";
    private static final Duration EKYC_CACHE_TTL = Duration.ofDays(7);
    private static final String VERIFICATION_METHOD = "Auto";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_AI_CHECKING = "AI_CHECKING";
    private static final String STATUS_VERIFIED = "VERIFIED";
    private static final Pattern FASTAPI_DETAIL_PATTERN = Pattern.compile(
            "\"detail\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\""
    );

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EkycResponse processEkyc(
            String email,
            MultipartFile frontImage,
            MultipartFile backImage,
            MultipartFile selfieImage,
            MultipartFile leftImage,
            MultipartFile rightImage,
            MultipartFile upImage,
            MultipartFile downImage
    ) {
        // ── Upload 3 ảnh lên Supabase → lấy public URL ───────────────────────
        String frontUrl   = uploadImage(frontImage,  "ekyc_front");
        String backUrl    = uploadImage(backImage,   "ekyc_back");
        String selfieUrl  = uploadImage(selfieImage, "ekyc_selfie");

        EkycRequest request = EkycRequest.builder()
                .frontImageUrl(frontUrl)
                .backImageUrl(backUrl)
                .faceImageUrl(selfieUrl)
                .build();

        // ── 1. Kiểm tra user tồn tại ─────────────────────────────────────────
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));

        if (!"Active".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa hoặc không hoạt động.");
        }

        // ── 2. Kiểm tra profile hiện tại ─────────────────────────────────────
        Optional<EkycProfile> profileOpt = ekycProfileRepository.findTopByUseridOrderByCreatedatDesc(user);
        boolean isUpdateFlow = false;
        EkycProfile profile;
        Instant now = Instant.now();

        if (profileOpt.isPresent()) {
            EkycProfile existingProfile = profileOpt.get();
            if (isVerifiedStatus(existingProfile.getStatus())) {
                throw new RuntimeException("Tài khoản của bạn đã được xác minh eKYC trước đó.");
            } else {
                isUpdateFlow = true;
                profile = existingProfile;
            }
        } else {
            profile = null;
        }

        // ── 3. Tạo/Cập nhật bản ghi EkycProfile với trạng thái SUBMITTED rồi AI_CHECKING ──
        if (isUpdateFlow) {
            profile.setFrontimage(request.getFrontImageUrl());
            profile.setBackimage(request.getBackImageUrl());
            profile.setFaceimage(request.getFaceImageUrl());
            profile.setStatus(STATUS_SUBMITTED);
            profile.setRejectionreason(null);
            profile.setVerificationmethod(VERIFICATION_METHOD);
            profile.setUpdatedat(now);
            ekycProfileRepository.save(profile);
        } else {
            profile = createSubmittedProfile(user, request);
            ekycProfileRepository.save(profile);
        }
        Instant aiStartedAt = Instant.now();
        ekycProfileRepository.markAsAiChecking(profile.getId(), aiStartedAt);
        profile.setStatus(STATUS_AI_CHECKING);
        profile.setUpdatedat(aiStartedAt);

        // ── 4. OCR CCCD ───────────────────────────────────────────────────────
        AiEkycResponse ocrResponse;
        try {
            ocrResponse = callAiService(request);
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Không thể đọc thông tin CCCD. Hãy chụp đúng mặt trước, mặt sau, "
                            + "đặt thẻ trong khung và bảo đảm ảnh rõ nét.",
                    ex
            );
        }

        String idCardNumber = normalizeIdCardNumber(ocrResponse.getIdCardNumber());
        String fullName     = blankToNull(ocrResponse.getFullName());
        String dateOfBirth  = blankToNull(ocrResponse.getDateOfBirth());
        String gender       = blankToNull(ocrResponse.getGender());
        String hometown     = blankToNull(ocrResponse.getHometown());
        String provinceCode = blankToNull(ocrResponse.getProvinceCode());
        String provinceName = blankToNull(ocrResponse.getProvinceName());
        if (hometown == null) {
            hometown = provinceName;
        }

        if (Boolean.FALSE.equals(ocrResponse.getValidationPassed())) {
            String validationDetail = ocrResponse.getValidationErrors() == null
                    || ocrResponse.getValidationErrors().isEmpty()
                    ? "CCCD OCR cross-field validation failed"
                    : String.join("; ", ocrResponse.getValidationErrors());
            throw new RuntimeException("Thông tin CCCD không nhất quán: " + validationDetail);
        }

        log.info(
                "CCCD OCR extracted id={}, fullName={}, dob={}, gender={}, hometown={}, province={}/{}",
                maskIdNumber(idCardNumber),
                fullName,
                dateOfBirth,
                gender,
                hometown,
                provinceCode,
                provinceName
        );

        if (idCardNumber == null) {
            throw new RuntimeException(
                    "Không nhận diện được số CCCD trên ảnh mặt trước. "
                            + "Vui lòng chụp lại mặt trước CCCD rõ nét và đầy đủ bốn góc."
            );
        }
        String registeredIdCardNumber = normalizeIdCardNumber(user.getIdcardnumber());
        if (registeredIdCardNumber == null) {
            throw new RuntimeException("Tai khoan chua co so CCCD da dang ky.");
        }
        if (!registeredIdCardNumber.equals(idCardNumber)) {
            throw new RuntimeException("So CCCD tren anh khong khop voi so CCCD da dang ky tai khoan.");
        }

        // ── 5. Liveness + Face enrollment ─────────────────────────────────────
        AiFaceEnrollmentResponse faceResponse;
        try {
            faceResponse = callFaceEnrollmentService(
                    selfieImage, leftImage, rightImage, upImage, downImage
            );
            if (faceResponse == null
                    || !Boolean.TRUE.equals(faceResponse.getEnrolled())
                    || !Boolean.TRUE.equals(faceResponse.getLivenessPassed())
                    || !Boolean.TRUE.equals(faceResponse.getActiveLivenessPassed())) {
                String detail = faceResponse != null
                        ? faceResponse.getMessage()
                        : "Face API returned an empty response";
                throw new RuntimeException(detail);
            }
            if (faceResponse.getEmbedding() == null || faceResponse.getEmbedding().size() != 512) {
                throw new RuntimeException("Face API did not return a valid 512-dimensional embedding");
            }
        } catch (Exception ex) {
            String aiDetail = ex.getMessage();
            boolean isAiDetail = aiDetail != null
                    && !aiDetail.startsWith("Face API")
                    && !aiDetail.startsWith("Không thể kết nối");
            throw new RuntimeException(
                    isAiDetail
                            ? aiDetail
                            : "Đăng ký khuôn mặt thất bại. Hãy thực hiện rõ đủ năm tư thế, "
                            + "bảo đảm chỉ có một người và không dùng ảnh hoặc màn hình.",
                    ex
            );
        }

        // ── 6. Lưu thông tin CCCD và embedding FaceID ────────────────────────
        return handleVerificationSuccess(
                user, profile,
                idCardNumber, fullName, dateOfBirth, gender, hometown,
                provinceCode, provinceName,
                faceResponse, isUpdateFlow
        );
    }

    private void saveAngleEmbeddings(
            Integer userId,
            List<AiFaceEnrollmentResponse.FaceAngleEmbedding> angleEmbeddings,
            Instant now
    ) {
        if (angleEmbeddings == null || angleEmbeddings.isEmpty()) {
            return;
        }

        Integer embeddingId = faceembeddingRepository.findEmbeddingIdByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Khong tim thay FaceEmbedding vua luu"));

        faceembeddingAngleRepository.deleteByEmbeddingId(embeddingId);
        for (AiFaceEnrollmentResponse.FaceAngleEmbedding angle : angleEmbeddings) {
            if (angle == null || angle.getPose() == null || angle.getEmbedding() == null) {
                continue;
            }

            String pose = angle.getPose().trim().toLowerCase(Locale.ROOT);
            if (!List.of("center", "left", "right", "up", "down").contains(pose)) {
                throw new RuntimeException("Face API tra ve pose khong hop le: " + angle.getPose());
            }

            faceembeddingAngleRepository.upsertAngle(
                    embeddingId,
                    pose,
                    convertToPostgresVectorString(angle.getEmbedding()),
                    angle.getYawScore(),
                    angle.getPitchScore(),
                    angle.getQualityScore(),
                    angle.getLivenessScore(),
                    angle.getAvailable() == null || Boolean.TRUE.equals(angle.getAvailable()),
                    angle.getFailReason(),
                    now
            );
        }
    }

    /**
     * Lấy trạng thái eKYC của user và thông tin chi tiết liên quan.
     */
    @Override
    public EkycStatusResponse getEkycStatus(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));

        Optional<EkycProfile> profileOpt = ekycProfileRepository.findTopByUseridOrderByCreatedatDesc(user);

        if (profileOpt.isEmpty()) {
            return EkycStatusResponse.builder()
                    .status("NOT_FOUND")
                    .message("You have not registered an identity verification profile yet.")
                    .fullName(user.getFullname())
                    .address(user.getAddress())
                    .build();
        }

        EkycProfile profile = profileOpt.get();

        String statusStr = normalizeEkycStatus(profile.getStatus());
        String statusMsg = switch (statusStr) {
            case STATUS_VERIFIED -> "Xac minh thanh cong.";
            case STATUS_AI_CHECKING -> "AI dang trich xuat du lieu.";
            case STATUS_SUBMITTED -> "Da nop ho so.";
            default -> "Ban chua dang ky ho so xac minh danh tinh.";
        };

        // Giải mã số CCCD (lưu dạng AES-256-GCM)
        String decryptedIdNumber = null;
        if (profile.getIdcardnumber() != null) {
            try {
                decryptedIdNumber = aesEncryptionService.decrypt(profile.getIdcardnumber());
                if (STATUS_VERIFIED.equals(statusStr)) {
                    decryptedIdNumber = maskIdNumber(decryptedIdNumber);
                }
            } catch (Exception ex) {
                // null → frontend hiển thị "N/A"
            }
        }

        String decryptedFullName = null;
        if (profile.getFullname() != null) {
            try { decryptedFullName = aesEncryptionService.decrypt(profile.getFullname()); }
            catch (Exception ex) { /* ignore */ }
        }

        String decryptedDob = null;
        if (profile.getDateofbirth() != null) {
            try { decryptedDob = aesEncryptionService.decrypt(profile.getDateofbirth()); }
            catch (Exception ex) { /* ignore */ }
        }

        String decryptedGender = null;
        if (profile.getGender() != null) {
            try { decryptedGender = aesEncryptionService.decrypt(profile.getGender()); }
            catch (Exception ex) { /* ignore */ }
        }

        String decryptedHometown = null;
        if (profile.getHometown() != null) {
            try { decryptedHometown = aesEncryptionService.decrypt(profile.getHometown()); }
            catch (Exception ex) { /* ignore */ }
        }

        String decryptedProvinceName = null;
        if (profile.getProvincename() != null) {
            try { decryptedProvinceName = aesEncryptionService.decrypt(profile.getProvincename()); }
            catch (Exception ex) { /* ignore */ }
        }

        return EkycStatusResponse.builder()
                .status(statusStr)
                .message(statusMsg)
                .fullName(decryptedFullName != null ? decryptedFullName : user.getFullname())
                .idNumber(decryptedIdNumber)
                .dateOfBirth(decryptedDob)
                .address(user.getAddress())
                .gender(decryptedGender)
                .hometown(decryptedHometown != null ? decryptedHometown : decryptedProvinceName)
                .provinceCode(profile.getProvincecode())
                .provinceName(decryptedProvinceName)
                .verifiedAt(profile.getVerifiedat())
                .rejectionReason(profile.getRejectionreason())
                .requestId(profile.getId() != null ? profile.getId().toString() : null)
                .frontImage(supabaseStorageService.getSignedUrl(profile.getFrontimage()))
                .backImage(supabaseStorageService.getSignedUrl(profile.getBackimage()))
                .faceImage(supabaseStorageService.getSignedUrl(profile.getFaceimage()))
                .build();
    }

    @Override
    public AiFaceFrameValidationResponse validateLivenessFrame(
            MultipartFile frameImage,
            MultipartFile referenceImage,
            MultipartFile oppositeImage,
            String step
    ) {
        if (frameImage == null || frameImage.isEmpty()) {
            throw new RuntimeException("Frame khuôn mặt không có dữ liệu.");
        }
        String normalizedStep = step == null ? "" : step.trim().toLowerCase();
        if (!List.of("center", "left", "right", "up", "down").contains(normalizedStep)) {
            throw new RuntimeException("Bước liveness không hợp lệ.");
        }
        if (!"center".equals(normalizedStep)
                && (referenceImage == null || referenceImage.isEmpty())) {
            throw new RuntimeException("Thiếu ảnh chính diện tham chiếu.");
        }

        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        addImagePart(bodyBuilder, "frame_image", frameImage);
        bodyBuilder.part("step", normalizedStep).contentType(MediaType.TEXT_PLAIN);
        if (referenceImage != null && !referenceImage.isEmpty()) {
            addImagePart(bodyBuilder, "reference_image", referenceImage);
        }
        if (oppositeImage != null && !oppositeImage.isEmpty()) {
            addImagePart(bodyBuilder, "opposite_image", oppositeImage);
        }

        try {
            AiFaceFrameValidationResponse response = webClient.post()
                    .uri(aiFaceFrameValidationUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(AiFaceFrameValidationResponse.class)
                    .timeout(Duration.ofSeconds(120))
                    .block();
            if (response == null) {
                throw new RuntimeException("Face validation API trả về phản hồi rỗng.");
            }
            return response;
        } catch (WebClientResponseException ex) {
            String detail = extractFastApiDetail(ex.getResponseBodyAsString());
            throw new RuntimeException(
                    detail != null ? detail : "Face validation API lỗi: " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Không thể kết nối tới dịch vụ kiểm tra tư thế khuôn mặt.", ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private EkycProfile createSubmittedProfile(User user, EkycRequest request) {
        Instant now = Instant.now();
        EkycProfile profile = new EkycProfile();
        profile.setUserid(user);
        profile.setFrontimage(request.getFrontImageUrl());
        profile.setBackimage(request.getBackImageUrl());
        profile.setFaceimage(request.getFaceImageUrl());
        profile.setStatus(STATUS_SUBMITTED);
        profile.setVerificationmethod(VERIFICATION_METHOD);
        profile.setCreatedat(now);
        profile.setUpdatedat(now);
        return profile;
    }

    private AiEkycResponse callAiService(EkycRequest request) {
        Map<String, String> payload = Map.of(
                "front_image_url", request.getFrontImageUrl(),
                "back_image_url",  request.getBackImageUrl()
        );

        try {
            AiEkycResponse response = webClient.post()
                    .uri(aiEkycUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(AiEkycResponse.class)
                    .timeout(Duration.ofSeconds(300))
                    .block();

            if (response == null) {
                throw new RuntimeException("AI Service trả về phản hồi rỗng");
            }
            return response;
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("AI Service lỗi: " + ex.getResponseBodyAsString(), ex);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Không thể kết nối tới dịch vụ xác minh AI. Vui lòng thử lại sau.", ex);
        }
    }

    private AiFaceEnrollmentResponse callFaceEnrollmentService(
            MultipartFile selfieImage,
            MultipartFile leftImage,
            MultipartFile rightImage,
            MultipartFile upImage,
            MultipartFile downImage
    ) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        addImagePart(bodyBuilder, "selfie_image", selfieImage);
        addImagePart(bodyBuilder, "left_image",   leftImage);
        addImagePart(bodyBuilder, "right_image",  rightImage);
        addImagePart(bodyBuilder, "up_image",     upImage);
        addImagePart(bodyBuilder, "down_image",   downImage);

        try {
            AiFaceEnrollmentResponse response = webClient.post()
                    .uri(aiFaceEnrollUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(AiFaceEnrollmentResponse.class)
                    .timeout(Duration.ofSeconds(300))
                    .block();

            if (response == null) {
                throw new RuntimeException("Face API trả về phản hồi rỗng");
            }
            return response;
        } catch (WebClientResponseException ex) {
            String detail = extractFastApiDetail(ex.getResponseBodyAsString());
            throw new RuntimeException(
                    detail != null ? detail : "Face API lỗi: " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Không thể kết nối tới dịch vụ nhận diện khuôn mặt.", ex);
        }
    }

    private void addImagePart(MultipartBodyBuilder bodyBuilder, String partName, MultipartFile file) {
        Resource resource = file.getResource();
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        if (file.getContentType() != null && !file.getContentType().isBlank()) {
            contentType = MediaType.parseMediaType(file.getContentType());
        }
        bodyBuilder.part(partName, resource)
                .filename(file.getOriginalFilename() != null
                        ? file.getOriginalFilename()
                        : partName + ".jpg")
                .contentType(contentType);
    }

    /**
     * Xử lý khi xác minh thành công.
     */
    private EkycResponse handleVerificationSuccess(
            User user,
            EkycProfile profile,
            String idCardNumber,
            String fullName,
            String dateOfBirth,
            String gender,
            String hometown,
            String provinceCode,
            String provinceName,
            AiFaceEnrollmentResponse faceResponse,
            boolean isUpdateFlow
    ) {
        Instant now = Instant.now();

        if (idCardNumber == null || idCardNumber.isBlank()) {
            throw new RuntimeException("Không thể xác minh eKYC khi chưa nhận diện được số CCCD.");
        }

        String idCardNumberHash = aesEncryptionService.hmac(idCardNumber);
        boolean isDuplicate = isUpdateFlow
                ? ekycProfileRepository.existsVerifiedByIdcardnumberhashAndUseridNot(idCardNumberHash, user)
                : ekycProfileRepository.existsVerifiedByIdcardnumberhash(idCardNumberHash);

        if (isDuplicate) {
            throw new RuntimeException("Số CCCD này đã được liên kết với một tài khoản khác.");
        }

        String registeredIdCardNumber = normalizeIdCardNumber(user.getIdcardnumber());
        if (registeredIdCardNumber == null || !registeredIdCardNumber.equals(idCardNumber)) {
            throw new RuntimeException("So CCCD tren anh khong khop voi so CCCD da dang ky tai khoan.");
        }

        // Cập nhật EkycProfile -> VERIFIED
        ekycProfileRepository.markAsVerified(profile.getId(), now, now, VERIFICATION_METHOD);

        // Mã hóa AES-256-GCM
        String encryptedIdCardNumber = aesEncryptionService.encrypt(idCardNumber);
        String encryptedFullName     = fullName     != null ? aesEncryptionService.encrypt(fullName)     : null;
        String encryptedDob          = dateOfBirth  != null ? aesEncryptionService.encrypt(dateOfBirth)  : null;
        String encryptedGender       = gender       != null ? aesEncryptionService.encrypt(gender)       : null;
        String encryptedHometown     = hometown     != null ? aesEncryptionService.encrypt(hometown)     : null;
        String encryptedProvinceName = provinceName != null ? aesEncryptionService.encrypt(provinceName) : null;

        // Lưu ciphertext + HMAC hash
        ekycProfileRepository.updateIdCardDetailsAndHash(
                profile.getId(),
                encryptedIdCardNumber,
                idCardNumberHash,
                encryptedFullName,
                encryptedDob,
                encryptedGender,
                encryptedHometown,
                provinceCode,
                encryptedProvinceName
        );

        // Upsert FaceEmbedding
        String embeddingVector = convertToPostgresVectorString(faceResponse.getEmbedding());
        faceembeddingRepository.upsertEmbedding(user.getId(), embeddingVector, now);
        saveAngleEmbeddings(user.getId(), faceResponse.getAngleEmbeddings(), now);

        // Cache Redis
        cacheEkycStatus(user.getId(), STATUS_VERIFIED);

        String successMessage = isUpdateFlow
                ? "CCCD đã được xác minh và FaceID đã được cập nhật thành công."
                : "CCCD đã được xác minh và FaceID đã được đăng ký thành công cho check-in.";

        try {
            notificationService.sendNotification(
                    user, 
                    "Xác minh danh tính eKYC thành công", 
                    "Hồ sơ danh tính eKYC của bạn đã được xác minh tự động thành công. Bạn đã có thể check-in phòng bằng nhận diện khuôn mặt và mã QR.", 
                    "Ekyc", 
                    profile.getId());
        } catch (Exception e) {
            log.error("Failed to send ekyc success notification: ", e);
        }

        return EkycResponse.builder()
                .status(STATUS_VERIFIED)
                .message(successMessage)
                .recognizedIdNumber(idCardNumber)
                .build();
    }

    private String convertToPostgresVectorString(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new RuntimeException("Face API trả về embedding rỗng");
        }
        if (embedding.size() != 512) {
            throw new RuntimeException("Embedding khuôn mặt phải có đúng 512 phần tử");
        }
        String vectorContent = embedding.stream()
                .map(d -> String.format(Locale.US, "%.8f", d))
                .collect(Collectors.joining(","));
        return "[" + vectorContent + "]";
    }

    private boolean isAlreadyVerifiedInCache(Integer userId) {
        Object cached = redisTemplate.opsForValue().get(EKYC_CACHE_PREFIX + userId);
        if (!STATUS_VERIFIED.equals(cached)) {
            return false;
        }
        User userRef = new User();
        userRef.setId(userId);
        boolean verifiedInDb = ekycProfileRepository.existsVerifiedByUserid(userRef);
        if (!verifiedInDb) {
            redisTemplate.delete(EKYC_CACHE_PREFIX + userId);
            return false;
        }
        return true;
    }

    private void cacheEkycStatus(Integer userId, String status) {
        redisTemplate.opsForValue().set(
                EKYC_CACHE_PREFIX + userId,
                status,
                EKYC_CACHE_TTL
        );
    }

    private boolean isVerifiedStatus(String status) {
        return STATUS_VERIFIED.equals(normalizeEkycStatus(status));
    }

    private String normalizeEkycStatus(String status) {
        if (status == null || status.isBlank()) {
            return STATUS_SUBMITTED;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case STATUS_VERIFIED -> STATUS_VERIFIED;
            case STATUS_AI_CHECKING -> STATUS_AI_CHECKING;
            case "PENDING" -> STATUS_AI_CHECKING;
            case STATUS_SUBMITTED, "REJECTED" -> STATUS_SUBMITTED;
            default -> STATUS_SUBMITTED;
        };
    }

    private String normalizeIdCardNumber(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        String digitsOnly = normalized.replaceAll("[^0-9]", "");
        return digitsOnly.isBlank() ? null : digitsOnly;
    }

    private String maskIdNumber(String id) {
        if (id == null || id.isBlank()) return id;
        int length = id.length();
        if (length <= 5) return "*".repeat(length);
        return id.substring(0, 2) + "*".repeat(length - 5) + id.substring(length - 3);
    }

    private String extractFastApiDetail(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return null;
        Matcher matcher = FASTAPI_DETAIL_PATTERN.matcher(responseBody);
        if (!matcher.find()) return null;
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        if (normalized.equals("null") || normalized.equals("none")
                || normalized.equals("nan") || normalized.equals("n/a")
                || normalized.equals("na") || trimmed.equals("-")
                || trimmed.equals("--") || trimmed.equals("---")
                || trimmed.equals("—") || trimmed.equals("–")) {
            return null;
        }
        return trimmed;
    }

    private String uploadImage(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File '" + prefix + "' không được để trống.");
        }
        try {
            String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
            String originalName = prefix + "_" + file.getOriginalFilename();
            return supabaseStorageService.uploadEkycDocument(file.getBytes(), originalName, contentType);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc file upload: " + e.getMessage(), e);
        }
    }
}
