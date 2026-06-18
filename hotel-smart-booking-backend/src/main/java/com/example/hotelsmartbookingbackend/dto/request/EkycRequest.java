package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO cho quá trình xác minh eKYC tự động.
 *
 * <p>Client chỉ cần gửi 3 URL ảnh (đã upload lên Supabase Storage).
 * Hệ thống sẽ tự động đọc Số CCCD từ ảnh mặt trước bằng OCR –
 * người dùng <b>không cần nhập số CCCD bằng tay</b>.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EkycRequest {

    @NotBlank(message = "URL ảnh mặt trước không được để trống")
    @Size(max = 512, message = "URL ảnh mặt trước không được vượt quá 512 ký tự")
    private String frontImageUrl;

    @NotBlank(message = "URL ảnh mặt sau không được để trống")
    @Size(max = 512, message = "URL ảnh mặt sau không được vượt quá 512 ký tự")
    private String backImageUrl;

    @NotBlank(message = "URL ảnh khuôn mặt không được để trống")
    @Size(max = 512, message = "URL ảnh khuôn mặt không được vượt quá 512 ký tự")
    private String faceImageUrl;
}
