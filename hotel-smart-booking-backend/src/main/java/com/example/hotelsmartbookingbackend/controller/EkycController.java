package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.EkycRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;
import com.example.hotelsmartbookingbackend.repository.UserRepository;
import com.example.hotelsmartbookingbackend.service.EkycService;
import com.example.hotelsmartbookingbackend.service.SupabaseStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Controller xử lý các endpoint eKYC.
 * Tất cả endpoint đều yêu cầu xác thực JWT.
 *
 * <p>Luồng:
 * <ol>
 *   <li>Client gửi multipart/form-data: 3 file ảnh (frontImage, backImage, selfieImage).</li>
 *   <li>Controller upload từng ảnh lên Supabase Storage → lấy signed URL.</li>
 *   <li>Gọi EkycService → gọi Python AI Service để OCR số CCCD và trích xuất face embedding.</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ekyc")
@RequiredArgsConstructor
@Tag(name = "eKYC", description = "API xác minh danh tính điện tử")
@SecurityRequirement(name = "bearerAuth")
public class EkycController {

    private final EkycService ekycService;
    private final UserRepository userRepository;
    private final SupabaseStorageService supabaseStorageService;

    /**
     * Khởi tạo quá trình xác minh eKYC – Tự động hóa hoàn toàn.
     *
     * <p>Client chỉ cần gửi multipart/form-data gồm 3 ảnh:
     * <ul>
     *   <li>{@code frontImage}  – ảnh mặt trước CCCD</li>
     *   <li>{@code backImage}   – ảnh mặt sau CCCD</li>
     *   <li>{@code selfieImage} – ảnh selfie khuôn mặt</li>
     * </ul>
     * Số CCCD sẽ được AI tự động đọc từ ảnh mặt trước bằng OCR.
     */
    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Xác minh eKYC", description = "Upload ảnh CCCD + selfie để xác minh danh tính")
    public ResponseEntity<ApiResponse<EkycResponse>> verifyEkyc(
            @RequestPart("frontImage")  MultipartFile frontImage,
            @RequestPart("backImage")   MultipartFile backImage,
            @RequestPart("selfieImage") MultipartFile selfieImage) throws IOException {

        Integer userId = extractUserId();
        log.info("[eKYC] Nhận yêu cầu xác minh – userId={}", userId);

        // ── Upload 3 ảnh lên Supabase → lấy public URL ───────────────────────
        String frontUrl   = uploadImage(frontImage,   "ekyc_front");
        String backUrl    = uploadImage(backImage,    "ekyc_back");
        String selfieUrl  = uploadImage(selfieImage,  "ekyc_selfie");

        log.info("[eKYC] Upload hoàn tất – front={}, back={}, selfie={}", frontUrl, backUrl, selfieUrl);

        // ── Build request và gọi service ─────────────────────────────────────
        EkycRequest request = EkycRequest.builder()
                .frontImageUrl(frontUrl)
                .backImageUrl(backUrl)
                .faceImageUrl(selfieUrl)
                .build();

        EkycResponse result = ekycService.processEkyc(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Xử lý eKYC hoàn tất", result));
    }

    /**
     * Cập nhật thông tin eKYC (UC-34).
     *
     * <p>Client gửi lên 3 ảnh mới để ghi đè hoặc thay thế hồ sơ eKYC bị từ chối.
     */
    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cập nhật eKYC", description = "Cập nhật tài liệu eKYC khi bị từ chối")
    public ResponseEntity<ApiResponse<EkycResponse>> updateEkyc(
            @RequestPart("frontImage")  MultipartFile frontImage,
            @RequestPart("backImage")   MultipartFile backImage,
            @RequestPart("selfieImage") MultipartFile selfieImage) throws IOException {

        Integer userId = extractUserId();
        log.info("[eKYC] Nhận yêu cầu cập nhật eKYC – userId={}", userId);

        // ── Upload 3 ảnh lên Supabase → lấy public URL ───────────────────────
        String frontUrl   = uploadImage(frontImage,   "ekyc_front");
        String backUrl    = uploadImage(backImage,    "ekyc_back");
        String selfieUrl  = uploadImage(selfieImage,  "ekyc_selfie");

        log.info("[eKYC] Cập nhật upload hoàn tất – front={}, back={}, selfie={}", frontUrl, backUrl, selfieUrl);

        // ── Build request và gọi service ─────────────────────────────────────
        EkycRequest request = EkycRequest.builder()
                .frontImageUrl(frontUrl)
                .backImageUrl(backUrl)
                .faceImageUrl(selfieUrl)
                .build();

        EkycResponse result = ekycService.processEkyc(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật eKYC hoàn tất", result));
    }

    /**
     * Lấy trạng thái eKYC của user đang đăng nhập.
     *
     * @return Trạng thái: "Pending", "Verified", "Rejected", hoặc "NotFound"
     */
    @GetMapping("/status")
    @Operation(summary = "Kiểm tra trạng thái eKYC")
    public ResponseEntity<ApiResponse<EkycStatusResponse>> getEkycStatus() {
        Integer userId = extractUserId();
        EkycStatusResponse status = ekycService.getEkycStatus(userId);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái eKYC", status));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Upload một ảnh lên Supabase Storage và trả về public URL.
     *
     * @param file   MultipartFile từ request
     * @param prefix Prefix tên file (vd: "ekyc_front")
     * @return Public URL của ảnh đã upload
     */
    private String uploadImage(MultipartFile file, String prefix) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File '" + prefix + "' không được để trống.");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String originalName = prefix + "_" + file.getOriginalFilename();
        return supabaseStorageService.uploadEkycDocument(file.getBytes(), originalName, contentType);
    }

    /**
     * Trích xuất userId từ SecurityContext.
     *
     * <p>JwtAuthenticationFilter đặt principal = email (String).
     * Hàm này lấy email từ SecurityContext rồi tra cứu userId từ DB.
     */
    private Integer extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Người dùng chưa xác thực.");
        }
        String email = auth.getPrincipal().toString();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user với email: " + email))
                .getId();
    }
}
