package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.response.EkycResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycStatusResponse;
import com.example.hotelsmartbookingbackend.dto.response.AiFaceFrameValidationResponse;
import com.example.hotelsmartbookingbackend.dto.response.EkycDocumentContent;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

public interface EkycService {


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


    EkycStatusResponse getEkycStatus(String email);

    Optional<EkycDocumentContent> getEkycDocument(
            Integer userId,
            String type,
            String requesterEmail
    );

    AiFaceFrameValidationResponse validateLivenessFrame(
            MultipartFile frameImage,
            MultipartFile referenceImage,
            MultipartFile oppositeImage,
            String step
    );
}
