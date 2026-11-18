package com.kdu.csdtdas.backend.service;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class SensitiveFieldEncryptionServiceTest {
    private final String key = Base64.getEncoder().encodeToString(new byte[] {
            0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,
            16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31
    });

    @Test
    void encryptsAndDecryptsWithoutStoringPlaintext() {
        SensitiveFieldEncryptionService service = new SensitiveFieldEncryptionService(key);
        String value = "https://checkout.paystack.com/private-checkout";
        String encrypted = service.encrypt(value);
        assertNotEquals(value, encrypted);
        assertEquals(value, service.decrypt(encrypted));
    }

    @Test
    void rejectsTamperedCiphertext() {
        SensitiveFieldEncryptionService service = new SensitiveFieldEncryptionService(key);
        String encrypted = service.encrypt("sensitive-checkout-url");
        char replacement = encrypted.charAt(encrypted.length() - 1) == 'A' ? 'B' : 'A';
        String tampered = encrypted.substring(0, encrypted.length() - 1) + replacement;
        assertThrows(IllegalStateException.class, () -> service.decrypt(tampered));
    }

    @Test
    void refusesToEncryptWhenKeyIsMissing() {
        SensitiveFieldEncryptionService service = new SensitiveFieldEncryptionService("");
        assertFalse(service.isConfigured());
        assertThrows(IllegalStateException.class, () -> service.encrypt("secret"));
    }
}
