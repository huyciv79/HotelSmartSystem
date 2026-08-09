package com.example.hotelsmartbookingbackend.controller;

import com.example.hotelsmartbookingbackend.dto.response.EkycDocumentContent;
import com.example.hotelsmartbookingbackend.service.EkycService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin eKYC documents", description = "Stream tai lieu eKYC qua backend")
@SecurityRequirement(name = "bearerAuth")
public class AdminEkycDocumentController {

    private final EkycService ekycService;

    @GetMapping("/users/{userId}/ekyc-documents/{type}")
    @Operation(summary = "Stream tai lieu eKYC", description = "Backend kiem tra quyen va stream binary tu bucket private")
    public ResponseEntity<byte[]> getEkycDocument(
            @PathVariable Integer userId,
            @PathVariable String type,
            Principal principal
    ) {
        Optional<EkycDocumentContent> document = ekycService.getEkycDocument(userId, type, principal.getName());
        if (document.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EkycDocumentContent content = document.get();
        return ResponseEntity.ok()
                .contentType(content.contentType())
                .contentLength(content.bytes().length)
                .cacheControl(CacheControl.noStore().mustRevalidate().cachePrivate().sMaxAge(0, TimeUnit.SECONDS))
                .header("Content-Disposition", "inline")
                .header("X-Content-Type-Options", "nosniff")
                .body(content.bytes());
    }
}
