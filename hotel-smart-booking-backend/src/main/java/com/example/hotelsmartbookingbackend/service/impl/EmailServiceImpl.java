package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.service.EmailService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(HOTEL_ZONE);
    private static final int QR_CODE_SIZE = 320;

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
            throw new RuntimeException("Lỗi gửi email xác thực: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendForgotPasswordOtpEmail(String to, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Mã xác thực OTP khôi phục mật khẩu Hotel Smart Booking");
        message.setText("Chào bạn,\n\nĐây là mã OTP để khôi phục mật khẩu tài khoản của bạn: " + otp
                + "\n\nMã này sẽ hết hạn trong 5 phút.\n\nTrân trọng,\nHotel Smart Booking Team");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi gửi email xác thực khôi phục mật khẩu: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendQrCheckInEmail(
            String to,
            String customerName,
            String bookingReference,
            String roomTypeName,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String qrToken,
            Instant expiresAt
    ) {
        try {
            byte[] qrCodePng = generateQrCodePng(qrToken);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            helper.setTo(to);
            helper.setSubject("Mã QR check-in - " + bookingReference);
            helper.setText(buildQrCheckInEmailHtml(
                    customerName,
                    bookingReference,
                    roomTypeName,
                    checkInDate,
                    checkOutDate,
                    qrToken,
                    expiresAt
            ), true);
            helper.addInline("qrCheckInCode", new ByteArrayResource(qrCodePng), "image/png");

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send QR check-in email: " + e.getMessage(), e);
        }
    }

    private byte[] generateQrCodePng(String qrToken) throws Exception {
        BitMatrix bitMatrix = new QRCodeWriter()
                .encode(qrToken, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        return outputStream.toByteArray();
    }

    private String buildQrCheckInEmailHtml(
            String customerName,
            String bookingReference,
            String roomTypeName,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            String qrToken,
            Instant expiresAt
    ) {
        return """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f4f7fb;font-family:Arial,sans-serif;color:#172033;">
                  <div style="max-width:620px;margin:0 auto;padding:28px 18px;">
                    <div style="background:#ffffff;border:1px solid #dce5ef;border-radius:12px;padding:28px;">
                      <h2 style="margin:0 0 12px;color:#0f4c81;">Mã QR check-in của bạn đã sẵn sàng</h2>
                      <p style="margin:0 0 18px;line-height:1.55;">Xin chào %s,</p>
                      <p style="margin:0 0 20px;line-height:1.55;">
                        Vui lòng dùng mã QR này tại quầy lễ tân để hoàn tất check-in. Mã có hiệu lực đến
                        <strong>%s</strong> và sẽ tự hết hiệu lực sau khi check-in thành công.
                      </p>
                      <div style="text-align:center;margin:24px 0;">
                        <img src="cid:qrCheckInCode" width="260" height="260" alt="QR Code check-in" />
                      </div>
                      <table style="width:100%%;border-collapse:collapse;margin:18px 0;">
                        <tr><td style="padding:8px 0;color:#5b677a;">Mã đặt phòng</td><td style="padding:8px 0;text-align:right;"><strong>%s</strong></td></tr>
                        <tr><td style="padding:8px 0;color:#5b677a;">Loại phòng</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#5b677a;">Ngày nhận phòng</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#5b677a;">Ngày trả phòng</td><td style="padding:8px 0;text-align:right;">%s</td></tr>
                      </table>
                      <p style="margin:20px 0 8px;color:#5b677a;">Nếu ảnh QR không quét được, hãy nhập token này thủ công:</p>
                      <p style="word-break:break-all;background:#eef4fb;border-radius:8px;padding:12px;margin:0;font-family:Consolas,monospace;">%s</p>
                      <p style="margin:22px 0 0;color:#5b677a;font-size:13px;line-height:1.5;">
                        Nếu bạn không yêu cầu mã QR này, vui lòng liên hệ khách sạn.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(valueOrFallback(customerName, "quý khách")),
                escapeHtml(formatInstant(expiresAt)),
                escapeHtml(valueOrFallback(bookingReference, "-")),
                escapeHtml(valueOrFallback(roomTypeName, "-")),
                escapeHtml(formatDate(checkInDate)),
                escapeHtml(formatDate(checkOutDate)),
                escapeHtml(qrToken)
        );
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "-" : DATE_TIME_FORMAT.format(instant);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMAT.format(date);
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
