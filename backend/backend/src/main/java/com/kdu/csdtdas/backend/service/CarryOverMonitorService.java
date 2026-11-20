package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CarryOverMonitorService {

    public record CourseLine(String code, String title, int unit, String semester, String status) {}

    public record StudentRow(
            Long studentId, String matricNumber, String fullName, String programme, String level,
            int outstanding, int notRegisteredNow, int units, List<CourseLine> courses
    ) {}

    public record Report(
            String sessionName, String semester, int checked, int withCarryOvers, int needAttention, List<StudentRow> students
    ) {}

    private final StudentRepository studentRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final ResultCalculationService calculationService;
    private final RegistrationClearanceService clearanceService;
    private final UserRepository userRepository;

    public CarryOverMonitorService(
            StudentRepository studentRepository,
            CourseRegistrationRepository registrationRepository,
            ResultCalculationService calculationService,
            RegistrationClearanceService clearanceService,
            UserRepository userRepository
    ) {
        this.studentRepository = studentRepository;
        this.registrationRepository = registrationRepository;
        this.calculationService = calculationService;
        this.clearanceService = clearanceService;
        this.userRepository = userRepository;
    }

    public Report monitor(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));
        String role = String.valueOf(user.getRole()).toUpperCase();
        boolean hod = role.equals("HOD") || role.equals("ADMIN");
        if (!hod && !role.equals("LEVEL_ADVISER")) throw new IllegalArgumentException("Only the H.O.D or a Level Adviser can see this.");

        AcademicSession session = clearanceService.currentSession();
        LocalDate today = LocalDate.now(ZoneId.of("Africa/Lagos"));
        String sem = today.getMonthValue() >= 10 || today.getMonthValue() <= 2 ? "FIRST" : "SECOND";

        int checked = 0, needAttention = 0;
        List<StudentRow> rows = new ArrayList<>();

        for (Student s : studentRepository.findAll()) {
            if (!"ACTIVE".equalsIgnoreCase(s.getStatus())) continue;
            if (!hod && !inScope(user, s)) continue;
            checked++;

            List<Course> outstanding;
            try {
                outstanding = calculationService.getOutstandingCourses(s.getId());
            } catch (RuntimeException e) {
                continue;
            }
            if (outstanding.isEmpty()) continue;

            Set<Long> registered = session == null ? Set.of()
                    : registrationRepository.findByStudentIdAndAcademicSessionId(s.getId(), session.getId()).stream()
                      .filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus()))
                      .map(r -> r.getCourse().getId())
                      .collect(Collectors.toSet());

            List<CourseLine> lines = new ArrayList<>();
            int notNow = 0, units = 0;
            for (Course c : outstanding) {
                String status;
                if (registered.contains(c.getId())) status = "REGISTERED";
                else if (sem.equalsIgnoreCase(c.getSemester())) { status = "NOT_REGISTERED"; notNow++; }
                else status = "LATER";

                int unit = c.getCreditUnit() == null ? 0 : c.getCreditUnit();
                units += unit;
                lines.add(new CourseLine(c.getCode(), c.getTitle(), unit, c.getSemester(), status));
            }
            lines.sort(Comparator.comparing(CourseLine::code));
            if (notNow > 0) needAttention++;

            rows.add(new StudentRow(s.getId(), s.getMatricNumber(), s.getFullName(),
                    s.getProgramme() != null ? s.getProgramme().getName() : "",
                    s.getLevel() != null ? s.getLevel().getName() : "",
                    outstanding.size(), notNow, units, lines));
        }

        rows.sort(Comparator.comparingInt(StudentRow::notRegisteredNow).reversed()
                .thenComparing(Comparator.comparingInt(StudentRow::outstanding).reversed())
                .thenComparing(StudentRow::fullName));

        return new Report(session == null ? "" : session.getName(), sem, checked, rows.size(), needAttention, rows);
    }

    /** An adviser sees their own programme, and their own level when one is set. */
    private boolean inScope(User user, Student s) {
        boolean sameProgramme = user.getProgramme() != null && s.getProgramme() != null
                && user.getProgramme().getId().equals(s.getProgramme().getId());
        boolean sameLevel = user.getLevel() == null
                || (s.getLevel() != null && user.getLevel().getId().equals(s.getLevel().getId()));
        return sameProgramme && sameLevel;
    }
}