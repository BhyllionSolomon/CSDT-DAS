package com.kdu.csdtdas.backend.service;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class ExamCryptoServiceTest {
    private String key() {
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (i + 1);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @Test
    void encryptsAndDecryptsPaperBytes() {
        ExamCryptoService service = new ExamCryptoService(key());
        byte[] original = "Confidential examination paper".getBytes();
        ExamCryptoService.Encrypted encrypted = service.encrypt(original);
        assertFalse(java.util.Arrays.equals(original, encrypted.ciphertext()));
        assertArrayEquals(original, service.decrypt(encrypted.ciphertext(), encrypted.wrappedKey()));
    }

    @Test
    void rejectsTamperedCiphertext() {
        ExamCryptoService service = new ExamCryptoService(key());
        ExamCryptoService.Encrypted encrypted = service.encrypt("paper".getBytes());
        byte[] tampered = encrypted.ciphertext().clone();
        tampered[tampered.length - 1] ^= 1;
        assertThrows(IllegalStateException.class, () -> service.decrypt(tampered, encrypted.wrappedKey()));
    }

    @Test
    void refusesMissingMasterKey() {
        ExamCryptoService service = new ExamCryptoService("");
        assertFalse(service.configured());
        assertThrows(IllegalStateException.class, () -> service.encrypt("paper".getBytes()));
    }
}