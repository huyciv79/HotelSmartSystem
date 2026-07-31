package com.example.hotelsmartbookingbackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionServiceTest {

    // Valid 32-byte (256-bit) Base64 keys
    private static final String VALID_AES_KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String VALID_HMAC_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    // Invalid short keys
    private static final String INVALID_SHORT_AES_KEY = Base64.getEncoder().encodeToString(new byte[16]);
    private static final String INVALID_SHORT_HMAC_KEY = Base64.getEncoder().encodeToString(new byte[8]);

    private AesEncryptionService aesEncryptionService;

    @BeforeEach
    void setUp() {
        aesEncryptionService = new AesEncryptionService(VALID_AES_KEY, VALID_HMAC_KEY);
    }

    // ==========================================
    // 1. Constructor Initialization Test Cases (UTCID01 - UTCID03)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Successful service initialization when valid keys are provided")
    void should_initializeSuccessfully_when_validKeysProvided() {
        assertNotNull(aesEncryptionService);
    }

    @Test
    @DisplayName("UTCID02 - Throw exception when AES key is not 32 bytes after Base64 decode")
    void should_throwException_when_aesKeyIsNot32Bytes() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new AesEncryptionService(INVALID_SHORT_AES_KEY, VALID_HMAC_KEY));

        assertTrue(ex.getMessage().contains("AES_SECRET_KEY phải là 32 bytes (256-bit)"));
    }

    @Test
    @DisplayName("UTCID03 - Throw exception when HMAC key is less than 16 bytes after Base64 decode")
    void should_throwException_when_hmacKeyIsLessThan16Bytes() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new AesEncryptionService(VALID_AES_KEY, INVALID_SHORT_HMAC_KEY));

        assertTrue(ex.getMessage().contains("HMAC_SECRET_KEY phải ít nhất 16 bytes"));
    }

    // ==========================================
    // 2. encrypt Test Cases (UTCID04 - UTCID06, UTCID15)
    // ==========================================

    @Test
    @DisplayName("UTCID04 - Successful encryption producing formatted iv:ciphertext Base64 string")
    void should_encryptSuccessfully_when_validPlaintextProvided() {
        String plaintext = "Sensitive User Information 123";

        String encryptedValue = aesEncryptionService.encrypt(plaintext);

        assertNotNull(encryptedValue);
        assertTrue(encryptedValue.contains(":"));
        String[] parts = encryptedValue.split(":", 2);
        assertEquals(2, parts.length);
        assertDoesNotThrow(() -> Base64.getDecoder().decode(parts[0]));
        assertDoesNotThrow(() -> Base64.getDecoder().decode(parts[1]));
    }

    @Test
    @DisplayName("UTCID05 - Return null when encrypt is called with null plaintext")
    void should_returnNull_when_encryptWithNullPlaintext() {
        String encryptedValue = aesEncryptionService.encrypt(null);

        assertNull(encryptedValue);
    }

    @Test
    @DisplayName("UTCID06 - Throw RuntimeException when encryption fails due to invalid secret key field")
    void should_throwException_when_encryptEncounterCryptoException() throws Exception {
        Field aesKeyField = AesEncryptionService.class.getDeclaredField("aesKey");
        aesKeyField.setAccessible(true);
        aesKeyField.set(aesEncryptionService, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aesEncryptionService.encrypt("Sensitive Data"));

        assertEquals("Không thể mã hóa dữ liệu nhạy cảm.", ex.getMessage());
    }

    // ==========================================
    // 3. decrypt Test Cases (UTCID07 - UTCID11)
    // ==========================================

    @Test
    @DisplayName("UTCID07 - Successful decryption returning original plaintext string")
    void should_decryptSuccessfully_when_validEncryptedValueProvided() {
        String originalText = "Super Secret Password!@#123";
        String encryptedValue = aesEncryptionService.encrypt(originalText);

        String decryptedText = aesEncryptionService.decrypt(encryptedValue);

        assertEquals(originalText, decryptedText);
    }

    @Test
    @DisplayName("UTCID08 - Return null when decrypt is called with null, empty, or blank string")
    void should_returnNull_when_decryptWithNullOrBlankString() {
        assertNull(aesEncryptionService.decrypt(null));
        assertNull(aesEncryptionService.decrypt(""));
        assertNull(aesEncryptionService.decrypt("   "));
    }

    @Test
    @DisplayName("UTCID09 - Throw RuntimeException when decrypt lacks the ':' delimiter")
    void should_throwException_when_decryptMissingDelimiter() {
        String invalidEncryptedValue = "InvalidBase64WithoutColonDelimiter";

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aesEncryptionService.decrypt(invalidEncryptedValue));

        assertEquals("Không thể giải mã dữ liệu nhạy cảm.", ex.getMessage());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("Định dạng ciphertext không hợp lệ"));
    }

    @Test
    @DisplayName("UTCID10 - Throw RuntimeException when ciphertext authentication tag is tampered")
    void should_throwException_when_decryptWithTamperedCiphertext() {
        String originalText = "Authentic Secret Data";
        String validEncrypted = aesEncryptionService.encrypt(originalText);
        String[] parts = validEncrypted.split(":", 2);

        // Tamper the ciphertext bytes
        byte[] ciphertextBytes = Base64.getDecoder().decode(parts[1]);
        ciphertextBytes[0] ^= 0xFF; // flip bits
        String tamperedCiphertext = parts[0] + ":" + Base64.getEncoder().encodeToString(ciphertextBytes);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aesEncryptionService.decrypt(tamperedCiphertext));

        assertEquals("Dữ liệu mã hóa không hợp lệ hoặc đã bị chỉnh sửa.", ex.getMessage());
    }

    @Test
    @DisplayName("UTCID11 - Throw RuntimeException when decrypt encounters invalid Base64 encoding")
    void should_throwException_when_decryptWithInvalidBase64() {
        String invalidBase64 = "invalid_iv_b64!:invalid_cipher_b64!";

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aesEncryptionService.decrypt(invalidBase64));

        assertEquals("Không thể giải mã dữ liệu nhạy cảm.", ex.getMessage());
    }

    // ==========================================
    // 4. hmac Test Cases (UTCID12 - UTCID14, UTCID16)
    // ==========================================

    @Test
    @DisplayName("UTCID12 - Successful HMAC calculation returning 64-character lowercase hex string")
    void should_computeHmacSuccessfully_when_validPlaintextProvided() {
        String plaintext = "User ID or Fingerprint Data";

        String hmacHex = aesEncryptionService.hmac(plaintext);

        assertNotNull(hmacHex);
        assertEquals(64, hmacHex.length());
        assertTrue(hmacHex.matches("^[0-9a-f]{64}$"));
    }

    @Test
    @DisplayName("UTCID13 - Return null when hmac is called with null plaintext")
    void should_returnNull_when_hmacWithNullPlaintext() {
        String hmacHex = aesEncryptionService.hmac(null);

        assertNull(hmacHex);
    }

    @Test
    @DisplayName("UTCID14 - Successful HMAC deterministic check for same input")
    void should_computeSameHmac_when_sameInputProvided() {
        String input1 = "Same String Input";
        String input2 = "Same String Input";

        String hash1 = aesEncryptionService.hmac(input1);
        String hash2 = aesEncryptionService.hmac(input2);

        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("UTCID16 - Throw RuntimeException when hmac fails due to invalid secret key field")
    void should_throwException_when_hmacEncounterMacException() throws Exception {
        Field hmacKeyField = AesEncryptionService.class.getDeclaredField("hmacKey");
        hmacKeyField.setAccessible(true);
        hmacKeyField.set(aesEncryptionService, null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> aesEncryptionService.hmac("Sensitive Fingerprint Data"));

        assertEquals("Không thể tạo fingerprint dữ liệu.", ex.getMessage());
    }
}
