package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.entity.VerificationCode;
import com.kdu.csdtdas.backend.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class VerificationService {

    private static final int TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_REQUESTS = 3;
    private static final int WINDOW_MINUTES = 15;
    private static final String INVALID = "That code is incorrect or has expired. Request a new one.";

    public record Issued(Long id, String code) {}

    public record Outcome(boolean ok, String message, String pendingValue) {}

    private final VerificationCodeRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    public VerificationService(
            VerificationCodeRepository repository,
            @Value("${app.verification.secret:${jwt.secret:}}") String secret
    ) {
        this.repository = repository;
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    /** Creates a new code. Earlier unused codes for the same purpose stop working. */
    @Transactional
    public Issued store(User user, String purpose, String pendingValue) {

        LocalDateTime now = LocalDateTime.now();
        repository.deleteExpired(now.minusDays(1));

        if (repository.countRecent(user.getId(), purpose, now.minusMinutes(WINDOW_MINUTES)) >= MAX_REQUESTS) {
            throw new IllegalArgumentException("Too many codes have been requested. Please wait about 15 minutes and try again.");
        }

        for (VerificationCode old : repository.findOpen(user.getId(), purpose)) {
            old.setConsumed(true);
        }

        String code = String.format("%06d", random.nextInt(1_000_000));

        VerificationCode v = new VerificationCode();
        v.setUser(user);
        v.setPurpose(purpose);
        v.setCodeHash(hash(user.getId(), purpose, code));
        v.setPendingValue(pendingValue);
        v.setExpiresAt(now.plusMinutes(TTL_MINUTES));
        VerificationCode saved = repository.save(v);

        return new Issued(saved.getId(), code);
    }

    /**
     * Checks a submitted code. A wrong code is counted and saved (this method does not throw),
     * so the five-try limit cannot be dodged by the transaction rolling back.
     */
    @Transactional
    public Outcome check(Long userId, String purpose, String submitted) {

        List<VerificationCode> open = repository.findOpen(userId, purpose);
        VerificationCode v = open.isEmpty() ? null : open.get(0);

        if (v == null || v.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new Outcome(false, INVALID, null);
        }

        if (v.getAttempts() >= MAX_ATTEMPTS) {
            v.setConsumed(true);
            return new Outcome(false, "Too many wrong attempts. Request a new code.", null);
        }

        String candidate = submitted == null ? "" : submitted.trim();
        boolean match = MessageDigest.isEqual(
                hash(userId, purpose, candidate).getBytes(StandardCharsets.UTF_8),
                v.getCodeHash().getBytes(StandardCharsets.UTF_8)
        );

        if (!match) {
            v.setAttempts(v.getAttempts() + 1);
            if (v.getAttempts() >= MAX_ATTEMPTS) v.setConsumed(true);
            return new Outcome(false, INVALID, null);
        }

        v.setConsumed(true);
        return new Outcome(true, "OK", v.getPendingValue());
    }

    @Transactional
    public void discard(Long id) {
        repository.deleteById(id);
    }

    private String hash(Long userId, String purpose, String code) {
        if (key.length < 16) {
            throw new IllegalStateException("The verification secret is not configured on this server.");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] out = mac.doFinal((userId + ":" + purpose + ":" + code).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not process the code.", e);
        }
    }
}