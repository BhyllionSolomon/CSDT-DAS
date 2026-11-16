package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.ResultStatsRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A read-only health check of the live setup, for the H.O.D and admin. It reports whether each setting is
 * present, never its value, and it changes nothing.
 */
@Service
@Transactional(readOnly = true)
public class SystemCheckService {

    public record Check(String key, String status, String title, String detail) {}

    public record Count(String label, long value) {}

    public record Report(int ok, int warn, int fail, List<Check> checks, List<Count> sessions, List<Count> totals) {}

    private static final Pattern YEAR = Pattern.compile("^\\s*(\\d{4})");

    @PersistenceContext
    private EntityManager em;

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AcademicSessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final ResultStatsRepository resultRepository;
    private final RegistrationClearanceService clearanceService;

    public SystemCheckService(
            StudentRepository studentRepository,
            UserRepository userRepository,
            AcademicSessionRepository sessionRepository,
            CourseRepository courseRepository,
            ResultStatsRepository resultRepository,
            RegistrationClearanceService clearanceService
    ) {
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.courseRepository = courseRepository;
        this.resultRepository = resultRepository;
        this.clearanceService = clearanceService;
    }

    public Report run(String username) {
        User me = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));
        String role = String.valueOf(me.getRole()).toUpperCase();
        if (!role.equals("HOD") && !role.equals("ADMIN")) throw new IllegalArgumentException("Only the H.O.D or an admin can run the system check.");

        List<Check> checks = new ArrayList<>();

        // ---- settings (present or missing only; values are never shown)
        if (blank("APP_MAIL_API_KEY") || blank("APP_MAIL_FROM_EMAIL")) {
            checks.add(fail("mail", "Emailed codes are not set up",
                    "Sign-up and forgot-password codes cannot be sent. In Render, add APP_MAIL_API_KEY, APP_MAIL_FROM_EMAIL and APP_MAIL_FROM_NAME from your Brevo account."));
        } else {
            checks.add(ok("mail", "Emailed codes are set up", "Sign-up and forgot-password codes can be emailed."));
        }

        String jwt = System.getenv("JWT_SECRET");
        if (jwt == null || jwt.isBlank()) {
            checks.add(fail("jwt", "The login signing key is missing", "Add JWT_SECRET in Render: a long random text of at least 32 characters."));
        } else if (jwt.length() < 32) {
            checks.add(warn("jwt", "The login signing key is short", "Use at least 32 random characters for JWT_SECRET."));
        } else {
            checks.add(ok("jwt", "The login signing key is set", "It is long enough."));
        }

        if (!blank("APP_BOOTSTRAP_ADMIN_PASSWORD") || !blank("APP_BOOTSTRAP_HOD_PASSWORD")) {
            checks.add(warn("bootstrap", "Setup passwords are still in Render",
                    "Once you and the H.O.D. have signed in and chosen your own passwords, delete APP_BOOTSTRAP_ADMIN_PASSWORD and APP_BOOTSTRAP_HOD_PASSWORD from Render."));
        } else {
            checks.add(ok("bootstrap", "No setup passwords are stored", "The two setup password settings are not present."));
        }

        String front = System.getenv("FRONTEND_URL");
        if (front == null || front.isBlank()) {
            checks.add(warn("frontend", "The website address is not set", "Set FRONTEND_URL in Render to your live website address."));
        } else if (front.contains("localhost") || front.contains("127.0.0.1")) {
            checks.add(warn("frontend", "The website address points to a computer, not the live site",
                    "FRONTEND_URL is set to a local address. Change it to https://csdt-das.onrender.com."));
        } else {
            checks.add(ok("frontend", "The website address is set", "It points to the live site."));
        }

        // ---- reference data
        long programmes = count("SELECT COUNT(p) FROM Programme p");
        List<String> levelCodes = em.createQuery("SELECT l.code FROM Level l", String.class).getResultList();
        if (programmes == 0 || levelCodes.isEmpty()) {
            checks.add(fail("reference", "Programmes or levels are missing", "Programme Overview and uploads need them. " + programmes + " programme(s), " + levelCodes.size() + " level(s)."));
        } else {
            boolean odd = levelCodes.stream().anyMatch(c -> c == null || !c.matches("\\d{3}"));
            checks.add(odd
                    ? warn("levelcodes", "Level codes are not plain numbers", "Found: " + levelCodes.stream().sorted().collect(Collectors.joining(", "))
                                                                              + ". Pages that filter by level expect 100, 200, 300 and 400.")
                    : ok("levelcodes", "Level codes look right", "Found: " + levelCodes.stream().sorted().collect(Collectors.joining(", ")) + "."));
        }

        // ---- sessions
        List<AcademicSession> sessions = sessionRepository.findAll().stream()
                .sorted(Comparator.comparing(AcademicSession::getStartDate, Comparator.nullsLast(Comparator.naturalOrder()))).toList();

        if (sessions.isEmpty()) {
            checks.add(fail("sessions", "No academic session exists", "Create the current session first."));
        } else {
            List<String> bad = new ArrayList<>();
            for (AcademicSession s : sessions) {
                Matcher m = YEAR.matcher(s.getName() == null ? "" : s.getName());
                if (s.getStartDate() == null) bad.add(s.getName() + " has no start date");
                else if (m.find() && Integer.parseInt(m.group(1)) != s.getStartDate().getYear()) bad.add(s.getName() + " starts " + s.getStartDate());
            }
            checks.add(bad.isEmpty()
                    ? ok("sessions", "Session dates match their names", sessions.size() + " session(s).")
                    : warn("sessions", "Some session dates look wrong", String.join("; ", bad) + ". Correct them on the Academic Sessions page."));

            checks.add(clearanceService.currentSession() == null
                    ? warn("current", "No current session is marked", "Students cannot register until one session is the current session.")
                    : ok("current", "A current session is set", clearanceService.currentSession().getName() + "."));
        }

        // ---- students
        List<Student> students = studentRepository.findAll();
        Map<String, Long> bySession = new LinkedHashMap<>();
        long noSession = 0, noLevel = 0, noProgramme = 0;
        for (Student s : students) {
            if (s.getAcademicSession() == null) noSession++;
            else bySession.merge(s.getAcademicSession().getName(), 1L, Long::sum);
            if (s.getLevel() == null) noLevel++;
            if (s.getProgramme() == null) noProgramme++;
        }

        if (students.isEmpty()) {
            checks.add(warn("students", "No students have been added yet", "Upload a roster on the Bulk Upload Students page."));
        } else if (noSession + noLevel + noProgramme > 0) {
            checks.add(warn("students", "Some students are incomplete",
                    noSession + " without a session, " + noLevel + " without a level, " + noProgramme + " without a programme. Fix them on Students Per Session."));
        } else {
            checks.add(ok("students", "Every student has a session, level and programme", students.size() + " student(s)."));
        }

        // ---- accounts
        List<User> users = userRepository.findAll();
        boolean hod = users.stream().anyMatch(u -> "HOD".equalsIgnoreCase(String.valueOf(u.getRole())) && Boolean.TRUE.equals(u.getActive()));
        boolean admin = users.stream().anyMatch(u -> "ADMIN".equalsIgnoreCase(String.valueOf(u.getRole())) && Boolean.TRUE.equals(u.getActive()));
        checks.add(hod && admin
                ? ok("owners", "There is an active H.O.D. and an active admin", "Both accounts can sign in.")
                : fail("owners", "The H.O.D. or admin account is missing or inactive", "Without them nobody can approve staff or manage the system."));

        long waiting = users.stream().filter(u -> Set.of("LECTURER", "ADJUNCT").contains(String.valueOf(u.getRole()).toUpperCase())
                && !Boolean.TRUE.equals(u.getActive())).count();
        if (waiting > 0) checks.add(warn("staff", waiting + " staff account(s) are waiting to be activated", "Activate them on the Manage Lecturers page."));

        long unlinked = users.stream().filter(u -> "STUDENT".equalsIgnoreCase(String.valueOf(u.getRole())) && u.getStudent() == null).count();
        if (unlinked > 0) checks.add(warn("unlinked", unlinked + " student account(s) are not linked to a student record", "Link them on the Registration Approvals page."));

        long pending = resultRepository.countPending();
        if (pending > 0) checks.add(warn("results", pending + " result(s) are waiting for verification", "Review them on the Result Verification page."));

        checks.add(ok("backup", "Backups", "Download the key lists (students, results, allocations) regularly. Neon keeps a limited history only."));

        List<Count> sessionCounts = new ArrayList<>();
        bySession.forEach((name, n) -> sessionCounts.add(new Count(name, n)));
        if (noSession > 0) sessionCounts.add(new Count("(no session)", noSession));
        sessionCounts.sort(Comparator.comparing(Count::label).reversed());

        List<Count> totals = List.of(
                new Count("Students", students.size()), new Count("Courses", courseRepository.count()),
                new Count("Sessions", sessions.size()), new Count("Accounts", users.size()));

        int ok = (int) checks.stream().filter(c -> c.status().equals("ok")).count();
        int warn = (int) checks.stream().filter(c -> c.status().equals("warn")).count();
        int fail = (int) checks.stream().filter(c -> c.status().equals("fail")).count();

        checks.sort(Comparator.comparingInt(c -> c.status().equals("fail") ? 0 : c.status().equals("warn") ? 1 : 2));
        return new Report(ok, warn, fail, checks, sessionCounts, totals);
    }

    // ---------------------------------------------------------------- helpers

    private boolean blank(String name) {
        String v = System.getenv(name);
        return v == null || v.isBlank();
    }

    private long count(String jpql) {
        return em.createQuery(jpql, Long.class).getSingleResult();
    }

    private Check ok(String key, String title, String detail) { return new Check(key, "ok", title, detail); }
    private Check warn(String key, String title, String detail) { return new Check(key, "warn", title, detail); }
    private Check fail(String key, String title, String detail) { return new Check(key, "fail", title, detail); }
}