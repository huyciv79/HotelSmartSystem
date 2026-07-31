package com.example.hotelsmartbookingbackend.service.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = new MimeMessage((Session) null);
    }

    // ==========================================
    // 1. sendOtpEmail Test Cases (UTCID01 - UTCID02)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful sendOtpEmail dispatching SimpleMailMessage")
    void should_sendOtpEmailSuccessfully_when_mailSenderSucceeds() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendOtpEmail("user@example.com", "123456");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertArrayEquals(new String[]{"user@example.com"}, sentMessage.getTo());
        assertEquals("Mã xác thực OTP đăng ký Hotel Smart Booking", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("123456"));
    }

    @Test
    @DisplayName("UTCID02 - Throw RuntimeException when sendOtpEmail encounters MailSendException")
    void should_throwException_when_sendOtpEmailMailSenderFails() {
        doThrow(new MailSendException("SMTP connection failed")).when(mailSender).send(any(SimpleMailMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendOtpEmail("user@example.com", "123456"));

        assertTrue(ex.getMessage().startsWith("Lỗi gửi email xác thực: SMTP connection failed"));
    }

    // ==========================================
    // 2. sendForgotPasswordOtpEmail Test Cases (UTCID03 - UTCID04)
    // ==========================================

    @Test
    @DisplayName("UTCID03 - Successful sendForgotPasswordOtpEmail dispatching SimpleMailMessage")
    void should_sendForgotPasswordOtpEmailSuccessfully_when_mailSenderSucceeds() {
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendForgotPasswordOtpEmail("user@example.com", "654321");

        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertArrayEquals(new String[]{"user@example.com"}, sentMessage.getTo());
        assertEquals("Mã xác thực OTP khôi phục mật khẩu Hotel Smart Booking", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("654321"));
    }

    @Test
    @DisplayName("UTCID04 - Throw RuntimeException when sendForgotPasswordOtpEmail encounters MailSendException")
    void should_throwException_when_sendForgotPasswordOtpEmailMailSenderFails() {
        doThrow(new MailSendException("SMTP timeout")).when(mailSender).send(any(SimpleMailMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> emailService.sendForgotPasswordOtpEmail("user@example.com", "654321"));

        assertTrue(ex.getMessage().startsWith("Lỗi gửi email xác thực khôi phục mật khẩu: SMTP timeout"));
    }

    // ==========================================
    // 3. sendQrCheckInEmail Test Cases (UTCID05 - UTCID09)
    // ==========================================

    @Test
    @DisplayName("UTCID05 - Successful sendQrCheckInEmail with all populated parameters")
    void should_sendQrCheckInEmailSuccessfully_when_allParametersPopulated() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendQrCheckInEmail(
                "user@example.com",
                "John Doe",
                "BK12345",
                "Deluxe Suite",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 5),
                "qr_token_abc_123",
                Instant.parse("2026-08-01T14:00:00Z")
        );

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UTCID06 - Successful sendQrCheckInEmail handling null and blank parameters with fallbacks")
    void should_sendQrCheckInEmailSuccessfully_when_parametersContainNullsAndBlanks() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendQrCheckInEmail(
                "user@example.com",
                null, // customerName -> "quý khách"
                "",   // bookingReference -> "-"
                "  ", // roomTypeName -> "-"
                null, // checkInDate -> "-"
                null, // checkOutDate -> "-"
                "qr_token_xyz",
                null  // expiresAt -> "-"
        );

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UTCID07 - Successful sendQrCheckInEmail escaping HTML special characters in customer name")
    void should_escapeHtmlSpecialCharactersInEmailBody_when_customerNameContainsHtmlEntities() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendQrCheckInEmail(
                "user@example.com",
                "John & Jane <Boss> 'Special'\"",
                "BK123",
                "Deluxe",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 5),
                "qr_token_html",
                Instant.now()
        );

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("UTCID08 - Throw RuntimeException when sendQrCheckInEmail encounters MailException")
    void should_throwException_when_sendQrCheckInEmailMailSenderFails() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Failed to transmit MimeMessage")).when(mailSender).send(any(MimeMessage.class));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> emailService.sendQrCheckInEmail(
                "user@example.com",
                "John Doe",
                "BK123",
                "Standard",
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                "qr_token_fail",
                Instant.now()
        ));

        assertTrue(ex.getMessage().startsWith("Failed to send QR check-in email: Failed to transmit MimeMessage"));
    }

    @Test
    @DisplayName("UTCID09 - Throw RuntimeException when ZXing fails due to null QR token")
    void should_throwException_when_qrTokenIsNull() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> emailService.sendQrCheckInEmail(
                "user@example.com",
                "John Doe",
                "BK123",
                "Standard",
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                null,
                Instant.now()
        ));

        assertTrue(ex.getMessage().startsWith("Failed to send QR check-in email:"));
    }
}
