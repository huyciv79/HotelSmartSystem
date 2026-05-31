package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mã xác thực OTP đăng ký Hotel Smart Booking");
        message.setText("Chào bạn,\n\nĐây là mã OTP để xác thực tài khoản của bạn: " + otp
                + "\n\nMã này sẽ hết hạn trong 5 phút.\n\nTrân trọng,\nHotel Smart Booking Team");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            throw new RuntimeException("Lỗi gửi email xác thực: " + e.getMessage(), e);
        }
    }
}
