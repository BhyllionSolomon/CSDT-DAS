package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CumulativeSummaryDTO;
import com.kdu.csdtdas.backend.dto.SemesterResultDTO;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class GraduatesService {

    public record Row(
            Long studentId, String matricNumber, String fullName, String programme,
            String classOf, Double cgpa, String degreeClass, int outstanding
    ) {}

    private final StudentRepository studentRepository;
    private final AcademicSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ResultCalculationService calculationService;

    public GraduatesService(
            StudentRepository studentRepository,
            AcademicSessionRepository sessionRepository,
            UserRepository userRepository,
            ResultCalculationService calculationService
    ) {
        this.studentRepository = studentRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.calculationService = calculationService;
    }

    public List<Row> list(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (!Set.of("HOD", "ADMIN").contains(String.valueOf(user.getRole()).toUpperCase())) {
            throw new IllegalArgumentException("Only the H.O.D can see the graduates list.");
        }

        List<AcademicSession> sessions = sessionRepository.findAll().stream()
                .sorted(Comparator.comparing(AcademicSession::getStartDate)).toList();

        List<Row> rows = new ArrayList<>();
        for (Student s : studentRepository.findAll()) {
            if (!"GRADUATED".equalsIgnoreCase(s.getStatus())) continue;

            Double cgpa = null;
            String degreeClass = null;
            try {
                List<SemesterResultDTO> history = calculationService.calculateFullAcademicHistory(s.getId());
                if (!history.isEmpty()) {
                    CumulativeSummaryDTO last = history.get(history.size() - 1).currentCumulative();
                    if (last != null) {
                        cgpa = last.cgpa() == null ? null : Math.round(last.cgpa().doubleValue() * 100.0) / 100.0;
                        degreeClass = last.degreeClass();
                    }
                }
            } catch (RuntimeException ignored) {
                // no approved results for this student
            }

            int outstanding = 0;
            try {
                outstanding = calculationService.getOutstandingCourses(s.getId()).size();
            } catch (RuntimeException ignored) {
                // leave as zero
            }

            rows.add(new Row(
                    s.getId(), s.getMatricNumber(), s.getFullName(),
                    s.getProgramme() != null ? s.getProgramme().getName() : "",
                    classOf(s, sessions), cgpa, degreeClass, outstanding
            ));
        }

        rows.sort(Comparator.comparing(Row::classOf).reversed().thenComparing(Row::fullName));
        return rows;
    }

    /** The session they graduated from: the one just before the session recorded on their record. */
    private String classOf(Student s, List<AcademicSession> sessions) {
        AcademicSession at = s.getAcademicSession();
        if (at == null) return "—";

        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getId().equals(at.getId())) {
                return i > 0 ? sessions.get(i - 1).getName() : at.getName();
            }
        }
        return at.getName();
    }
}