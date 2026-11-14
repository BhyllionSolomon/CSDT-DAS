package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CourseResultDTO;
import com.kdu.csdtdas.backend.dto.CumulativeSummaryDTO;
import com.kdu.csdtdas.backend.dto.ProgressDTOs;
import com.kdu.csdtdas.backend.dto.SemesterResultDTO;
import com.kdu.csdtdas.backend.dto.SemesterSummaryDTO;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StudentProgressService {

    private static final double FIRST = 4.50;
    private static final double UPPER = 3.50;
    private static final double LOWER = 2.40;
    private static final double THIRD = 1.50;
    private static final List<String> BAND_LABELS = List.of("70 and above", "60–69", "50–59", "45–49", "40–44", "Below 40");

    private final ResultCalculationService calculationService;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public StudentProgressService(
            ResultCalculationService calculationService,
            StudentRepository studentRepository,
            UserRepository userRepository
    ) {
        this.calculationService = calculationService;
        this.studentRepository = studentRepository;
        this.userRepository = userRepository;
    }

    public ProgressDTOs.Progress progress(String username, Long studentId) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        Student student = studentRepository.findById(studentId).orElseThrow(() -> bad("Student not found."));
        authorize(user, student);

        List<SemesterResultDTO> history = calculationService.calculateFullAcademicHistory(studentId);

        List<ProgressDTOs.SemesterPoint> points = new ArrayList<>();
        List<ProgressDTOs.Highlight> all = new ArrayList<>();
        int[] bands = new int[6];

        for (SemesterResultDTO h : history) {
            int sem = h.semester() == null ? 1 : h.semester();
            List<ProgressDTOs.CourseLine> lines = new ArrayList<>();

            for (CourseResultDTO c : h.courses()) {
                Double score = dbl(c.score());
                lines.add(new ProgressDTOs.CourseLine(
                        c.courseCode(), c.courseTitle(), c.creditUnit() == null ? 0 : c.creditUnit(),
                        score, c.letterGrade(), c.passed()));

                if (score != null) {
                    bands[score >= 70 ? 0 : score >= 60 ? 1 : score >= 50 ? 2 : score >= 45 ? 3 : score >= 40 ? 4 : 5]++;
                    all.add(new ProgressDTOs.Highlight(c.courseCode(), c.courseTitle(), h.session(), sem, score));
                }
            }

            SemesterSummaryDTO s = h.semesterSummary();
            CumulativeSummaryDTO cum = h.currentCumulative();

            points.add(new ProgressDTOs.SemesterPoint(
                    h.session(), sem, h.session() + (sem == 1 ? " · First semester" : " · Second semester"),
                    s == null ? null : dbl(s.gpa()),
                    cum == null ? null : dbl(cum.cgpa()),
                    cum == null ? null : cum.degreeClass(),
                    s == null || s.tuo() == null ? 0 : s.tuo(),
                    s == null || s.tup() == null ? 0 : s.tup(),
                    s == null || s.tuf() == null ? 0 : s.tuf(),
                    lines
            ));
        }

        Double cgpa = null;
        String degreeClass = null;
        Integer totalUnits = null;
        if (!history.isEmpty()) {
            CumulativeSummaryDTO last = history.get(history.size() - 1).currentCumulative();
            if (last != null) {
                cgpa = dbl(last.cgpa());
                degreeClass = last.degreeClass();
                totalUnits = last.ctu();
            }
        }

        String trend = null;
        List<Double> gpas = new ArrayList<>();
        for (ProgressDTOs.SemesterPoint p : points) if (p.gpa() != null) gpas.add(p.gpa());
        if (gpas.size() >= 2) {
            double diff = gpas.get(gpas.size() - 1) - gpas.get(gpas.size() - 2);
            trend = diff > 0.1 ? "IMPROVING" : diff < -0.1 ? "DECLINING" : "STEADY";
        }

        Double nextAt = null;
        String nextName = null;
        Double gap = null;
        if (cgpa != null) {
            if (cgpa < THIRD) { nextAt = THIRD; nextName = "Third Class"; }
            else if (cgpa < LOWER) { nextAt = LOWER; nextName = "Second Class Lower"; }
            else if (cgpa < UPPER) { nextAt = UPPER; nextName = "Second Class Upper"; }
            else if (cgpa < FIRST) { nextAt = FIRST; nextName = "First Class"; }
            if (nextAt != null) gap = Math.round((nextAt - cgpa) * 100.0) / 100.0;
        }

        all.sort(Comparator.comparing(ProgressDTOs.Highlight::score, Comparator.reverseOrder()));
        int top = Math.min(3, all.size());
        List<ProgressDTOs.Highlight> strongest = new ArrayList<>(all.subList(0, top));
        int bottom = Math.min(3, Math.max(0, all.size() - top));
        List<ProgressDTOs.Highlight> weakest = new ArrayList<>();
        for (int i = all.size() - 1; i >= all.size() - bottom; i--) weakest.add(all.get(i));

        List<String> carryCodes = new ArrayList<>();
        for (Course c : calculationService.getOutstandingCourses(studentId)) carryCodes.add(c.getCode());
        carryCodes.sort(String::compareTo);

        List<Integer> bandList = new ArrayList<>();
        for (int b : bands) bandList.add(b);

        return new ProgressDTOs.Progress(
                student.getFullName(), student.getMatricNumber(),
                student.getProgramme() != null ? student.getProgramme().getName() : "",
                student.getLevel() != null ? student.getLevel().getName() : "",
                cgpa, degreeClass, totalUnits, carryCodes.size(), carryCodes,
                trend, nextAt, nextName, gap, points, strongest, weakest, bandList, BAND_LABELS,
                "Only approved results are included. The class boundaries are the department's CGPA cut-offs: 4.50, 3.50, 2.40 and 1.50."
        );
    }

    // ---------------------------------------------------------------- helpers

    /** A student sees only their own progress; the H.O.D any student's; a Level Adviser students in their own programme. */
    private void authorize(User user, Student student) {
        String role = String.valueOf(user.getRole()).toUpperCase();

        if (role.equals("STUDENT")) {
            if (user.getStudent() == null || !user.getStudent().getId().equals(student.getId())) {
                throw bad("You can only see your own progress.");
            }
            return;
        }
        if (role.equals("HOD") || role.equals("ADMIN")) return;

        if (role.equals("LEVEL_ADVISER")) {
            boolean sameProgramme = user.getProgramme() != null && student.getProgramme() != null
                    && user.getProgramme().getId().equals(student.getProgramme().getId());
            boolean sameLevel = user.getLevel() == null
                    || (student.getLevel() != null && user.getLevel().getId().equals(student.getLevel().getId()));
            if (sameProgramme && sameLevel) return;
            throw bad("This student is outside your programme and level.");
        }

        throw bad("You do not have permission to see this.");
    }

    private Double dbl(BigDecimal v) {
        return v == null ? null : Math.round(v.doubleValue() * 100.0) / 100.0;
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}