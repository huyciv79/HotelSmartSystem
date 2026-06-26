package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.EkycRequest;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceFrameValidationResponse;

import org.springframework.web.multipart.MultipartFile;

public interface EkycService {

    /**
     * Xử lý toàn bộ luồng eKYC tự động hóa cho một user.
     *
     * @param email       Email của user cần xác minh
     * @param frontImage  Ảnh mặt trước CCCD
     * @param backImage   Ảnh mặt sau CCCD
     * @param selfieImage Ảnh selfie khuôn mặt
     * Ảnh CCCD được dùng cho OCR/hồ sơ; selfie được dùng để đăng ký khuôn mặt
     * mà không so sánh với chân dung trên CCCD.
     *
     * @return EkycResponse chứa kết quả đăng ký và số CCCD tự động đọc được
     */
    EkycResponse processEkyc(
            String email,
            MultipartFile frontImage,
            MultipartFile backImage,
            MultipartFile selfieImage,
            MultipartFile leftImage,
            MultipartFile rightImage,
            MultipartFile upImage,
            MultipartFile downImage
    );

    /**
     * Lấy chi tiết trạng thái eKYC của user.
     *
     * @param email Email của user
     * @return EkycStatusResponse chứa trạng thái và thông tin hồ sơ eKYC
     */
    EkycStatusResponse getEkycStatus(String email);

    AiFaceFrameValidationResponse validateLivenessFrame(
            MultipartFile frameImage,
            MultipartFile referenceImage,
            MultipartFile oppositeImage,
            String step
    );
}
