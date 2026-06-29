package com.example.hotelsmartbookingbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QrCheckInRequest {

    @JsonAlias({"qrToken", "qrCode", "qrcode", "code"})
    @NotBlank(message = "Vui long cung cap ma QR check-in")
    private String token;
}
