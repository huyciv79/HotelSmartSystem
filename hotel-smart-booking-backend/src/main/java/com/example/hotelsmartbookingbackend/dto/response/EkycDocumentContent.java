package com.example.hotelsmartbookingbackend.dto.response;

import org.springframework.http.MediaType;

public record EkycDocumentContent(byte[] bytes, MediaType contentType) {
}
