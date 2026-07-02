package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.response.AiFaceFrameValidationResponse;
import com.example.hotelsmartbookingbackend.dto.response.ApiResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;
import com.example.hotelsmartbookingbackend.service.EkycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/ekyc")
@RequiredArgsConstructor
@Tag(name = "eKYC", description = "API xac minh danh tinh dien tu")
@SecurityRequirement(name = "bearerAuth")
public class EkycController {

    private final EkycService ekycService;

    @PostMapping(
            value = "/face/validate-frame",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Kiem tra realtime tung tu the liveness",
            description = "Chi chuyen sang buoc tiep theo khi frame hien tai dat yeu cau tu the va khuon mat."
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
        return ResponseEntity.ok(ApiResponse.success("Ket qua kiem tra frame", result));
    }

    @PostMapping(value = "/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Dang ky eKYC va FaceID",
            description = "OCR va xac minh thong tin CCCD, sau do dang ky khuon mat selfie lam du lieu FaceID cho check-in."
    )
    public ResponseEntity<ApiResponse<EkycResponse>> verifyEkyc(
            @RequestPart("frontImage") MultipartFile frontImage,
            @RequestPart("backImage") MultipartFile backImage,
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
        return ResponseEntity.ok(ApiResponse.success("Xu ly eKYC hoan tat", result));
    }

    @GetMapping("/status")
    @Operation(summary = "Kiem tra trang thai eKYC")
    public ResponseEntity<ApiResponse<EkycStatusResponse>> getEkycStatus(Principal principal) {
        String email = principal.getName();
        EkycStatusResponse status = ekycService.getEkycStatus(email);
        return ResponseEntity.ok(ApiResponse.success("Trang thai eKYC", status));
    }
}
