package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.EkycRequest;
import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;

public interface EkycService {

    /**
     * Xử lý toàn bộ luồng eKYC tự động hóa cho một user.
     *
     * @param userId  ID của user cần xác minh
     * @param request 3 URL ảnh từ client (mặt trước CCCD, mặt sau CCCD, selfie)
     * @return EkycResponse chứa kết quả xác minh và số CCCD tự động đọc được
     */
    EkycResponse processEkyc(Integer userId, EkycRequest request);

    /**
     * Lấy chi tiết trạng thái eKYC của user.
     *
     * @param userId ID của user
     * @return EkycStatusResponse chứa trạng thái và thông tin hồ sơ eKYC
     */
    EkycStatusResponse getEkycStatus(Integer userId);
}
