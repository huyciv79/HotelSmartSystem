package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.request.EkycRequest;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceFrameValidationResponse;
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

    @PostMapping(
            value = "/face/validate-frame",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Kiểm tra realtime từng tư thế liveness",
            description = "Chỉ chuyển sang bước tiếp theo khi frame hiện tại đạt yêu cầu tư thế và khuôn mặt."
    )
    public ResponseEntity<ApiResponse<AiFaceFrameValidationResponse>> validateLivenessFrame(
            @RequestPart("frameImage") MultipartFile frameImage,
            @RequestPart(value = "referenceImage", required = false) MultipartFile referenceImage,
            @RequestPart(value = "oppositeImage", required = false) MultipartFile oppositeImage,
            @RequestPart("step") String step) {
        AiFaceFrameValidationResponse result = ekycService.validateLivenessFrame(
                frameImage,
                referenceImage,
                oppositeImage,
                step
        );
        return ResponseEntity.ok(ApiResponse.success("Kết quả kiểm tra frame", result));
    }

    /**
     * Khởi tạo quá trình xác minh eKYC – Tự động hóa hoàn toàn.
     *
     * <p>Client gửi hai ảnh CCCD và năm frame liveness:
     * <ul>
     *   <li>{@code frontImage}  – ảnh mặt trước CCCD</li>
     *   <li>{@code backImage}   – ảnh mặt sau CCCD</li>
     *   <li>{@code selfieImage} – ảnh selfie khuôn mặt</li>
     *   <li>{@code leftImage}, {@code rightImage}, {@code upImage}, {@code downImage}
     *       – các tư thế active liveness</li>
     * </ul>
     * Ảnh CCCD được dùng cho hồ sơ và OCR. Ảnh selfie được dùng để đăng ký FaceID;
     * không so sánh khuôn mặt selfie với chân dung trên CCCD ở bước này.
     */
    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Đăng ký eKYC và FaceID",
            description = "Bước 1 OCR và xác minh thông tin CCCD; bước 2 chỉ khi CCCD hợp lệ mới đăng ký khuôn mặt selfie làm dữ liệu FaceID cho check-in"
    )
    public ResponseEntity<ApiResponse<EkycResponse>> verifyEkyc(
            @RequestPart("frontImage")  MultipartFile frontImage,
            @RequestPart("backImage")   MultipartFile backImage,
            @RequestPart("selfieImage") MultipartFile selfieImage,
            @RequestPart("leftImage") MultipartFile leftImage,
            @RequestPart("rightImage") MultipartFile rightImage,
            @RequestPart("upImage") MultipartFile upImage,
            @RequestPart("downImage") MultipartFile downImage,
            Principal principal) {

        String email = principal.getName();
        EkycResponse result = ekycService.processEkyc(
                email,
                frontImage,
                backImage,
                selfieImage,
                leftImage,
                rightImage,
                upImage,
                downImage
        );
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
            @RequestPart("leftImage") MultipartFile leftImage,
            @RequestPart("rightImage") MultipartFile rightImage,
            @RequestPart("upImage") MultipartFile upImage,
            @RequestPart("downImage") MultipartFile downImage,
            Principal principal) {

        String email = principal.getName();
        EkycResponse result = ekycService.processEkyc(
                email,
                frontImage,
                backImage,
                selfieImage,
                leftImage,
                rightImage,
                upImage,
                downImage
        );
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
