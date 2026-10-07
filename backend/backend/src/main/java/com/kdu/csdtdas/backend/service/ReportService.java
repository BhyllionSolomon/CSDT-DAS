package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CourseResultDTO;
import com.kdu.csdtdas.backend.dto.CumulativeSummaryDTO;
import com.kdu.csdtdas.backend.dto.ReportDTOs;
import com.kdu.csdtdas.backend.dto.SemesterResultDTO;
import com.kdu.csdtdas.backend.dto.SemesterSummaryDTO;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.StudentLevelHistory;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public class ReportService {

    private final StudentLevelHistoryService historyService;
    private final ResultCalculationService calculationService;
    private final CourseRepository courseRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final ProgrammeRepository programmeRepository;
    private final LevelRepository levelRepository;
    private final AcademicSessionRepository sessionRepository;

    public ReportService(
            StudentLevelHistoryService historyService,
            ResultCalculationService calculationService,
            CourseRepository courseRepository,
            CourseRegistrationRepository registrationRepository,
            ProgrammeRepository programmeRepository,
            LevelRepository levelRepository,
            AcademicSessionRepository sessionRepository
    ) {
        this.historyService = historyService;
        this.calculationService = calculationService;
        this.courseRepository = courseRepository;
        this.registrationRepository = registrationRepository;
        this.programmeRepository = programmeRepository;
        this.levelRepository = levelRepository;
        this.sessionRepository = sessionRepository;
    }

    public ReportDTOs.Broadsheet broadsheet(Long programmeId, Long levelId, Long sessionId, String semester) {

        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Level not found."));
        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        int semesterNumber = "SECOND".equalsIgnoreCase(semester) ? 2 : 1;
        String cleanSemester = semesterNumber == 2 ? "SECOND" : "FIRST";

        Set<String> codes = new TreeSet<>();
        Map<String, Integer> unitFromResults = new HashMap<>();
        List<ReportDTOs.BroadsheetRow> rows = new ArrayList<>();
        List<ReportDTOs.StudentRef> without = new ArrayList<>();
        int no = 0;

        for (Student student : cohortStudents(sessionId, programmeId, levelId)) {

            List<SemesterResultDTO> history;
            try {
                history = calculationService.calculateFullAcademicHistory(student.getId());
            } catch (IllegalArgumentException e) {
                without.add(ref(student, "No approved results on record."));
                continue;
            }

            int idx = indexOf(history, sessionId, semesterNumber);
            if (idx < 0) {
                without.add(ref(student, "No approved results for this semester."));
                continue;
            }

            SemesterResultDTO entry = history.get(idx);

            int prevTup = 0;
            int prevTuf = 0;
            for (int i = 0; i < idx; i++) {
                prevTup += history.get(i).semesterSummary().tup();
                prevTuf += history.get(i).semesterSummary().tuf();
            }

            Map<String, BigDecimal> scores = new LinkedHashMap<>();
            for (CourseResultDTO c : entry.courses()) {
                scores.put(c.courseCode(), c.score());
                unitFromResults.put(c.courseCode(), c.creditUnit());
            }

            // Registered this semester but no approved score: shown as "-" and flagged in the remark.
            List<String> missing = new ArrayList<>();
            for (CourseRegistration reg : registrationRepository
                    .findByStudentIdAndAcademicSessionId(student.getId(), sessionId)) {

                if (!cleanSemester.equalsIgnoreCase(reg.getSemester())) continue;
                if (!"REGISTERED".equalsIgnoreCase(reg.getStatus())) continue;

                String code = reg.getCourse().getCode();
                if (!scores.containsKey(code)) {
                    missing.add(code);
                    unitFromResults.putIfAbsent(code, reg.getCourse().getCreditUnit());
                }
            }

            codes.addAll(scores.keySet());
            codes.addAll(missing);

            SemesterSummaryDTO cur = entry.semesterSummary();
            CumulativeSummaryDTO prev = entry.previousCumulative();

            ReportDTOs.Totals previous = prev.ctu() == 0
                    ? null
                    : new ReportDTOs.Totals(prev.ctu(), prevTup, prevTuf, prev.cumulativeTwgp(), prev.cgpa());

            ReportDTOs.Totals current = new ReportDTOs.Totals(
                    cur.tuo(), cur.tup(), cur.tuf(), cur.twgp(), cur.gpa()
            );

            String remark = entry.remark();
            if (!missing.isEmpty()) {
                String joined = String.join(", ", missing);
                remark = remark.contains(" - ") ? remark + ", " + joined : remark + " - " + joined;
            }

            rows.add(new ReportDTOs.BroadsheetRow(
                    ++no,
                    student.getId(),
                    student.getFullName(),
                    student.getMatricNumber(),
                    scores,
                    previous,
                    current,
                    entry.currentCumulative().cgpa(),
                    entry.currentCumulative().degreeClass(),
                    remark
            ));
        }

        List<ReportDTOs.CourseColumn> columns = new ArrayList<>();
        for (String code : codes) {
            Optional<Course> course = courseRepository.findByCode(code);
            int unit = course.map(Course::getCreditUnit).orElse(unitFromResults.getOrDefault(code, 0));
            String status = course.map(Course::getStatus).orElse("C");
            columns.add(new ReportDTOs.CourseColumn(code, unit, status));
        }

        return new ReportDTOs.Broadsheet(
                programme.getName(), level.getName(), session.getName(), cleanSemester,
                columns, rows, without
        );
    }

    public ReportDTOs.Standing standing(Long programmeId, Long levelId, Long sessionId) {

        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Level not found."));
        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        List<ReportDTOs.StandingRow> rows = new ArrayList<>();
        List<ReportDTOs.StudentRef> without = new ArrayList<>();

        for (Student student : cohortStudents(sessionId, programmeId, levelId)) {

            List<SemesterResultDTO> history;
            try {
                history = calculationService.calculateFullAcademicHistory(student.getId());
            } catch (IllegalArgumentException e) {
                without.add(ref(student, "No approved results on record."));
                continue;
            }

            // Standing as at the latest approved semester of the chosen session.
            SemesterResultDTO last = null;
            for (SemesterResultDTO h : history) {
                if (h.sessionId().equals(sessionId)) last = h;
            }

            if (last == null) {
                without.add(ref(student, "No approved results in this session."));
                continue;
            }

            String remark = last.remark();
            String standing = remark.startsWith("GS") ? "GS" : "NGS";

            List<String> outstanding = new ArrayList<>();
            int dash = remark.indexOf(" - ");
            if (dash >= 0) {
                for (String code : remark.substring(dash + 3).split(",")) {
                    if (!code.isBlank()) outstanding.add(code.trim());
                }
            }

            rows.add(new ReportDTOs.StandingRow(
                    student.getId(),
                    student.getMatricNumber(),
                    student.getFullName(),
                    last.currentCumulative().cgpa(),
                    last.currentCumulative().degreeClass(),
                    standing,
                    outstanding,
                    last.semester()
            ));
        }

        return new ReportDTOs.Standing(programme.getName(), level.getName(), session.getName(), rows, without);
    }

    private List<Student> cohortStudents(Long sessionId, Long programmeId, Long levelId) {
        return historyService.cohort(sessionId, programmeId, levelId).stream()
                .map(StudentLevelHistory::getStudent)
                .sorted(Comparator.comparing((Student s) -> s.getFullName().toLowerCase()))
                .toList();
    }

    private int indexOf(List<SemesterResultDTO> history, Long sessionId, int semesterNumber) {
        for (int i = 0; i < history.size(); i++) {
            SemesterResultDTO h = history.get(i);
            if (h.sessionId().equals(sessionId) && h.semester() == semesterNumber) return i;
        }
        return -1;
    }

    private ReportDTOs.StudentRef ref(Student student, String note) {
        return new ReportDTOs.StudentRef(student.getId(), student.getMatricNumber(), student.getFullName(), note);
    }
}