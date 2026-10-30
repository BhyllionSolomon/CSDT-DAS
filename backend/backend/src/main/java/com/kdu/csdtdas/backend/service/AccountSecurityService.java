package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.AccountDTOs;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.UserRepository;
import com.kdu.csdtdas.backend.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Self-service account changes. Deliberately not @Transactional as a whole: the wrong-code
 * counter must be saved even when the request is rejected.
 */
@Service
public class AccountSecurityService {

    private static final Logger log = LoggerFactory.getLogger(AccountSecurityService.class);

    private static final String INVALID_CODE = "That code is incorrect or has expired. Request a new one.";
    private static final Set<String> CHANGES = Set.of("NAME", "USERNAME", "EMAIL", "PHONE", "PASSWORD");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,50}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final long WINDOW_MS = 15 * 60_000L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final VerificationService verificationService;
    private final MailService mailService;
    private final Map<String, Deque<Long>> wrongPasswords = new ConcurrentHashMap<>();

    public AccountSecurityService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            VerificationService verificationService,
            MailService mailService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.verificationService = verificationService;
        this.mailService = mailService;
    }

    // ------------------------------------------------------ signed-in changes

    public AccountDTOs.Profile profile(String username) {
        User u = load(username);
        return new AccountDTOs.Profile(u.getUsername(), u.getFullName(), u.getEmail(), u.getPhoneNumber(), u.getRole());
    }

    public AccountDTOs.Message requestChange(String username, String purposeRaw, String newValue, String currentPassword) {

        User user = load(username);
        String purpose = purposeOf(purposeRaw);

        requireCurrentPassword(user, currentPassword);
        String pending = validate(user, purpose, newValue);

        // A new email proves itself by receiving the code. Everything else goes to the email on file.
        String sendTo = "EMAIL".equals(purpose) ? pending : user.getEmail();
        if (sendTo == null || sendTo.isBlank()) {
            throw new IllegalArgumentException("There is no email address on this account to send a code to.");
        }

        sendCode(user, purpose, pending, sendTo, describe(purpose));

        return new AccountDTOs.Message("A 6-digit code has been sent to " + mask(sendTo) + ". It expires in 10 minutes.");
    }

    public AccountDTOs.ChangeResult confirmChange(String username, String purposeRaw, String code, String newPassword) {

        User user = load(username);
        String purpose = purposeOf(purposeRaw);

        // Check the new password first so a weak one does not use up the code.
        if ("PASSWORD".equals(purpose)) checkNewPassword(newPassword, user);

        VerificationService.Outcome outcome = verificationService.check(user.getId(), purpose, code);
        if (!outcome.ok()) throw new IllegalArgumentException(outcome.message());

        String pending = outcome.pendingValue();
        String oldEmail = user.getEmail();
        String token = null;
        String message;

        if ("NAME".equals(purpose)) {
            user.setFullName(pending);
            message = "Your name has been updated.";

        } else if ("USERNAME".equals(purpose)) {
            if (usernameTaken(pending, user.getId())) {
                throw new IllegalArgumentException("That username has just been taken. Please choose another.");
            }
            user.setUsername(pending);
            token = jwtUtil.generateToken(pending, user.getRole());
            message = "Your username is now " + pending + ".";

        } else if ("EMAIL".equals(purpose)) {
            if (emailTaken(pending, user.getId())) {
                throw new IllegalArgumentException("That email address has just been taken. Please use another.");
            }
            user.setEmail(pending);
            message = "Your email address is now " + pending + ".";

        } else if ("PHONE".equals(purpose)) {
            user.setPhoneNumber(pending);
            message = "Your phone number has been updated.";

        } else {
            user.setPassword(passwordEncoder.encode(newPassword));
            message = "Your password has been changed.";
        }

        userRepository.save(user);

        if ("EMAIL".equals(purpose) && oldEmail != null && !oldEmail.equalsIgnoreCase(pending)) {
            notifyQuietly(oldEmail, "Your CSDT-DAS email address was changed",
                    "The email address on your CSDT-DAS account was changed to " + mask(pending)
                            + ".\n\nIf this was not you, contact the department's administrator immediately.");
        }
        if ("PASSWORD".equals(purpose)) {
            notifyQuietly(user.getEmail(), "Your CSDT-DAS password was changed",
                    "The password on your CSDT-DAS account was just changed.\n\n"
                            + "If this was not you, contact the department's administrator immediately.");
        }

        return new AccountDTOs.ChangeResult(message, token);
    }

    // --------------------------------------------------- forgotten password

    public void requestPasswordReset(String identifier) {

        User user = findByIdentifier(identifier);

        // Same reply whether or not the account exists, so this cannot be used to find accounts.
        if (user == null || !Boolean.TRUE.equals(user.getActive())
                || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }

        try {
            sendCode(user, "RESET", null, user.getEmail(), "resetting your password");
        } catch (RuntimeException e) {
            log.warn("A password reset code could not be sent for '{}': {}", user.getUsername(), e.getMessage());
        }
    }

    public void confirmPasswordReset(String identifier, String code, String newPassword) {

        User user = findByIdentifier(identifier);
        if (user == null) throw new IllegalArgumentException(INVALID_CODE);

        checkNewPassword(newPassword, user);

        VerificationService.Outcome outcome = verificationService.check(user.getId(), "RESET", code);
        if (!outcome.ok()) throw new IllegalArgumentException(outcome.message());

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        notifyQuietly(user.getEmail(), "Your CSDT-DAS password was reset",
                "The password on your CSDT-DAS account was just reset.\n\n"
                        + "If this was not you, contact the department's administrator immediately.");
    }

    // ------------------------------------------------------------- helpers

    private void sendCode(User user, String purpose, String pending, String sendTo, String reason) {

        VerificationService.Issued issued = verificationService.store(user, purpose, pending);

        try {
            mailService.send(sendTo, "Your CSDT-DAS verification code",
                    "Your CSDT-DAS verification code is " + issued.code() + ".\n\n"
                            + "It is for " + reason + ". It expires in 10 minutes and works once.\n\n"
                            + "If you did not ask for this, ignore this message and do not share the code with anyone.");
        } catch (RuntimeException e) {
            verificationService.discard(issued.id());
            String text = e.getMessage() != null && e.getMessage().contains("not set up")
                    ? e.getMessage()
                    : "We could not send the email right now. Please try again in a few minutes.";
            throw new IllegalArgumentException(text);
        }
    }

    private void requireCurrentPassword(User user, String password) {

        Deque<Long> failures = wrongPasswords.computeIfAbsent(user.getUsername().toLowerCase(), k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();

        synchronized (failures) {
            while (!failures.isEmpty() && now - failures.peekFirst() > WINDOW_MS) failures.pollFirst();

            if (failures.size() >= 5) {
                throw new IllegalArgumentException("Too many wrong passwords. Please wait about 15 minutes.");
            }
            if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
                failures.addLast(now);
                throw new IllegalArgumentException("Your current password is not correct.");
            }
            failures.clear();
        }
    }

    private String validate(User user, String purpose, String raw) {

        String v = raw == null ? "" : raw.trim();

        if ("NAME".equals(purpose)) {
            if (v.length() < 2 || v.length() > 150) throw bad("Enter your full name (2 to 150 characters).");
            return v;
        }

        if ("USERNAME".equals(purpose)) {
            if (!USERNAME.matcher(v).matches()) {
                throw bad("Use 3 to 50 letters, numbers, dots, dashes or underscores.");
            }
            if (v.equalsIgnoreCase(user.getUsername())) throw bad("That is already your username.");
            if (usernameTaken(v, user.getId())) throw bad("That username is already taken.");
            return v;
        }

        if ("EMAIL".equals(purpose)) {
            String email = v.toLowerCase();
            if (email.length() > 150 || !EMAIL.matcher(email).matches()) throw bad("Enter a valid email address.");
            if (email.equalsIgnoreCase(user.getEmail())) throw bad("That is already your email address.");
            if (emailTaken(email, user.getId())) throw bad("That email address is already used by another account.");
            return email;
        }

        if ("PHONE".equals(purpose)) {
            String phone = v.replaceAll("[\\s-]", "");
            if (!PHONE.matcher(phone).matches()) throw bad("Enter a valid phone number, e.g. 08012345678.");
            return phone;
        }

        return null; // PASSWORD: the new password is supplied only when the code is confirmed
    }

    private void checkNewPassword(String password, User user) {
        if (password == null || password.length() < 8) {
            throw bad("Choose a password of at least 8 characters.");
        }
        if (password.equalsIgnoreCase(user.getUsername())) {
            throw bad("Your password must not be the same as your username.");
        }
    }

    private boolean usernameTaken(String name, Long selfId) {
        return userRepository.findAll().stream()
                .anyMatch(u -> !u.getId().equals(selfId) && u.getUsername().equalsIgnoreCase(name));
    }

    private boolean emailTaken(String email, Long selfId) {
        return userRepository.findAll().stream()
                .anyMatch(u -> !u.getId().equals(selfId) && u.getEmail() != null && u.getEmail().equalsIgnoreCase(email));
    }

    private User findByIdentifier(String identifier) {
        String id = identifier == null ? "" : identifier.trim();
        if (id.isEmpty()) return null;

        return userRepository.findAll().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(id)
                        || (u.getStudent() != null && u.getStudent().getMatricNumber().equalsIgnoreCase(id)))
                .findFirst()
                .orElse(null);
    }

    private User load(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private String purposeOf(String raw) {
        String p = raw == null ? "" : raw.trim().toUpperCase();
        if (!CHANGES.contains(p)) throw bad("Unknown request.");
        return p;
    }

    private String describe(String purpose) {
        return switch (purpose) {
            case "NAME" -> "changing your name";
            case "USERNAME" -> "changing your username";
            case "EMAIL" -> "changing your email address";
            case "PHONE" -> "changing your phone number";
            default -> "changing your password";
        };
    }

    private String mask(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        return at < 1 ? "your email address" : email.charAt(0) + "***" + email.substring(at);
    }

    private void notifyQuietly(String to, String subject, String text) {
        if (to == null || to.isBlank()) return;
        try {
            mailService.send(to, subject, text);
        } catch (RuntimeException ignored) {
            // A failed notice must never block the change itself.
        }
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}