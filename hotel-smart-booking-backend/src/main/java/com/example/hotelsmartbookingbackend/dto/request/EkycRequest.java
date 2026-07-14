package com.example.hotelsmartbookingbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


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
