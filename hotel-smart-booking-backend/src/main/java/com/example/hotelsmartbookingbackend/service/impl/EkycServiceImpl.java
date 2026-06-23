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
import com.example.hotelsmartbookingbackend.repository.FaceembeddingRepository;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.EkycService;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.transaction.annotation.Propagation;
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
 *
 * <p>Luồng hoạt động mới:
 * <ol>
 *   <li>Client gửi hai ảnh CCCD và năm frame khuôn mặt – <b>không cần nhập số CCCD</b>.</li>
 *   <li>Tạo bản ghi EkycProfile tạm với trạng thái "Pending".</li>
 *   <li>Gọi Python AI Service:
 *     <ul>
 *       <li>YOLOv11 phát hiện vùng số CCCD, họ tên và ngày sinh.</li>
 *       <li>VietOCR đọc tiếng Việt trong từng vùng đã cắt.</li>
 *       <li>Face API kiểm tra liveness và tạo face template 512 chiều từ năm góc.</li>
 *     </ul>
 *   </li>
 *   <li>Nếu OCR không đọc được số CCCD → báo lỗi ngay cho client.</li>
 *   <li>Nếu hợp lệ → lưu Số CCCD vào EkycProfile (Verified) và embedding vào FaceEmbeddings.</li>
 * </ol>
 *
 * <p>Tính toàn vẹn: @Transactional bảo vệ ghi vào 2 bảng trong cùng một transaction.
 * Redis được cập nhật sau khi commit thành công.
 */
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

    @Value("${ai.service.face-enroll-url:http://localhost:8000/api/v1/face/enroll}")
    private String aiFaceEnrollUrl;

    @Value("${ai.service.face-frame-validation-url:http://localhost:8000/api/v1/face/validate-frame}")
    private String aiFaceFrameValidationUrl;

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final String EKYC_CACHE_PREFIX = "user:ekyc:";
    private static final Duration EKYC_CACHE_TTL = Duration.ofDays(7);
    private static final String VERIFICATION_METHOD = "Auto";
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
        String frontUrl   = uploadImage(frontImage,   "ekyc_front");
        String backUrl    = uploadImage(backImage,    "ekyc_back");
        String selfieUrl  = uploadImage(selfieImage,  "ekyc_selfie");

        EkycRequest request = EkycRequest.builder()
                .frontImageUrl(frontUrl)
                .backImageUrl(backUrl)
                .faceImageUrl(selfieUrl)
                .build();

        // ── 1. Kiểm tra user tồn tại ─────────────────────────────────────────
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email));

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
                throw new RuntimeException("Tài khoản của bạn đã được xác minh eKYC trước đó.");
            } else {
                // Cả "Pending" (kẹt do lỗi trước) lẫn "Rejected" đều cho phép thử lại (isUpdateFlow).
                // Điều này tránh user bị block vĩnh viễn khi server crash giữa chừng ở lần trước.
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
        } else {
            profile = createPendingProfile(user, request);
            ekycProfileRepository.save(profile);
        }

        // ── 4. OCR CCCD trước; chỉ tiếp tục đăng ký khuôn mặt khi CCCD hợp lệ ──
        AiEkycResponse ocrResponse;
        try {
            ocrResponse = callAiService(request);
        } catch (Exception ex) {
            String rejectionReason = "CCCD OCR failed: " + ex.getMessage();
            self.markAsRejectedNewTx(profile.getId(), rejectionReason, Instant.now());
            throw new RuntimeException(
                    "Không thể đọc thông tin CCCD. Hãy chụp đúng mặt trước, mặt sau, "
                            + "đặt thẻ trong khung và bảo đảm ảnh rõ nét.",
                    ex
            );
        }

        String idCardNumber = blankToNull(ocrResponse.getIdCardNumber());
        String fullName = blankToNull(ocrResponse.getFullName());
        String dateOfBirth = blankToNull(ocrResponse.getDateOfBirth());

        if (idCardNumber == null) {
            String rejectionReason = "CCCD OCR did not return an identification number";
            self.markAsRejectedNewTx(profile.getId(), rejectionReason, Instant.now());
            throw new RuntimeException(
                    "Không nhận diện được số CCCD trên ảnh mặt trước. "
                            + "Vui lòng chụp lại mặt trước CCCD rõ nét và đầy đủ bốn góc."
            );
        }

        // ── 5. CCCD hợp lệ mới kiểm tra liveness và tạo face template ─────────
        // Không so sánh selfie với chân dung trên CCCD.
        AiFaceEnrollmentResponse faceResponse;
        try {
            faceResponse = callFaceEnrollmentService(
                    selfieImage,
                    leftImage,
                    rightImage,
                    upImage,
                    downImage
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
            // Ưu tiên trả message từ AI service (đã chi tiết) thay vì wrap chung chung.
            // ex.getMessage() đã là detail từ Python Face API (active liveness, passive liveness, v.v.)
            String aiDetail = ex.getMessage();
            String rejectionReason = "Face enrollment failed: " + aiDetail;
            self.markAsRejectedNewTx(profile.getId(), rejectionReason, Instant.now());
            // Nếu message đã đủ cụ thể (từ AI), dùng nguyên; nếu không, fallback generic.
            boolean isAiDetail = aiDetail != null && !aiDetail.startsWith("Face API") && !aiDetail.startsWith("Không thể kết nối");
            throw new RuntimeException(
                    isAiDetail
                            ? aiDetail
                            : "Đăng ký khuôn mặt thất bại. Hãy thực hiện rõ đủ năm tư thế, "
                            + "bảo đảm chỉ có một người và không dùng ảnh hoặc màn hình.",
                    ex
            );
        }

        // ── 6. Lưu thông tin CCCD và embedding FaceID ──────────────────────────
        return handleVerificationSuccess(
                user,
                profile,
                idCardNumber,
                fullName,
                dateOfBirth,
                faceResponse.getEmbedding(),
                isUpdateFlow
        );
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
                // Trả về null thay vì crash – frontend sẽ hiển thị "N/A"
            }
        }

        String decryptedFullName = null;
        if (profile.getFullname() != null) {
            try {
                decryptedFullName = aesEncryptionService.decrypt(profile.getFullname());
            } catch (Exception ex) {
            }
        }

        String decryptedDob = null;
        if (profile.getDateofbirth() != null) {
            try {
                decryptedDob = aesEncryptionService.decrypt(profile.getDateofbirth());
            } catch (Exception ex) {
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
        bodyBuilder.part("step", normalizedStep)
                .contentType(MediaType.TEXT_PLAIN);
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
                    detail != null
                            ? detail
                            : "Face validation API lỗi: " + ex.getResponseBodyAsString(),
                    ex
            );
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(
                    "Không thể kết nối tới dịch vụ kiểm tra tư thế khuôn mặt.",
                    ex
            );
        }
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

    private String extractFastApiDetail(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        Matcher matcher = FASTAPI_DETAIL_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1)
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\\\", "\\");
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
     * AI Service dùng YOLOv11 + VietOCR để đọc thông tin mặt trước CCCD.
     *
     * <p>Payload gửi lên: { front_image_url, back_image_url }
     *
     * @throws RuntimeException nếu AI service không phản hồi hoặc trả về lỗi HTTP
     */
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
                    .block(); // Block vì đang trong @Transactional context

            if (response == null) {
                throw new RuntimeException("AI Service trả về phản hồi rỗng");
            }

            return response;

        } catch (WebClientResponseException ex) {
            throw new RuntimeException("AI Service lỗi: " + ex.getResponseBodyAsString(), ex);
        } catch (RuntimeException ex) {
            // Rethrow RuntimeException trực tiếp (bao gồm timeout, response rỗng, ...)
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Không thể kết nối tới dịch vụ xác minh AI. Vui lòng thử lại sau.", ex);
        }
    }

    /**
     * Gọi Face API bằng multipart/form-data để kiểm tra liveness và tạo face template nhiều góc.
     * Ảnh CCCD không được gửi tới Face API và không được dùng để so sánh khuôn mặt.
     */
    private AiFaceEnrollmentResponse callFaceEnrollmentService(
            MultipartFile selfieImage,
            MultipartFile leftImage,
            MultipartFile rightImage,
            MultipartFile upImage,
            MultipartFile downImage
    ) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        addImagePart(bodyBuilder, "selfie_image", selfieImage);
        addImagePart(bodyBuilder, "left_image", leftImage);
        addImagePart(bodyBuilder, "right_image", rightImage);
        addImagePart(bodyBuilder, "up_image", upImage);
        addImagePart(bodyBuilder, "down_image", downImage);

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

    private void addImagePart(
            MultipartBodyBuilder bodyBuilder,
            String partName,
            MultipartFile file
    ) {
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
        Instant now = Instant.now();

        if (idCardNumber == null || idCardNumber.isBlank()) {
            throw new RuntimeException("Không thể xác minh eKYC khi chưa nhận diện được số CCCD.");
        }

        String idCardNumberHash = aesEncryptionService.hmac(idCardNumber);
        boolean isDuplicate = isUpdateFlow
                ? ekycProfileRepository.existsByIdcardnumberhashAndStatusAndUseridNot(
                        idCardNumberHash, "Verified", user)
                : ekycProfileRepository.existsByIdcardnumberhashAndStatus(
                        idCardNumberHash, "Verified");

        if (isDuplicate) {
            String reason = "Số CCCD này đã được đăng ký bởi một tài khoản khác";
            self.markAsRejectedNewTx(profile.getId(), reason, now);
            throw new RuntimeException(
                    "Số CCCD này đã được liên kết với một tài khoản khác."
            );
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


        // ─ 6. Chuyển embedding → vector PostgreSQL và lưu FaceEmbedding ─────
        String embeddingVector = convertToPostgresVectorString(embedding);

        // Upsert FaceEmbedding (tạo mới hoặc cập nhật nếu user verify lại)
        faceembeddingRepository.upsertEmbedding(user.getId(), embeddingVector, now);

        // Lưu trạng thái "Verified" vào Redis Cache
        cacheEkycStatus(user.getId(), "Verified");

        String successMessage = isUpdateFlow
                ? "CCCD đã được xác minh và FaceID đã được cập nhật thành công."
                : "CCCD đã được xác minh và FaceID đã được đăng ký thành công cho check-in.";

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
        ekycProfileRepository.markAsRejected(ekycId, reason, updatedAt);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
