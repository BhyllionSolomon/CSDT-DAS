package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.AnalyticsDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Result;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.AnalyticsRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PerformanceAnalyticsService {

    private static final List<String> GRADES = List.of("A", "B", "C", "D", "E", "F");

    private final AnalyticsRepository analyticsRepository;
    private final AcademicSessionRepository sessionRepository;
    private final ProgrammeRepository programmeRepository;
    private final LevelRepository levelRepository;

    public PerformanceAnalyticsService(
            AnalyticsRepository analyticsRepository,
            AcademicSessionRepository sessionRepository,
            ProgrammeRepository programmeRepository,
            LevelRepository levelRepository
    ) {
        this.analyticsRepository = analyticsRepository;
        this.sessionRepository = sessionRepository;
        this.programmeRepository = programmeRepository;
        this.levelRepository = levelRepository;
    }

    public AnalyticsDTOs.Cohort cohort(Long sessionId, Long programmeId, Long levelId) {

        AcademicSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        String programmeName = "All programmes";
        if (programmeId != null) {
            programmeName = programmeRepository.findById(programmeId)
                    .orElseThrow(() -> new IllegalArgumentException("Programme not found.")).getName();
        }

        String levelName = "All levels";
        if (levelId != null) {
            levelName = levelRepository.findById(levelId)
                    .orElseThrow(() -> new IllegalArgumentException("Level not found.")).getName();
        }

        List<Result> scope = analyticsRepository.findAllApproved().stream()
                .filter(r -> programmeId == null || r.getStudent().getProgramme().getId().equals(programmeId))
                .filter(r -> levelId == null || r.getCourse().getLevel().getId().equals(levelId))
                .toList();

        List<Result> current = scope.stream()
                .filter(r -> r.getAcademicSession().getId().equals(sessionId))
                .toList();

        return new AnalyticsDTOs.Cohort(
                session.getName(), programmeName, levelName,
                summarize(current), gradeCounts(current), gpaBands(current),
                coursePerformance(current), trend(scope)
        );
    }

    // ---------------------------------------------------------------- pieces

    private AnalyticsDTOs.Summary summarize(List<Result> results) {

        if (results.isEmpty()) return new AnalyticsDTOs.Summary(0, 0, 0, 0, 0);

        Set<Long> students = results.stream().map(r -> r.getStudent().getId()).collect(Collectors.toSet());
        double average = results.stream().mapToDouble(this::score).average().orElse(0);
        long passed = results.stream().filter(r -> score(r) >= 40).count();
        double averageGpa = studentGpas(results).values().stream().mapToDouble(Double::doubleValue).average().orElse(0);

        return new AnalyticsDTOs.Summary(
                students.size(), results.size(), round(average),
                round(100.0 * passed / results.size()), round(averageGpa)
        );
    }

    private List<AnalyticsDTOs.CountItem> gradeCounts(List<Result> results) {
        Map<String, Long> counts = results.stream()
                .collect(Collectors.groupingBy(r -> grade(score(r)), Collectors.counting()));

        return GRADES.stream()
                .map(g -> new AnalyticsDTOs.CountItem(g, counts.getOrDefault(g, 0L)))
                .toList();
    }

    private List<AnalyticsDTOs.CountItem> gpaBands(List<Result> results) {
        long[] bands = new long[5];

        for (double gpa : studentGpas(results).values()) {
            if (gpa >= 4.5) bands[0]++;
            else if (gpa >= 3.5) bands[1]++;
            else if (gpa >= 2.4) bands[2]++;
            else if (gpa >= 1.5) bands[3]++;
            else bands[4]++;
        }

        return List.of(
                new AnalyticsDTOs.CountItem("First class (4.50+)", bands[0]),
                new AnalyticsDTOs.CountItem("Second upper (3.50–4.49)", bands[1]),
                new AnalyticsDTOs.CountItem("Second lower (2.40–3.49)", bands[2]),
                new AnalyticsDTOs.CountItem("Third class (1.50–2.39)", bands[3]),
                new AnalyticsDTOs.CountItem("Below 1.50", bands[4])
        );
    }

    private List<AnalyticsDTOs.CoursePerformance> coursePerformance(List<Result> results) {

        Map<Long, List<Result>> byCourse = results.stream()
                .collect(Collectors.groupingBy(r -> r.getCourse().getId()));

        List<AnalyticsDTOs.CoursePerformance> out = new ArrayList<>();

        for (List<Result> rows : byCourse.values()) {
            var course = rows.get(0).getCourse();
            long passed = rows.stream().filter(r -> score(r) >= 40).count();

            Map<String, Long> grades = new LinkedHashMap<>();
            for (String g : GRADES) grades.put(g, 0L);
            rows.forEach(r -> grades.merge(grade(score(r)), 1L, Long::sum));

            out.add(new AnalyticsDTOs.CoursePerformance(
                    course.getId(), course.getCode(), course.getTitle(), course.getLevel().getName(),
                    rows.size(),
                    round(rows.stream().mapToDouble(this::score).average().orElse(0)),
                    round(100.0 * passed / rows.size()),
                    grades
            ));
        }

        out.sort(Comparator.comparing(AnalyticsDTOs.CoursePerformance::code));
        return out;
    }

    private List<AnalyticsDTOs.TrendPoint> trend(List<Result> scope) {

        Map<Long, List<Result>> bySession = scope.stream()
                .collect(Collectors.groupingBy(r -> r.getAcademicSession().getId()));

        Comparator<List<Result>> byStart =
                Comparator.comparing((List<Result> rows) -> rows.get(0).getAcademicSession().getStartDate(),
                        Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder()));

        List<AnalyticsDTOs.TrendPoint> out = new ArrayList<>();

        for (List<Result> rows : bySession.values().stream().sorted(byStart).toList()) {
            long passed = rows.stream().filter(r -> score(r) >= 40).count();
            int students = rows.stream().map(r -> r.getStudent().getId()).collect(Collectors.toSet()).size();

            out.add(new AnalyticsDTOs.TrendPoint(
                    rows.get(0).getAcademicSession().getName(),
                    round(rows.stream().mapToDouble(this::score).average().orElse(0)),
                    round(100.0 * passed / rows.size()),
                    students
            ));
        }

        return out;
    }

    // ---------------------------------------------------------------- maths

    /** Each student's GPA over the given results: sum(units x points) / sum(units). */
    private Map<Long, Double> studentGpas(List<Result> results) {

        Map<Long, double[]> totals = new HashMap<>();

        for (Result r : results) {
            Integer creditUnit = r.getCourse().getCreditUnit();
            int units = creditUnit == null ? 0 : creditUnit;

            double[] t = totals.computeIfAbsent(r.getStudent().getId(), k -> new double[2]);
            t[0] += units * points(score(r));
            t[1] += units;
        }

        Map<Long, Double> gpas = new HashMap<>();
        totals.forEach((id, t) -> {
            if (t[1] > 0) gpas.put(id, t[0] / t[1]);
        });
        return gpas;
    }

    private double score(Result r) {
        Object value = r.getTotalScore();
        return value instanceof Number n ? n.doubleValue() : 0.0;
    }

    private String grade(double s) {
        if (s >= 70) return "A";
        if (s >= 60) return "B";
        if (s >= 50) return "C";
        if (s >= 45) return "D";
        if (s >= 40) return "E";
        return "F";
    }

    private int points(double s) {
        return switch (grade(s)) {
            case "A" -> 5;
            case "B" -> 4;
            case "C" -> 3;
            case "D" -> 2;
            case "E" -> 1;
            default -> 0;
        };
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}