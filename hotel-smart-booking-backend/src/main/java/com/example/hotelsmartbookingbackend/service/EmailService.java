package com.example.hotelsmartbookingbackend.service;

import java.time.Instant;
import java.time.LocalDate;

public interface EmailService {


    void sendOtpEmail(String to, String otp);

    void sendForgotPasswordOtpEmail(String to, String otp);

    void sendQrCheckInEmail(
            String to,
            String customerName,
            String bookingReference,
            String roomTypeName,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String qrToken,
            Instant expiresAt
    );
}
