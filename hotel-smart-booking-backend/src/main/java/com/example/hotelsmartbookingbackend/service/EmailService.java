package com.example.hotelsmartbookingbackend.service;

public interface EmailService {


    void sendOtpEmail(String to, String otp);

    void sendForgotPasswordOtpEmail(String to, String otp);
}
