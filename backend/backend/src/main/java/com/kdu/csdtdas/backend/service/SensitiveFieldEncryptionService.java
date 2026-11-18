package com.kdu.csdtdas.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SensitiveFieldEncryptionService {
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SensitiveFieldEncryptionService(@Value("${app.data-encryption-key:}") String encodedKey) {
        SecretKeySpec parsed = null;
        if (encodedKey != null && !encodedKey.isBlank()) {
            byte[] bytes = Base64.getDecoder().decode(encodedKey.trim());
            if (bytes.length != 32) throw new IllegalStateException("APP_DATA_ENCRYPTION_KEY must be a Base64-encoded 32-byte key.");
            parsed = new SecretKeySpec(bytes, "AES");
        }
        this.key = parsed;
    }

    public boolean isConfigured() { return key != null; }

    public String encrypt(String plaintext) {
        if (key == null) throw new IllegalStateException("Sensitive-field encryption is not configured.");
        try {
            byte[] nonce = new byte[NONCE_LENGTH];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(ciphertext, 0, combined, nonce.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt sensitive payment data.", e);
        }
    }

    public String decrypt(String encoded) {
        if (key == null) throw new IllegalStateException("Sensitive-field encryption is not configured.");
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            if (combined.length <= NONCE_LENGTH) throw new IllegalArgumentException("Invalid encrypted field.");
            byte[] nonce = java.util.Arrays.copyOfRange(combined, 0, NONCE_LENGTH);
            byte[] ciphertext = java.util.Arrays.copyOfRange(combined, NONCE_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt sensitive payment data. Check APP_DATA_ENCRYPTION_KEY.", e);
        }
    }
}
