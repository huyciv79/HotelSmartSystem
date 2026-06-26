package com.example.hotelsmartbookingbackend.service;

import com.example.hotelsmartbookingbackend.dto.request.ForgotPasswordRequest;
import com.example.hotelsmartbookingbackend.dto.request.LoginRequest;

import com.example.hotelsmartbookingbackend.dto.request.RegisterRequest;
import com.example.hotelsmartbookingbackend.dto.request.VerifyOtpRequest;
import com.example.hotelsmartbookingbackend.dto.request.VerifyForgotOtpRequest;
import com.example.hotelsmartbookingbackend.dto.request.ResetPasswordRequest;
import com.example.hotelsmartbookingbackend.dto.request.ChangePasswordRequest;
import com.example.hotelsmartbookingbackend.dto.response.LoginResponse;

public interface AuthService {

    void registerInit(RegisterRequest request);

    void verifyRegistrationOtp(VerifyOtpRequest request);

    LoginResponse login(LoginRequest request);



    void forgotPassword(ForgotPasswordRequest request);

    String verifyForgotOtp(VerifyForgotOtpRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request);
}
