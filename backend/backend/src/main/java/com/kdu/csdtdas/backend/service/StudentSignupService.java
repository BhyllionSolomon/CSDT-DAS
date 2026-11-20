package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.LoginResponse;
import com.kdu.csdtdas.backend.entity.PendingSignup;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.StudentActivationCode;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.PendingSignupRepository;
import com.kdu.csdtdas.backend.repository.StudentActivationCodeRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import com.kdu.csdtdas.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Student sign-up in two proofs: a one-time activation code the department hands out (so a matric number
 * alone is useless), and a 6-digit code emailed to the address the student typed. No account exists
 * until both are right.
 */
@Service
public class StudentSignupService {

    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int PENDING_MINUTES = 15;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int MAX_CODE_FAILS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final int MAX_SENDS = 3;
    private static final int RESEND_SECONDS = 60;

    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9._-]{3,40}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final String WRONG_CODE =
            "The matric number or activation code is not correct. Ask your Level Adviser or the H.O.D. for your activation code.";
    private static final String BAD_EMAIL_CODE = "That code is incorrect or has expired. Go back and request a new one.";

    public record Started(boolean verificationRequired, String maskedEmail, String message) {}

    public record IssuedCode(String matricNumber, String fullName, String code) {}

    public record IssueResult(int issued, int alreadyHadCode, int hasAccount, List<IssuedCode> codes) {}

    public record Status(int students, int withAccount, int withUnusedCode, int withoutCode) {}

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final StudentActivationCodeRepository codeRepository;
    private final PendingSignupRepository pendingRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mail;
    private final JwtUtil jwtUtil;
    private final SecurityAuditService audit;
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    public StudentSignupService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            StudentActivationCodeRepository codeRepository,
            PendingSignupRepository pendingRepository,
            PasswordEncoder passwordEncoder,
            MailService mail,
            JwtUtil jwtUtil,
            SecurityAuditService audit,
            @Value("${app.verification.secret:${jwt.secret:}}") String secret
    ) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.codeRepository = codeRepository;
        this.pendingRepository = pendingRepository;
        this.passwordEncoder = passwordEncoder;
        this.mail = mail;
        this.jwtUtil = jwtUtil;
        this.audit = audit;
        this.key = secret.getBytes(StandardCharsets.UTF_8);
    }

    // ============================================================ the student's side

    /** Step 1: checks the activation code and details, then emails a 6-digit code. Creates nothing but a pending sign-up. */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Started begin(String idNumber, String fullName, String email, String username, String password,
                         String activationCode, String ip) {

        LocalDateTime now = LocalDateTime.now();
        pendingRepository.deleteExpired(now);

        String matric = clean(idNumber);
        Student student = studentRepository.findByMatricNumber(matric).orElse(null);
        if (student == null || !"ACTIVE".equalsIgnoreCase(student.getStatus())) throw bad(WRONG_CODE);

        if (hasAccount(student)) {
            throw bad("An account already exists for this matric number. If you cannot sign in, use Forgot password.");
        }

        StudentActivationCode code = codeRepository.findByStudentId(student.getId()).orElse(null);
        if (code == null || code.getUsedAt() != null) throw bad(WRONG_CODE);

        if (code.getLockedUntil() != null && code.getLockedUntil().isAfter(now)) {
            throw bad("Too many wrong codes. Please wait about 15 minutes and try again.");
        }

        if (!same(hash("activation", student.getId(), normalise(activationCode)), code.getCodeHash())) {
            int fails = code.getFailedAttempts() + 1;
            if (fails >= MAX_CODE_FAILS) {
                code.setLockedUntil(now.plusMinutes(LOCK_MINUTES));
                fails = 0;
            }
            code.setFailedAttempts(fails);
            codeRepository.save(code);
            audit.record(matric, "STUDENT", ip, "SIGNUP_WRONG_CODE", "wrong activation code for " + matric);
            throw bad(WRONG_CODE);
        }

        String cleanName = fullName == null ? "" : fullName.trim();
        if (cleanName.isEmpty() || cleanName.length() > 150) throw bad("Enter your full name.");

        String cleanUser = username == null ? "" : username.trim();
        if (!USERNAME.matcher(cleanUser).matches()) {
            throw bad("Choose a username of 3 to 40 letters, numbers, dots, dashes or underscores.");
        }
        if (userRepository.existsByUsername(cleanUser) || pendingRepository.existsByUsernameAndStudentIdNot(cleanUser, student.getId())) {
            throw bad("That username is taken. Choose another.");
        }

        String cleanEmail = email == null ? "" : email.trim().toLowerCase();
        if (cleanEmail.length() > 150 || !EMAIL.matcher(cleanEmail).matches()) throw bad("Enter a valid email address.");
        if (userRepository.findAll().stream().anyMatch(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(cleanEmail))) {
            throw bad("That email address is already used by another account.");
        }

        checkPassword(password, cleanUser);

        String otp = newOtp();
        PendingSignup p = pendingRepository.findByStudentId(student.getId()).orElseGet(PendingSignup::new);
        p.setStudent(student);
        p.setUsername(cleanUser);
        p.setPasswordHash(passwordEncoder.encode(password));
        p.setFullName(cleanName);
        p.setEmail(cleanEmail);
        p.setCodeHash(hash("signup", student.getId(), otp));
        p.setAttempts(0);
        p.setSends(1);
        p.setLastSentAt(now);
        p.setExpiresAt(now.plusMinutes(PENDING_MINUTES));
        pendingRepository.save(p);

        code.setFailedAttempts(0);
        codeRepository.save(code);

        sendOtp(p, otp);
        return started(p);
    }

    /** Step 2: the emailed code is right, so the account is created and the student is signed in. */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public LoginResponse verify(String idNumber, String submitted, String ip) {

        LocalDateTime now = LocalDateTime.now();
        String matric = clean(idNumber);
        Student student = studentRepository.findByMatricNumber(matric).orElse(null);
        PendingSignup p = student == null ? null : pendingRepository.findByStudentId(student.getId()).orElse(null);

        if (p == null || p.getExpiresAt().isBefore(now)) {
            if (p != null) pendingRepository.delete(p);
            throw bad(BAD_EMAIL_CODE);
        }

        if (p.getAttempts() >= MAX_VERIFY_ATTEMPTS) {
            pendingRepository.delete(p);
            throw bad("Too many wrong attempts. Please start again.");
        }

        String candidate = submitted == null ? "" : submitted.trim();
        if (!same(hash("signup", student.getId(), candidate), p.getCodeHash())) {
            p.setAttempts(p.getAttempts() + 1);
            if (p.getAttempts() >= MAX_VERIFY_ATTEMPTS) pendingRepository.delete(p);
            else pendingRepository.save(p);
            throw bad(BAD_EMAIL_CODE);
        }

        if (hasAccount(student)) {
            pendingRepository.delete(p);
            throw bad("An account already exists for this matric number.");
        }
        if (userRepository.existsByUsername(p.getUsername())) {
            pendingRepository.delete(p);
            throw bad("That username was taken a moment ago. Please start again with another one.");
        }

        User user = new User();
        user.setUsername(p.getUsername());
        user.setPassword(p.getPasswordHash());
        user.setFullName(p.getFullName());
        user.setEmail(p.getEmail());
        user.setRole("STUDENT");
        user.setActive(true);
        user.setStudent(student);
        userRepository.save(user);

        codeRepository.findByStudentId(student.getId()).ifPresent(c -> {
            c.setUsedAt(now);
            codeRepository.save(c);
        });
        pendingRepository.delete(p);

        audit.record(user.getUsername(), "STUDENT", ip, "SIGNUP_COMPLETED", "account created for " + matric);
        return new LoginResponse(jwtUtil.generateToken(user.getUsername(), "STUDENT"), user.getUsername(), user.getFullName(), "STUDENT");
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Started resend(String idNumber) {

        LocalDateTime now = LocalDateTime.now();
        Student student = studentRepository.findByMatricNumber(clean(idNumber)).orElse(null);
        PendingSignup p = student == null ? null : pendingRepository.findByStudentId(student.getId()).orElse(null);

        if (p == null || p.getExpiresAt().isBefore(now)) {
            if (p != null) pendingRepository.delete(p);
            throw bad("This sign-up has expired. Please start again.");
        }
        if (p.getSends() >= MAX_SENDS) throw bad("The code has already been sent three times. Please wait a few minutes and start again.");
        if (p.getLastSentAt() != null && p.getLastSentAt().plusSeconds(RESEND_SECONDS).isAfter(now)) {
            throw bad("Please wait a minute before asking for another code.");
        }

        String otp = newOtp();
        p.setCodeHash(hash("signup", student.getId(), otp));
        p.setAttempts(0);
        p.setSends(p.getSends() + 1);
        p.setLastSentAt(now);
        p.setExpiresAt(now.plusMinutes(PENDING_MINUTES));
        pendingRepository.save(p);

        sendOtp(p, otp);
        return started(p);
    }

    // =================================================== the department's side

    /** Makes activation codes for the students of one programme and level who have no account yet. */
    @Transactional
    public IssueResult issue(String actorUsername, String ip, Long programmeId, Long levelId, boolean regenerate) {

        User actor = requireIssuer(actorUsername, programmeId, levelId);
        LocalDateTime now = LocalDateTime.now();

        Set<Long> linked = linkedStudentIds();
        Map<Long, StudentActivationCode> existing = new HashMap<>();
        for (StudentActivationCode c : codeRepository.findAll()) existing.put(c.getStudent().getId(), c);

        int issued = 0, had = 0, accounts = 0;
        List<IssuedCode> out = new ArrayList<>();

        for (Student s : group(programmeId, levelId)) {
            if (linked.contains(s.getId())) { accounts++; continue; }

            StudentActivationCode c = existing.get(s.getId());
            if (c != null && c.getUsedAt() == null && !regenerate) { had++; continue; }

            String plain = newActivationCode();
            if (c == null) c = new StudentActivationCode();
            c.setStudent(s);
            c.setCodeHash(hash("activation", s.getId(), plain));
            c.setIssuedBy(actor.getUsername());
            c.setIssuedAt(now);
            c.setUsedAt(null);
            c.setFailedAttempts(0);
            c.setLockedUntil(null);
            codeRepository.save(c);

            out.add(new IssuedCode(s.getMatricNumber(), s.getFullName(), plain.substring(0, 4) + "-" + plain.substring(4)));
            issued++;
        }

        audit.record(actor.getUsername(), String.valueOf(actor.getRole()).toUpperCase(), ip, "ACTIVATION_CODES_ISSUED",
                issued + " code(s) for programme " + programmeId + ", level " + levelId);
        return new IssueResult(issued, had, accounts, out);
    }

    @Transactional(readOnly = true)
    public Status status(String actorUsername, Long programmeId, Long levelId) {

        requireIssuer(actorUsername, programmeId, levelId);

        Set<Long> linked = linkedStudentIds();
        Map<Long, StudentActivationCode> existing = new HashMap<>();
        for (StudentActivationCode c : codeRepository.findAll()) existing.put(c.getStudent().getId(), c);

        int total = 0, accounts = 0, unused = 0, none = 0;
        for (Student s : group(programmeId, levelId)) {
            total++;
            if (linked.contains(s.getId())) { accounts++; continue; }
            StudentActivationCode c = existing.get(s.getId());
            if (c != null && c.getUsedAt() == null) unused++; else none++;
        }
        return new Status(total, accounts, unused, none);
    }

    // ================================================================== helpers

    private List<Student> group(Long programmeId, Long levelId) {
        return studentRepository.findAllWithDetails().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .filter(s -> s.getProgramme() != null && s.getProgramme().getId().equals(programmeId))
                .filter(s -> s.getLevel() != null && s.getLevel().getId().equals(levelId))
                .sorted(Comparator.comparing(Student::getFullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Set<Long> linkedStudentIds() {
        Set<Long> ids = new HashSet<>();
        for (User u : userRepository.findAll()) if (u.getStudent() != null) ids.add(u.getStudent().getId());
        return ids;
    }

    private boolean hasAccount(Student student) {
        return linkedStudentIds().contains(student.getId());
    }

    /** The H.O.D and admin may issue for anyone; a Level Adviser only for their own programme and level. */
    private User requireIssuer(String username, Long programmeId, Long levelId) {
        User actor = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        if (programmeId == null || levelId == null) throw bad("Choose a programme and a level.");

        String role = String.valueOf(actor.getRole()).toUpperCase();
        if (role.equals("HOD") || role.equals("ADMIN")) return actor;

        if (!role.equals("LEVEL_ADVISER") || actor.getProgramme() == null || actor.getLevel() == null) {
            throw bad("Only the H.O.D, an admin or a Level Adviser can issue activation codes.");
        }
        if (!actor.getProgramme().getId().equals(programmeId) || !actor.getLevel().getId().equals(levelId)) {
            throw bad("You can only issue codes for your own programme and level.");
        }
        return actor;
    }

    private void sendOtp(PendingSignup p, String otp) {
        try {
            mail.send(p.getEmail(), "Your CSDT-DAS verification code",
                    "Hello " + p.getFullName().trim().split("\\s+")[0] + ",\n\n"
                            + "Your CSDT-DAS verification code is: " + otp + "\n\n"
                            + "It expires in " + PENDING_MINUTES + " minutes. If you did not try to create an account, you can ignore this email.\n");
        } catch (RuntimeException e) {
            pendingRepository.delete(p);
            throw bad("We could not send the verification email right now. Please try again in a few minutes.");
        }
    }

    private Started started(PendingSignup p) {
        String masked = mask(p.getEmail());
        return new Started(true, masked, "We sent a 6-digit code to " + masked + ". Type it below. It expires in " + PENDING_MINUTES + " minutes.");
    }

    private String mask(String email) {
        int at = email.indexOf('@');
        if (at < 2) return "***" + email.substring(Math.max(at, 0));
        return email.charAt(0) + "***" + email.substring(at);
    }

    private void checkPassword(String password, String username) {
        if (password == null || password.length() < 8 || password.length() > 100) {
            throw bad("Choose a password of at least 8 characters.");
        }
        boolean letter = password.chars().anyMatch(Character::isLetter);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!letter || !digit) throw bad("Your password must contain at least one letter and one number.");
        if (password.equalsIgnoreCase(username)) throw bad("Your password cannot be the same as your username.");
    }

    private String newOtp() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String newActivationCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalise(String code) {
        return code == null ? "" : code.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private boolean same(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private String hash(String kind, Long studentId, String code) {
        if (key.length < 16) throw new IllegalStateException("The verification secret is not configured on this server.");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] out = mac.doFinal((kind + ":" + studentId + ":" + code).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not process the code.", e);
        }
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}