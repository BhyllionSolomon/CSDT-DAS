package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.ClearanceDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.AppSetting;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.entity.RegistrationClearance;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.AppSettingRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.RegistrationClearanceRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class RegistrationClearanceService {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final Pattern INVOICE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9/\\-]{5,29}$");
    private static final String GATE_KEY = "registration.gate";
    private static final String UNRECORDED_KEY = "NOTRECORDED";

    private final SecureRandom random = new SecureRandom();
    private final RegistrationClearanceRepository repository;
    private final AppSettingRepository settingRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AcademicSessionRepository sessionRepository;
    private final CourseRegistrationRepository registrationRepository;

    public RegistrationClearanceService(
            RegistrationClearanceRepository repository,
            AppSettingRepository settingRepository,
            StudentRepository studentRepository,
            UserRepository userRepository,
            AcademicSessionRepository sessionRepository,
            CourseRegistrationRepository registrationRepository
    ) {
        this.repository = repository;
        this.settingRepository = settingRepository;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.registrationRepository = registrationRepository;
    }

    // ------------------------------------------------------------ student side

    @Transactional(readOnly = true)
    public ClearanceDTOs.Status status(String username) {
        return statusFor(studentOf(username));
    }

    public ClearanceDTOs.Status submit(String username, String invoiceCode) {

        Student student = studentOf(username);
        AcademicSession session = requireSession();

        if (repository.findByStudentIdAndAcademicSessionId(student.getId(), session.getId()).isPresent()) {
            throw new IllegalArgumentException(
                    "You have already submitted your registration for " + session.getName()
                            + ". Check its status on this page."
            );
        }

        if (registeredCodes(student.getId(), session.getId()).isEmpty()) {
            throw new IllegalArgumentException("Register your courses first, then enter your invoice code.");
        }

        String code = cleanInvoice(invoiceCode);
        String key = keyOf(code);

        RegistrationClearance c = blank(student, session, code, key, "STUDENT");

        if (!repository.findByAcademicSessionIdAndInvoiceKey(session.getId(), key).isEmpty()) {
            c.setStatus("FLAGGED");
            c.setNote("This invoice code has already been used by another student.");
        }

        repository.save(c);
        return statusFor(student);
    }

    /** Used by the access gate on every request a student makes to a protected area. */
    @Transactional(readOnly = true)
    public boolean studentIsCleared(String username) {

        if (!gateEnabled()) return true;

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getStudent() == null) return true;

        AcademicSession session = currentSession();
        if (session == null) return true;

        return repository.findByStudentIdAndAcademicSessionId(user.getStudent().getId(), session.getId())
                .map(c -> "APPROVED".equals(c.getStatus()))
                .orElse(false);
    }

    // --------------------------------------------------------------- H.O.D side

    @Transactional(readOnly = true)
    public List<ClearanceDTOs.Entry> list(String statusFilter) {

        AcademicSession session = currentSession();
        if (session == null) return List.of();

        List<RegistrationClearance> rows = repository.findForSession(session.getId());
        Map<String, List<RegistrationClearance>> byInvoice = rows.stream()
                .collect(Collectors.groupingBy(RegistrationClearance::getInvoiceKey));

        boolean all = statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter);

        return rows.stream()
                .filter(c -> all || c.getStatus().equalsIgnoreCase(statusFilter))
                .map(c -> entry(c, byInvoice, session.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ClearanceDTOs.Entry> forStudent(Long studentId) {

        AcademicSession session = currentSession();
        if (session == null) return Optional.empty();

        Map<String, List<RegistrationClearance>> byInvoice = repository.findForSession(session.getId()).stream()
                .collect(Collectors.groupingBy(RegistrationClearance::getInvoiceKey));

        return repository.findByStudentIdAndAcademicSessionId(studentId, session.getId())
                .map(c -> entry(c, byInvoice, session.getId()));
    }

    public ClearanceDTOs.Entry approve(Long id, String actor) {
        RegistrationClearance c = find(id);
        c.setStatus("APPROVED");
        c.setNote(null);
        decide(c, actor);
        repository.save(c);
        return one(c);
    }

    public ClearanceDTOs.Entry reject(Long id, String note, String actor) {
        RegistrationClearance c = find(id);
        c.setStatus("REJECTED");
        c.setNote(note == null || note.isBlank() ? "The invoice code could not be verified." : note.trim());
        decide(c, actor);
        repository.save(c);
        return one(c);
    }

    /** Removes the request so the student can submit again. */
    public void reset(Long id) {
        repository.delete(find(id));
    }

    /** The student is in front of the H.O.D: record their invoice and clear them in one step. */
    public ClearanceDTOs.Entry assistedClear(Long studentId, String invoiceCode, String actor) {

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));
        AcademicSession session = requireSession();

        String code = cleanInvoice(invoiceCode);
        String key = keyOf(code);

        for (RegistrationClearance other : repository.findByAcademicSessionIdAndInvoiceKey(session.getId(), key)) {
            if (!other.getStudent().getId().equals(student.getId())) {
                throw new IllegalArgumentException(
                        "This invoice code is already recorded for " + other.getStudent().getMatricNumber()
                                + ". Check the invoice before continuing."
                );
            }
        }

        RegistrationClearance c = repository.findByStudentIdAndAcademicSessionId(student.getId(), session.getId())
                .orElseGet(() -> blank(student, session, code, key, "HOD"));

        c.setInvoiceCode(code);
        c.setInvoiceKey(key);
        c.setSource("HOD");
        c.setStatus("APPROVED");
        c.setNote("Registered in person with the H.O.D.");
        decide(c, actor);
        repository.save(c);

        return one(c);
    }

    /** Approves everything waiting, and clears every student who already registered courses this session. */
    public ClearanceDTOs.Count approveRegistered(String actor) {

        AcademicSession session = requireSession();
        int count = 0;
        Set<Long> have = new HashSet<>();

        for (RegistrationClearance c : repository.findForSession(session.getId())) {
            have.add(c.getStudent().getId());

            if ("PENDING".equals(c.getStatus())) {
                c.setStatus("APPROVED");
                decide(c, actor);
                repository.save(c);
                count++;
            }
        }

        Map<Long, Student> toClear = new LinkedHashMap<>();
        for (CourseRegistration r : registrationRepository.findAll()) {
            if (!"REGISTERED".equalsIgnoreCase(r.getStatus())) continue;
            if (!r.getAcademicSession().getId().equals(session.getId())) continue;

            Student s = r.getStudent();
            if (!have.contains(s.getId())) toClear.putIfAbsent(s.getId(), s);
        }

        for (Student s : toClear.values()) {
            RegistrationClearance c = blank(s, session, "NOT RECORDED", UNRECORDED_KEY, "ROLLOUT");
            c.setStatus("APPROVED");
            c.setNote("Cleared by the H.O.D. without an invoice code on record.");
            decide(c, actor);
            repository.save(c);
            count++;
        }

        return new ClearanceDTOs.Count(count);
    }

    // ------------------------------------------------------------------ settings

    @Transactional(readOnly = true)
    public boolean gateEnabled() {
        return settingRepository.findById(GATE_KEY)
                .map(s -> "ON".equals(s.getSettingValue()))
                .orElse(false);
    }

    public boolean setGate(boolean on) {
        AppSetting setting = settingRepository.findById(GATE_KEY).orElseGet(() -> {
            AppSetting created = new AppSetting();
            created.setSettingKey(GATE_KEY);
            return created;
        });
        setting.setSettingValue(on ? "ON" : "OFF");
        settingRepository.save(setting);
        return on;
    }

    /** The latest session whose start date has arrived. */
    @Transactional(readOnly = true)
    public AcademicSession currentSession() {

        List<AcademicSession> all = sessionRepository.findAll();
        if (all.isEmpty()) return null;

        LocalDate today = LocalDate.now();
        Comparator<AcademicSession> byStart = Comparator.comparing(
                AcademicSession::getStartDate,
                Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder())
        );

        return all.stream()
                .filter(s -> s.getStartDate() != null && !s.getStartDate().isAfter(today))
                .max(byStart)
                .orElseGet(() -> all.stream().max(byStart).orElse(null));
    }

    // ------------------------------------------------------------------ helpers

    private AcademicSession requireSession() {
        AcademicSession session = currentSession();
        if (session == null) throw new IllegalArgumentException("No academic session has been set up yet.");
        return session;
    }

    private Student studentOf(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (user.getStudent() == null) {
            throw new IllegalArgumentException("Your account is not linked to a student record.");
        }
        return user.getStudent();
    }

    private RegistrationClearance find(Long id) {
        return repository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration request not found."));
    }

    private ClearanceDTOs.Entry one(RegistrationClearance c) {
        Long sessionId = c.getAcademicSession().getId();
        Map<String, List<RegistrationClearance>> byInvoice = repository.findForSession(sessionId).stream()
                .collect(Collectors.groupingBy(RegistrationClearance::getInvoiceKey));
        return entry(c, byInvoice, sessionId);
    }

    private ClearanceDTOs.Entry entry(
            RegistrationClearance c, Map<String, List<RegistrationClearance>> byInvoice, Long sessionId
    ) {
        Student s = c.getStudent();

        List<String> shared = new ArrayList<>();
        if (!UNRECORDED_KEY.equals(c.getInvoiceKey())) {
            for (RegistrationClearance other : byInvoice.getOrDefault(c.getInvoiceKey(), List.of())) {
                if (!other.getId().equals(c.getId())) shared.add(other.getStudent().getMatricNumber());
            }
        }

        return new ClearanceDTOs.Entry(
                c.getId(), s.getId(), s.getMatricNumber(), s.getFullName(),
                s.getProgramme() != null ? s.getProgramme().getName() : null,
                s.getLevel() != null ? s.getLevel().getName() : null,
                c.getInvoiceCode(), c.getRegistrationCode(), c.getStatus(), c.getNote(), c.getSource(),
                c.getSubmittedAt(), c.getDecidedAt(), c.getDecidedBy(),
                registeredCodes(s.getId(), sessionId), shared
        );
    }

    private List<String> registeredCodes(Long studentId, Long sessionId) {
        List<String> codes = new ArrayList<>();
        for (CourseRegistration r : registrationRepository.findByStudentIdAndAcademicSessionId(studentId, sessionId)) {
            if ("REGISTERED".equalsIgnoreCase(r.getStatus())) codes.add(r.getCourse().getCode());
        }
        Collections.sort(codes);
        return codes;
    }

    private ClearanceDTOs.Status statusFor(Student student) {

        boolean gate = gateEnabled();
        AcademicSession session = currentSession();

        if (session == null) {
            return new ClearanceDTOs.Status(
                    gate, null, null, "NONE", null, null, null, true, "No academic session has been set up yet."
            );
        }

        RegistrationClearance c = repository
                .findByStudentIdAndAcademicSessionId(student.getId(), session.getId())
                .orElse(null);

        String state = c == null ? "NONE" : c.getStatus();
        boolean cleared = !gate || "APPROVED".equals(state);

        return new ClearanceDTOs.Status(
                gate, session.getId(), session.getName(), state,
                c == null ? null : c.getRegistrationCode(),
                c == null ? null : c.getInvoiceCode(),
                c == null ? null : c.getNote(),
                cleared, messageFor(state, gate)
        );
    }

    private String messageFor(String state, boolean gate) {
        return switch (state) {
            case "APPROVED" -> "Your registration has been approved by the H.O.D.";
            case "PENDING" -> "Your registration is waiting for the H.O.D's approval."
                    + (gate ? " Class materials, online classes, results and performance analytics unlock once it is approved." : "");
            case "REJECTED", "FLAGGED" -> "Your invoice code could not be accepted. Please see the H.O.D. in person to complete your course registration."
                    + (gate ? " Until then, class materials, online classes, results and performance analytics stay locked." : "");
            default -> "Register your courses, then enter the invoice code from your school fees payment to get your registration code.";
        };
    }

    private RegistrationClearance blank(
            Student student, AcademicSession session, String code, String key, String source
    ) {
        RegistrationClearance c = new RegistrationClearance();
        c.setStudent(student);
        c.setAcademicSession(session);
        c.setInvoiceCode(code);
        c.setInvoiceKey(key);
        c.setRegistrationCode(newRegistrationCode(session));
        c.setStatus("PENDING");
        c.setSource(source);
        return c;
    }

    private void decide(RegistrationClearance c, String actor) {
        c.setDecidedAt(LocalDateTime.now());
        c.setDecidedBy(actor);
    }

    private String newRegistrationCode(AcademicSession session) {

        String digits = session.getName() == null ? "" : session.getName().replaceAll("\\D", "");
        String tag = digits.length() >= 8 ? digits.substring(2, 4) + digits.substring(6, 8) : "0000";

        for (int attempt = 0; attempt < 20; attempt++) {
            StringBuilder b = new StringBuilder("REG-").append(tag).append('-');
            for (int i = 0; i < 6; i++) b.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));

            String code = b.toString();
            if (!repository.existsByRegistrationCode(code)) return code;
        }
        throw new IllegalStateException("Could not generate a registration code. Please try again.");
    }

    private String cleanInvoice(String raw) {
        String code = raw == null ? "" : raw.trim().replaceAll("\\s+", "");
        if (!INVOICE.matcher(code).matches() || keyOf(code).length() < 6) {
            throw new IllegalArgumentException(
                    "Enter the invoice code exactly as shown on your school invoice (letters, numbers and dashes, 6 to 30 characters)."
            );
        }
        return code.toUpperCase();
    }

    private String keyOf(String code) {
        return code.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}