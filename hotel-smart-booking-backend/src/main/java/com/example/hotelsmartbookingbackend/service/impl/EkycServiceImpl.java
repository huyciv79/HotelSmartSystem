package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.request.EkycRequest;
import com.example.hotelsmartbookingbackend.dto.response.AiEkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;
import com.example.hotelsmartbookingbackend.entity.EkycProfile;
import com.example.hotelsmartbookingbackend.entity.User;
import com.example.hotelsmartbookingbackend.repository.EkycProfileRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Triển khai luồng xử lý eKYC (Electronic Know Your Customer) – Tự động hóa hoàn toàn.
 *
 * <p>Luồng hoạt động mới:
 * <ol>
 *   <li>Client gửi 3 URL ảnh (mặt trước CCCD, mặt sau CCCD, ảnh selfie) – <b>không cần nhập số CCCD</b>.</li>
 *   <li>Tạo bản ghi EkycProfile tạm với trạng thái "Pending".</li>
 *   <li>Gọi Python AI Service:
 *     <ul>
 *       <li>EasyOCR tự đọc Số CCCD từ ảnh mặt trước.</li>
 *       <li>DeepFace.represent trích xuất vector embedding 512 chiều từ ảnh selfie.</li>
 *     </ul>
 *   </li>
 *   <li>Nếu OCR không đọc được số CCCD → báo lỗi ngay cho client.</li>
 *   <li>Nếu hợp lệ → lưu Số CCCD vào EkycProfile (Verified) và embedding vào FaceEmbeddings.</li>
 * </ol>
 *
 * <p>Tính toàn vẹn: @Transactional bảo vệ ghi vào 2 bảng trong cùng một transaction.
 * Redis được cập nhật sau khi commit thành công.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EkycServiceImpl implements EkycService {

    // ── Dependencies ─────────────────────────────────────────────────────────
    private final UserRepository userRepository;
    private final EkycProfileRepository ekycProfileRepository;
    private final FaceembeddingRepository faceembeddingRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final WebClient webClient;
    private final AesEncryptionService aesEncryptionService;
    private final SupabaseStorageService supabaseStorageService;

    @Autowired
    @Lazy
    private EkycServiceImpl self;

    // ── Config ────────────────────────────────────────────────────────────────
    @Value("${ai.service.ekyc-url:http://localhost:8000/api/v1/ai/verify-ekyc}")
    private String aiEkycUrl;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String EKYC_CACHE_PREFIX = "user:ekyc:";
    private static final Duration EKYC_CACHE_TTL = Duration.ofDays(7);
    private static final String VERIFICATION_METHOD = "Auto";

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EkycResponse processEkyc(Integer userId, EkycRequest request) {
        log.info("[eKYC] Bắt đầu xử lý eKYC tự động cho userId={}", userId);

        // ── 1. Kiểm tra user tồn tại ─────────────────────────────────────────
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + userId));

        // PRE-2: The Customer account status is Active
        if (!"Active".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Tài khoản của bạn đã bị khóa hoặc không hoạt động.");
        }

        // ── 2. Kiểm tra profile hiện tại và phân loại luồng Register / Update ─────
        Optional<EkycProfile> profileOpt = ekycProfileRepository.findTopByUseridOrderByCreatedatDesc(user);
        boolean isUpdateFlow = false;
        EkycProfile profile;
        Instant now = Instant.now();

        if (profileOpt.isPresent()) {
            EkycProfile existingProfile = profileOpt.get();
            if ("Verified".equalsIgnoreCase(existingProfile.getStatus())) {
                log.warn("[eKYC] userId={} đã được xác minh trước đó, bỏ qua.", userId);
                throw new RuntimeException("Tài khoản của bạn đã được xác minh eKYC trước đó.");
            } else if ("Pending".equalsIgnoreCase(existingProfile.getStatus())) {
                log.warn("[eKYC] userId={} đang có yêu cầu xác minh chờ xử lý.", userId);
                throw new RuntimeException("Yêu cầu xác minh eKYC của bạn đang được xử lý, không thể thực hiện yêu cầu mới.");
            } else if ("Rejected".equalsIgnoreCase(existingProfile.getStatus())) {
                isUpdateFlow = true;
                profile = existingProfile;
                log.info("[eKYC] Phát hiện profile Rejected cho userId={}. Chuyển sang luồng Update.", userId);
            } else {
                isUpdateFlow = true;
                profile = existingProfile;
            }
        } else {
            profile = null;
        }

        // ── 3. Tạo/Cập nhật bản ghi EkycProfile tạm với trạng thái Pending ─────────
        if (isUpdateFlow) {
            profile.setFrontimage(request.getFrontImageUrl());
            profile.setBackimage(request.getBackImageUrl());
            profile.setFaceimage(request.getFaceImageUrl());
            profile.setStatus("Pending");
            profile.setRejectionreason(null);
            profile.setVerificationmethod(VERIFICATION_METHOD);
            profile.setUpdatedat(now);
            ekycProfileRepository.save(profile);
            log.info("[eKYC] Đã cập nhật profile ekycId={} sang Pending cho userId={}", profile.getId(), userId);
        } else {
            profile = createPendingProfile(user, request);
            ekycProfileRepository.save(profile);
            log.info("[eKYC] Đã tạo EkycProfile mới id={} (Pending) cho userId={}", profile.getId(), userId);
        }

        // ── 4. Gọi Python AI Service và bắt exception ──────────────────────────
        AiEkycResponse aiResponse;
        try {
            aiResponse = callAiService(request);
            if (aiResponse == null || aiResponse.getEmbedding() == null || aiResponse.getEmbedding().isEmpty()) {
                throw new RuntimeException("AI Service trả về kết quả hoặc vector khuôn mặt rỗng");
            }
        } catch (Exception ex) {
            log.error("[eKYC] Lỗi trích xuất khuôn mặt hoặc kết nối AI Service cho userId={}: {}", userId, ex.getMessage());

            // Đánh dấu status = Rejected, cập nhật lý do từ chối vào DB bằng new transaction để tránh rollback
            String rejectionReason = "Face detection failed: " + ex.getMessage();
            self.markAsRejectedNewTx(profile.getId(), rejectionReason, Instant.now());

            if (isUpdateFlow) {
                throw new RuntimeException(
                        "Verification failed. Please ensure your face is clearly visible and well-lit, or bring your original identification documents to the reception desk for manual verification during check-in."
                );
            } else {
                throw new RuntimeException(
                        "Face detection failed. Please ensure your face is clearly visible, well-lit, and fits inside the frame, then try again."
                );
            }
        }

        // ── 5. Kiểm tra số CCCD bóc tách từ OCR ────────────────────────────
        String idCardNumber = aiResponse.getIdCardNumber();
        if (idCardNumber == null || idCardNumber.isBlank()) {
            log.warn("[eKYC] OCR không đọc được số CCCD cho userId={}", userId);

            // Đánh dấu Rejected
            self.markAsRejectedNewTx(
                    profile.getId(),
                    "OCR không nhận dạng được số CCCD từ ảnh mặt trước",
                    Instant.now()
            );

            throw new RuntimeException(
                    "Unable to read document clearly. Please ensure the image is bright, unblurred, and clear of glare, then try again."
            );
        }

        log.info("[eKYC] OCR đọc được Số CCCD thành công ({}***) cho userId={}",
                idCardNumber.substring(0, Math.min(4, idCardNumber.length())), userId);

        // ── 5b. So khớp Họ tên giữa OCR và thông tin tài khoản đăng ký ────────────────────────────
        String ocrName = aiResponse.getFullName();
        if (ocrName == null || ocrName.isBlank()) {
            log.warn("[eKYC] OCR không đọc được họ tên cho userId={}", userId);
            self.markAsRejectedNewTx(
                    profile.getId(),
                    "OCR không đọc được họ tên trên mặt trước CCCD",
                    Instant.now()
            );
            throw new RuntimeException(
                    "Unable to read your name clearly from the ID card. Please ensure the image is bright, unblurred, and try again."
            );
        }

        String normalizedUser = removeDiacritics(user.getFullname()).replaceAll("\\s+", "").trim().toLowerCase();
        String normalizedOcr = removeDiacritics(ocrName).replaceAll("\\s+", "").trim().toLowerCase();

        if (!normalizedUser.equals(normalizedOcr)) {
            log.warn("[eKYC] Họ tên không khớp cho userId={}: user='{}', ocr='{}'", userId, user.getFullname(), ocrName);
            self.markAsRejectedNewTx(
                    profile.getId(),
                    "Họ tên trên CCCD (" + ocrName + ") không khớp với tên đăng ký tài khoản (" + user.getFullname() + ")",
                    Instant.now()
            );
            throw new RuntimeException(
                    "Verification failed. The name on your ID card does not match your registered profile name."
            );
        }

        // ── 6. Xác minh thành công: lưu Số CCCD + Họ tên + Ngày sinh + embedding ──────────────────
        return handleVerificationSuccess(user, profile, idCardNumber, aiResponse.getFullName(),
                aiResponse.getDateOfBirth(), aiResponse.getEmbedding(), isUpdateFlow);
    }

    /**
     * Lấy trạng thái eKYC của user và thông tin chi tiết liên quan.
     */
    @Override
    public EkycStatusResponse getEkycStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với ID: " + userId));

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
        
        String statusStr = "NOT_FOUND";
        String statusMsg = "";
        
        if ("Verified".equalsIgnoreCase(profile.getStatus())) {
            statusStr = "VERIFIED";
            statusMsg = "Your identity has been successfully verified.";
        } else if ("Pending".equalsIgnoreCase(profile.getStatus())) {
            statusStr = "PENDING";
            statusMsg = "Your identity verification request is currently being processed by the system or a receptionist. Please wait.";
        } else if ("Rejected".equalsIgnoreCase(profile.getStatus())) {
            statusStr = "REJECTED";
            statusMsg = profile.getRejectionreason() != null ? profile.getRejectionreason() : "Your identity verification request was rejected.";
        }

        // Giải mã số CCCD trước khi trả về (lưu DB dưới dạng AES-256-GCM ciphertext)
        String decryptedIdNumber = null;
        if (profile.getIdcardnumber() != null) {
            try {
                decryptedIdNumber = aesEncryptionService.decrypt(profile.getIdcardnumber());
                if ("VERIFIED".equals(statusStr)) {
                    decryptedIdNumber = maskIdNumber(decryptedIdNumber);
                }
            } catch (Exception ex) {
                log.error("[eKYC] Không thể giải mã idcardnumber cho ekycId={}: {}",
                        profile.getId(), ex.getMessage());
                // Trả về null thay vì crash – frontend sẽ hiển thị "N/A"
            }
        }

        String decryptedFullName = null;
        if (profile.getFullname() != null) {
            try {
                decryptedFullName = aesEncryptionService.decrypt(profile.getFullname());
            } catch (Exception ex) {
                log.error("[eKYC] Không thể giải mã fullname cho ekycId={}: {}",
                        profile.getId(), ex.getMessage());
            }
        }

        String decryptedDob = null;
        if (profile.getDateofbirth() != null) {
            try {
                decryptedDob = aesEncryptionService.decrypt(profile.getDateofbirth());
            } catch (Exception ex) {
                log.error("[eKYC] Không thể giải mã dateofbirth cho ekycId={}: {}",
                        profile.getId(), ex.getMessage());
            }
        }

        return EkycStatusResponse.builder()
                .status(statusStr)
                .message(statusMsg)
                .fullName(decryptedFullName != null ? decryptedFullName : user.getFullname())
                .idNumber(decryptedIdNumber)
                .dateOfBirth(decryptedDob)
                .address(user.getAddress())
                .verifiedAt(profile.getVerifiedat())
                .rejectionReason(profile.getRejectionreason())
                .requestId(profile.getId() != null ? profile.getId().toString() : null)
                .frontImage(supabaseStorageService.getSignedUrl(profile.getFrontimage()))
                .backImage(supabaseStorageService.getSignedUrl(profile.getBackimage()))
                .faceImage(supabaseStorageService.getSignedUrl(profile.getFaceimage()))
                .build();
    }

    /**
     * Mask số CCCD bảo mật thông tin cá nhân.
     * Ví dụ: "091234123" (độ dài 9) -> "09****123"
     */
    private String maskIdNumber(String id) {
        if (id == null || id.isBlank()) {
            return id;
        }
        int length = id.length();
        if (length <= 5) {
            return "*".repeat(length);
        }
        return id.substring(0, 2) + "*".repeat(length - 5) + id.substring(length - 3);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo EkycProfile mới với trạng thái "Pending".
     * IDCardNumber để null vì số CCCD chưa được OCR; sẽ cập nhật sau.
     */
    private EkycProfile createPendingProfile(User user, EkycRequest request) {
        Instant now = Instant.now();
        EkycProfile profile = new EkycProfile();
        profile.setUserid(user);
        profile.setFrontimage(request.getFrontImageUrl());
        profile.setBackimage(request.getBackImageUrl());
        profile.setFaceimage(request.getFaceImageUrl());
        profile.setStatus("Pending");
        profile.setVerificationmethod(VERIFICATION_METHOD);
        profile.setCreatedat(now);
        profile.setUpdatedat(now);
        return profile;
    }

    /**
     * Gọi Python AI Service thông qua WebClient.
     * AI Service tự đọc Số CCCD bằng OCR và trích xuất embedding từ selfie.
     *
     * <p>Payload gửi lên: { front_image_url, back_image_url, face_image_url }
     *
     * @throws RuntimeException nếu AI service không phản hồi hoặc trả về lỗi HTTP
     */
    private AiEkycResponse callAiService(EkycRequest request) {
        Map<String, String> payload = Map.of(
                "front_image_url", request.getFrontImageUrl(),
                "back_image_url",  request.getBackImageUrl(),
                "face_image_url",  request.getFaceImageUrl()
        );

        log.info("[eKYC] Gọi Python AI Service tại URL: {}", aiEkycUrl);
        try {
            AiEkycResponse response = webClient.post()
                    .uri(aiEkycUrl)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(AiEkycResponse.class)
                    .timeout(Duration.ofSeconds(120)) // Model load lần đầu có thể mất 60-90s
                    .block(); // Block vì đang trong @Transactional context

            if (response == null) {
                throw new RuntimeException("AI Service trả về phản hồi rỗng");
            }

            log.info("[eKYC] Nhận kết quả từ AI Service: idCardNumber={}, embeddingSize={}",
                    response.getIdCardNumber(),
                    response.getEmbedding() != null ? response.getEmbedding().size() : 0);
            return response;

        } catch (WebClientResponseException ex) {
            log.error("[eKYC] AI Service trả về lỗi HTTP {}: {}", ex.getStatusCode(), ex.getMessage());
            throw new RuntimeException("AI Service lỗi: " + ex.getResponseBodyAsString(), ex);
        } catch (RuntimeException ex) {
            // Rethrow RuntimeException trực tiếp (bao gồm timeout, response rỗng, ...)
            throw ex;
        } catch (Exception ex) {
            log.error("[eKYC] Không thể kết nối AI Service: {}", ex.getMessage());
            throw new RuntimeException("Không thể kết nối tới dịch vụ xác minh AI. Vui lòng thử lại sau.", ex);
        }
    }

    /**
     * Xử lý khi xác minh thành công:
     * <ol>
     *   <li>Kiểm tra trùng số CCCD (HMAC-SHA256 fingerprint) với các tài khoản khác</li>
     *   <li>Cập nhật EkycProfile → Verified, lưu AES ciphertext + HMAC hash</li>
     *   <li>Upsert FaceEmbedding (512 chiều) vào DB</li>
     *   <li>Cache trạng thái "Verified" vào Redis (TTL 7 ngày)</li>
     * </ol>
     */
    private EkycResponse handleVerificationSuccess(User user, EkycProfile profile,
                                                   String idCardNumber,
                                                   String fullName,
                                                   String dateOfBirth,
                                                   List<Double> embedding,
                                                   boolean isUpdateFlow) {
        log.info("[eKYC] Xác minh thành công cho userId={}", user.getId());
        Instant now = Instant.now();

        // ─ 1. Tính HMAC-SHA256 fingerprint (deterministic, không thể reverse) ────────
        String idCardNumberHash = aesEncryptionService.hmac(idCardNumber);

        // ─ 2. Kiểm tra trùng số CCCD với các tài khoản đã Verified khác ─────────
        boolean isDuplicate;
        if (isUpdateFlow) {
            isDuplicate = ekycProfileRepository.existsByIdcardnumberhashAndStatusAndUseridNot(idCardNumberHash, "Verified", user);
        } else {
            isDuplicate = ekycProfileRepository.existsByIdcardnumberhashAndStatus(idCardNumberHash, "Verified");
        }

        if (isDuplicate) {
            log.warn("[eKYC] Phát hiện trùng số CCCD (hash={}) cho userId={}",
                    idCardNumberHash.substring(0, 8) + "...", user.getId());

            // Đánh dấu Rejected vì số CCCD đã thuộc tài khoản khác (dùng new tx)
            String reason = isUpdateFlow
                    ? "Số CCCD này đã được đăng ký xác minh với một tài khoản khác trong hệ thống"
                    : "Số CCCD này đã được đăng ký bởi một tài khoản khác";
            self.markAsRejectedNewTx(profile.getId(), reason, now);

            if (isUpdateFlow) {
                throw new RuntimeException(
                        "This identification number has already been registered. Please verify the information and try again."
                );
            } else {
                throw new RuntimeException(
                        "This identification number is already linked to another account."
                );
            }
        }

        // ─ 3. Cập nhật EkycProfile → Verified ────────────────────────────────
        ekycProfileRepository.markAsVerified(profile.getId(), now, now, VERIFICATION_METHOD);

        // ─ 4. Mã hóa Số CCCD, Họ tên, Ngày sinh bằng AES-256-GCM ────────────────────────────
        String encryptedIdCardNumber = aesEncryptionService.encrypt(idCardNumber);
        String encryptedFullName = fullName != null ? aesEncryptionService.encrypt(fullName) : null;
        String encryptedDob = dateOfBirth != null ? aesEncryptionService.encrypt(dateOfBirth) : null;

        // ─ 5. Lưu cả ciphertext lẫn HMAC hash trong 1 query ──────────────────
        ekycProfileRepository.updateIdCardDetailsAndHash(
                profile.getId(), encryptedIdCardNumber, idCardNumberHash, encryptedFullName, encryptedDob);
        log.info("[eKYC] Đã lưu AES ciphertext + HMAC hash cho userId={}, ekycId={}",
                user.getId(), profile.getId());


        // ─ 6. Chuyển embedding → vector PostgreSQL và lưu FaceEmbedding ─────
        String embeddingVector = convertToPostgresVectorString(embedding);

        // Upsert FaceEmbedding (tạo mới hoặc cập nhật nếu user verify lại)
        faceembeddingRepository.upsertEmbedding(user.getId(), embeddingVector, now);
        log.info("[eKYC] Đã lưu FaceEmbedding cho userId={}", user.getId());

        // Lưu trạng thái "Verified" vào Redis Cache
        cacheEkycStatus(user.getId(), "Verified");
        log.info("[eKYC] Đã cache trạng thái Verified vào Redis cho userId={}", user.getId());

        String successMessage = isUpdateFlow
                ? "eKYC documents and facial data have been updated successfully. Your face data is now ready for Face Recognition Check-in."
                : "eKYC documents and facial data have been registered successfully. Your face data is now ready for Face Recognition Check-in.";

        return EkycResponse.builder()
                .status("Verified")
                .message(successMessage)
                .recognizedIdNumber(idCardNumber)
                .build();
    }

    /**
     * Chuyển List&lt;Double&gt; thành chuỗi định dạng vector PostgreSQL.
     * Ví dụ: [0.1, -0.2, 0.35] → "[0.10000000,-0.20000000,0.35000000]"
     *
     * @param embedding Mảng 512 giá trị double từ AI Service
     * @return Chuỗi vector PostgreSQL
     * @throws RuntimeException nếu embedding null hoặc rỗng
     */
    private String convertToPostgresVectorString(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new RuntimeException("AI Service trả về embedding rỗng");
        }
        if (embedding.size() != 512) {
            log.warn("[eKYC] Kích thước embedding không đúng: expected=512, actual={}", embedding.size());
        }

        String vectorContent = embedding.stream()
                .map(d -> String.format("%.8f", d))
                .collect(Collectors.joining(","));

        return "[" + vectorContent + "]";
    }

    /**
     * Kiểm tra trạng thái Verified trong Redis Cache.
     *
     * <p><b>Quan trọng:</b> Redis KHÔNG tham gia Spring @Transactional.
     * Nếu DB bị rollback nhưng Redis đã ghi, cache sẽ bị "stale".
     * Method này double-check với DB khi cache nói "Verified" để tránh
     * block user vĩnh viễn khi DB thực sự chưa có dữ liệu.
     */
    private boolean isAlreadyVerifiedInCache(Integer userId) {
        Object cached = redisTemplate.opsForValue().get(EKYC_CACHE_PREFIX + userId);
        if (!"Verified".equals(cached)) {
            return false;
        }

        // Cache nói Verified → cross-check với DB để tránh stale cache
        User userRef = new User();
        userRef.setId(userId);
        boolean verifiedInDb = ekycProfileRepository.existsByUseridAndStatus(userRef, "Verified");

        if (!verifiedInDb) {
            // Cache lỗi thời (DB bị rollback trước đó) → xóa để user có thể retry
            log.warn("[eKYC] Phát hiện stale cache cho userId={}: Redis=Verified nhưng DB không có bản ghi. Xóa cache.", userId);
            redisTemplate.delete(EKYC_CACHE_PREFIX + userId);
            return false;
        }

        return true;
    }

    /**
     * Lưu trạng thái eKYC vào Redis với TTL 7 ngày.
     * Key: "user:ekyc:{userId}", Value: "Verified"
     */
    private void cacheEkycStatus(Integer userId, String status) {
        redisTemplate.opsForValue().set(
                EKYC_CACHE_PREFIX + userId,
                status,
                EKYC_CACHE_TTL
        );
    }

    /**
     * Cập nhật trạng thái Rejected trong một transaction hoàn toàn mới (để không bị rollback cùng main transaction).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsRejectedNewTx(Integer ekycId, String reason, Instant updatedAt) {
        log.info("[eKYC] Đánh dấu Rejected cho ekycId={} với lý do: {}", ekycId, reason);
        ekycProfileRepository.markAsRejected(ekycId, reason, updatedAt);
    }

    /**
     * Loại bỏ dấu tiếng Việt để so khớp tên.
     */
    private String removeDiacritics(String str) {
        if (str == null) {
            return "";
        }
        String nfdNormalizedString = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String withoutDiacritics = pattern.matcher(nfdNormalizedString).replaceAll("");
        return withoutDiacritics.replace("Đ", "D").replace("đ", "d");
    }
}
