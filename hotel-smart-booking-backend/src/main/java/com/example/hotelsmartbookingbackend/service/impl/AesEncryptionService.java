package com.example.hotelsmartbookingbackend.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Service mã hóa / giải mã dữ liệu nhạy cảm bằng AES-256-GCM và tạo HMAC-SHA256 fingerprint.
 *
 * <h3>AES-256-GCM (Authenticated Encryption)</h3>
 * <ul>
 *   <li>Thuật toán: AES/GCM/NoPadding</li>
 *   <li>Key size: 256-bit (32 bytes), env: {@code AES_SECRET_KEY}</li>
 *   <li>IV: 96-bit sinh ngẫu nhiên bằng SecureRandom mỗi lần encrypt</li>
 *   <li>Auth tag: 128-bit – chống giả mạo / tamper</li>
 *   <li>Định dạng lưu trữ: {@code base64(iv) + ":" + base64(ciphertext || authTag)}</li>
 * </ul>
 *
 * <h3>HMAC-SHA256 (Deterministic Fingerprint)</h3>
 * <ul>
 *   <li>Deterministic: cùng input + key → cùng output → dùng được để check trùng</li>
 *   <li>Không thể reverse: biết hash không tìm ra số CCCD gốc</li>
 *   <li>Key riêng: env {@code HMAC_SECRET_KEY} – khác với AES key</li>
 *   <li>Đầu ra: chuỗi HEX 64 ký tự (256-bit)</li>
 * </ul>
 *
 * <h3>Sinh key</h3>
 * <pre>
 *   openssl rand -base64 32
 * </pre>
 */
@Service
public class AesEncryptionService {

    private static final String ALGORITHM       = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM   = "HmacSHA256";
    private static final int    IV_LENGTH_BYTES  = 12;   // 96-bit IV cho GCM
    private static final int    TAG_LENGTH_BITS  = 128;  // Authentication tag 128-bit
    private static final String DELIMITER        = ":";

    private final SecretKey aesKey;
    private final SecretKey hmacKey;

    /**
     * Khởi tạo service với AES key và HMAC key từ biến môi trường.
     *
     * @param aesBase64Key  Giá trị của {@code aes.secret-key}  (32 bytes Base64)
     * @param hmacBase64Key Giá trị của {@code hmac.secret-key} (32 bytes Base64)
     */
    public AesEncryptionService(
            @Value("${aes.secret-key}")  String aesBase64Key,
            @Value("${hmac.secret-key}") String hmacBase64Key) {

        byte[] aesBytes = Base64.getDecoder().decode(aesBase64Key);
        if (aesBytes.length != 32) {
            throw new IllegalArgumentException(
                    "AES_SECRET_KEY phải là 32 bytes (256-bit) sau khi decode Base64. " +
                    "Hiện tại: " + aesBytes.length + " bytes. " +
                    "Sinh key hợp lệ bằng: openssl rand -base64 32"
            );
        }
        this.aesKey = new SecretKeySpec(aesBytes, "AES");

        byte[] hmacBytes = Base64.getDecoder().decode(hmacBase64Key);
        if (hmacBytes.length < 16) {
            throw new IllegalArgumentException(
                    "HMAC_SECRET_KEY phải ít nhất 16 bytes. " +
                    "Hiện tại: " + hmacBytes.length + " bytes. " +
                    "Sinh key hợp lệ bằng: openssl rand -base64 32"
            );
        }
        this.hmacKey = new SecretKeySpec(hmacBytes, HMAC_ALGORITHM);
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Mã hóa chuỗi plaintext bằng AES-256-GCM.
     *
     * <p>Mỗi lần gọi sinh một IV ngẫu nhiên mới → cùng plaintext cho ra ciphertext khác nhau.
     *
     * @param plaintext Chuỗi cần mã hóa (ví dụ: số CCCD "012345678901")
     * @return Chuỗi mã hóa dạng {@code "base64iv:base64ciphertext"}, hoặc {@code null} nếu input null
     * @throws RuntimeException nếu quá trình mã hóa thất bại
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            // Sinh IV ngẫu nhiên 96-bit
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] ciphertextWithTag = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            String ivBase64         = Base64.getEncoder().encodeToString(iv);
            String ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertextWithTag);

            return ivBase64 + DELIMITER + ciphertextBase64;

        } catch (Exception ex) {
            throw new RuntimeException("Không thể mã hóa dữ liệu nhạy cảm.", ex);
        }
    }

    /**
     * Giải mã chuỗi đã được mã hóa bởi {@link #encrypt(String)}.
     *
     * @param encryptedValue Chuỗi mã hóa dạng {@code "base64iv:base64ciphertext"}
     * @return Plaintext gốc, hoặc {@code null} nếu input null/blank
     * @throws RuntimeException nếu chuỗi không đúng định dạng hoặc giải mã thất bại
     *                          (ví dụ: key sai, dữ liệu bị tamper)
     */
    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) return null;
        try {
            String[] parts = encryptedValue.split(DELIMITER, 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Định dạng ciphertext không hợp lệ – thiếu dấu ':' phân tách IV và ciphertext."
                );
            }

            byte[] iv                = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertextWithTag = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] plaintextBytes = cipher.doFinal(ciphertextWithTag);
            return new String(plaintextBytes, java.nio.charset.StandardCharsets.UTF_8);

        } catch (javax.crypto.AEADBadTagException ex) {
            throw new RuntimeException("Dữ liệu mã hóa không hợp lệ hoặc đã bị chỉnh sửa.", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể giải mã dữ liệu nhạy cảm.", ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo HMAC-SHA256 fingerprint từ plaintext dùng để kiểm tra trùng lặp.
     *
     * <p>Đặc điểm:
     * <ul>
     *   <li><b>Deterministic</b>: cùng input + key → cùng output → so sánh được trong DB</li>
     *   <li><b>Không reverseđược</b>: biết hash không suy ra được số CCCD gốc</li>
     *   <li><b>Key-dependent</b>: khác AES key, dùng {@code HMAC_SECRET_KEY} riêng</li>
     * </ul>
     *
     * @param plaintext Chuỗi cần tạo fingerprint (ví dụ: số CCCD "012345678901")
     * @return Chuỗi HEX 64 ký tự (256-bit HMAC-SHA256), hoặc {@code null} nếu input null
     * @throws RuntimeException nếu quá trình tính hash thất bại
     */
    public String hmac(String plaintext) {
        if (plaintext == null) return null;
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            byte[] hashBytes = mac.doFinal(
                    plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            return HexFormat.of().formatHex(hashBytes);  // 64 ký tự hex
        } catch (Exception ex) {
            throw new RuntimeException("Không thể tạo fingerprint dữ liệu.", ex);
        }
    }
}
