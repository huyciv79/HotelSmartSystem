package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.EkycRequest;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;

import org.springframework.web.multipart.MultipartFile;

public interface EkycService {

    /**
     * Xử lý toàn bộ luồng eKYC tự động hóa cho một user.
     *
     * @param email       Email của user cần xác minh
     * @param frontImage  Ảnh mặt trước CCCD
     * @param backImage   Ảnh mặt sau CCCD
     * @param selfieImage Ảnh selfie khuôn mặt
     * @return EkycResponse chứa kết quả xác minh và số CCCD tự động đọc được
     */
    EkycResponse processEkyc(String email, MultipartFile frontImage, MultipartFile backImage, MultipartFile selfieImage);

    /**
     * Lấy chi tiết trạng thái eKYC của user.
     *
     * @param email Email của user
     * @return EkycStatusResponse chứa trạng thái và thông tin hồ sơ eKYC
     */
    EkycStatusResponse getEkycStatus(String email);
}
