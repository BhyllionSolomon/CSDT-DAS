package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.DirectEntryProfile;
import com.kdu.csdtdas.backend.entity.ExtraUnitGrant;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.DirectEntryProfileRepository;
import com.kdu.csdtdas.backend.repository.ExtraUnitGrantRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DirectEntryService {

    private static final Set<String> STAFF = Set.of("HOD", "ADMIN", "LEVEL_ADVISER");
    private static final int MAX_EXTRA_UNITS = 24;

    public record DirectRow(Long studentId, String matricNumber, String fullName, String programme, String level, int entryLevel) {}

    public record GrantRow(
            Long id, Long studentId, String matricNumber, String fullName,
            String semester, int units, String note, String grantedBy
    ) {}

    private final DirectEntryProfileRepository directRepository;
    private final ExtraUnitGrantRepository grantRepository;
    private final StudentRepository studentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public DirectEntryService(
            DirectEntryProfileRepository directRepository,
            ExtraUnitGrantRepository grantRepository,
            StudentRepository studentRepository,
            AcademicSessionRepository sessionRepository,
            UserRepository userRepository
    ) {
        this.directRepository = directRepository;
        this.grantRepository = grantRepository;
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<DirectRow> list(String username) {
        User user = staff(username);
        return directRepository.findAllWithStudent().stream()
                .filter(d -> inScope(user, d.getStudent()))
                .map(d -> new DirectRow(
                        d.getStudent().getId(), d.getStudent().getMatricNumber(), d.getStudent().getFullName(),
                        d.getStudent().getProgramme().getName(), d.getStudent().getLevel().getName(), d.getEntryLevelNumber()))
                .toList();
    }

    /** Entry level 200 or 300 marks a direct-entry student. Anything else removes the mark. */
    public void mark(String username, Long studentId, Integer entryLevel) {
        User user = staff(username);
        Student student = studentRepository.findById(studentId).orElseThrow(() -> bad("Student not found."));
        if (!inScope(user, student)) throw bad("This student is outside your programme and level.");

        DirectEntryProfile existing = directRepository.findByStudentId(studentId).orElse(null);

        if (entryLevel == null || entryLevel == 0) {
            if (existing != null) directRepository.delete(existing);
            return;
        }
        if (entryLevel != 200 && entryLevel != 300) throw bad("Direct-entry students enter at 200 or 300 level.");

        DirectEntryProfile p = existing != null ? existing : new DirectEntryProfile();
        p.setStudent(student);
        p.setEntryLevelNumber(entryLevel);
        p.setMarkedBy(username);
        p.setMarkedAt(LocalDateTime.now());
        directRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<GrantRow> grants(String username, Long sessionId) {
        User user = staff(username);
        return grantRepository.findForSession(sessionId).stream()
                .filter(g -> inScope(user, g.getStudent()))
                .map(g -> new GrantRow(
                        g.getId(), g.getStudent().getId(), g.getStudent().getMatricNumber(), g.getStudent().getFullName(),
                        g.getSemester(), g.getUnits(), g.getNote(), g.getGrantedBy()))
                .toList();
    }

    /** The Level Adviser (or H.O.D) approves extra units for a direct-entry student in one semester. */
    public void grant(String username, Long studentId, Long sessionId, String semester, int units, String note) {
        User user = staff(username);
        Student student = studentRepository.findById(studentId).orElseThrow(() -> bad("Student not found."));
        if (!inScope(user, student)) throw bad("This student is outside your programme and level.");
        if (directRepository.findByStudentId(studentId).isEmpty()) {
            throw bad("Only direct-entry students can be given extra units. Mark the student as direct entry first.");
        }
        if (units < 1 || units > MAX_EXTRA_UNITS) throw bad("Extra units must be between 1 and " + MAX_EXTRA_UNITS + ".");

        String sem = semester == null ? "" : semester.trim().toUpperCase();
        if (!sem.equals("FIRST") && !sem.equals("SECOND")) throw bad("Semester must be FIRST or SECOND.");

        AcademicSession session = sessionRepository.findById(sessionId).orElseThrow(() -> bad("Academic session not found."));

        ExtraUnitGrant g = grantRepository.findByStudentIdAndAcademicSessionIdAndSemester(studentId, sessionId, sem)
                .orElseGet(ExtraUnitGrant::new);
        g.setStudent(student);
        g.setAcademicSession(session);
        g.setSemester(sem);
        g.setUnits(units);
        g.setNote(note == null || note.isBlank() ? null : note.trim());
        g.setGrantedBy(username);
        g.setGrantedAt(LocalDateTime.now());
        grantRepository.save(g);
    }

    public void revoke(String username, Long id) {
        User user = staff(username);
        ExtraUnitGrant g = grantRepository.findById(id).orElseThrow(() -> bad("Grant not found."));
        if (!inScope(user, g.getStudent())) throw bad("This student is outside your programme and level.");
        grantRepository.delete(g);
    }

    // ---------------------------------------------------------------- helpers

    private User staff(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        if (!STAFF.contains(String.valueOf(user.getRole()).toUpperCase())) {
            throw bad("Only the H.O.D or a Level Adviser can do this.");
        }
        return user;
    }

    /** The H.O.D sees every student; a Level Adviser only their own programme (and level, when one is set). */
    private boolean inScope(User user, Student student) {
        String role = String.valueOf(user.getRole()).toUpperCase();
        if (role.equals("HOD") || role.equals("ADMIN")) return true;

        boolean sameProgramme = user.getProgramme() != null && student.getProgramme() != null
                && user.getProgramme().getId().equals(student.getProgramme().getId());
        boolean sameLevel = user.getLevel() == null
                || (student.getLevel() != null && user.getLevel().getId().equals(student.getLevel().getId()));
        return sameProgramme && sameLevel;
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}