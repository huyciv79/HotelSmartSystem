package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.EkycRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;
import com.example.hotelsmartbookingbackend.service.EkycService;
import java.security.Principal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller xử lý các endpoint eKYC.
 * Tất cả endpoint đều yêu cầu xác thực JWT.
 */
@RestController
@RequestMapping("/api/v1/ekyc")
@RequiredArgsConstructor
@Tag(name = "eKYC", description = "API xác minh danh tính điện tử")
@SecurityRequirement(name = "bearerAuth")
public class EkycController {

    private final EkycService ekycService;

    /**
     * Khởi tạo quá trình xác minh eKYC – Tự động hóa hoàn toàn.
     *
     * <p>Client chỉ cần gửi multipart/form-data gồm 3 ảnh:
     * <ul>
     *   <li>{@code frontImage}  – ảnh mặt trước CCCD</li>
     *   <li>{@code backImage}   – ảnh mặt sau CCCD</li>
     *   <li>{@code selfieImage} – ảnh selfie khuôn mặt</li>
     * </ul>
     * Khuôn mặt trên CCCD được so khớp với selfie để kích hoạt FaceID.
     * OCR chỉ bổ sung thông tin và không làm quy trình thất bại nếu chưa đọc được.
     */
    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Xác minh eKYC bằng khuôn mặt",
            description = "So khớp khuôn mặt trên CCCD với selfie và đăng ký FaceID"
    )
    public ResponseEntity<ApiResponse<EkycResponse>> verifyEkyc(
            @RequestPart("frontImage")  MultipartFile frontImage,
            @RequestPart("backImage")   MultipartFile backImage,
            @RequestPart("selfieImage") MultipartFile selfieImage,
            Principal principal) {

        String email = principal.getName();
        EkycResponse result = ekycService.processEkyc(email, frontImage, backImage, selfieImage);
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
            @RequestPart("selfieImage") MultipartFile selfieImage,
            Principal principal) {

        String email = principal.getName();
        EkycResponse result = ekycService.processEkyc(email, frontImage, backImage, selfieImage);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật eKYC hoàn tất", result));
    }

    /**
     * Lấy trạng thái eKYC của user đang đăng nhập.
     *
     * @return Trạng thái: "Pending", "Verified", "Rejected", hoặc "NotFound"
     */
    @GetMapping("/status")
    @Operation(summary = "Kiểm tra trạng thái eKYC")
    public ResponseEntity<ApiResponse<EkycStatusResponse>> getEkycStatus(Principal principal) {
        String email = principal.getName();
        EkycStatusResponse status = ekycService.getEkycStatus(email);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái eKYC", status));
    }

}
