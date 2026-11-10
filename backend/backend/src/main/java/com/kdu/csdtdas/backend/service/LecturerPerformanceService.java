package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.LecturerPerformanceDTOs;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.entity.Result;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.ResultStatsRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class LecturerPerformanceService {

    private static final Set<String> HOD = Set.of("HOD", "ADMIN");
    private static final Set<String> TEACHING = Set.of("LECTURER", "ADJUNCT", "LEVEL_ADVISER");
    private static final List<String> BANDS = List.of("70 and above", "60–69", "50–59", "45–49", "40–44", "Below 40");

    /** Running totals for a group of scores. */
    private static final class Tally {
        int n;
        double sum;
        int passed;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        final int[] bands = new int[6];

        void add(double s) {
            n++;
            sum += s;
            if (s >= 40) passed++;
            max = Math.max(max, s);
            min = Math.min(min, s);
            bands[s >= 70 ? 0 : s >= 60 ? 1 : s >= 50 ? 2 : s >= 45 ? 3 : s >= 40 ? 4 : 5]++;
        }

        Double average() { return n == 0 ? null : round(sum / n); }
        Double passRate() { return n == 0 ? null : round(passed * 100.0 / n); }
        Double highest() { return n == 0 ? null : round(max); }
        Double lowest() { return n == 0 ? null : round(min); }
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private final AcademicSessionRepository sessionRepository;
    private final CourseAllocationRepository allocationRepository;
    private final CourseRegistrationRepository registrationRepository;
    private final ResultStatsRepository resultRepository;
    private final UserRepository userRepository;

    public LecturerPerformanceService(
            AcademicSessionRepository sessionRepository,
            CourseAllocationRepository allocationRepository,
            CourseRegistrationRepository registrationRepository,
            ResultStatsRepository resultRepository,
            UserRepository userRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.allocationRepository = allocationRepository;
        this.registrationRepository = registrationRepository;
        this.resultRepository = resultRepository;
        this.userRepository = userRepository;
    }

    /** The H.O.D sees every lecturer; a lecturer sees only their own results. */
    public LecturerPerformanceDTOs.Report report(String username, Long sessionId, String semesterFilter) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> bad("User not found."));
        String role = String.valueOf(user.getRole()).toUpperCase();
        boolean hod = HOD.contains(role);
        if (!hod && !TEACHING.contains(role)) throw bad("You do not have permission to see this.");

        List<AcademicSession> sessions = sessionRepository.findAll().stream()
                .sorted(Comparator.comparing(AcademicSession::getStartDate)).toList();
        if (sessions.isEmpty()) throw bad("No academic session has been set up yet.");

        AcademicSession selected = sessionId == null
                ? sessions.get(sessions.size() - 1)
                : sessions.stream().filter(s -> s.getId().equals(sessionId)).findFirst()
                  .orElseThrow(() -> bad("Academic session not found."));

        String sem = semesterFilter == null || semesterFilter.isBlank() || semesterFilter.equalsIgnoreCase("ALL")
                ? null : clean(semesterFilter);

        Map<Long, Map<Long, Tally>> trend = new HashMap<>();
        Map<Long, List<LecturerPerformanceDTOs.CourseStat>> courseRows = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        Map<Long, Tally> overall = new HashMap<>();
        Map<Long, int[]> completion = new HashMap<>();
        Set<String> seen = new HashSet<>();
        Tally department = new Tally();

        for (AcademicSession s : sessions) {
            boolean isSelected = s.getId().equals(selected.getId());

            Map<String, List<Result>> byCourse = new HashMap<>();
            for (Result r : resultRepository.findApprovedForSession(s.getId())) {
                if (r.getTotalScore() == null) continue;
                String semester = semOf(r.getSemester());
                byCourse.computeIfAbsent(r.getCourse().getId() + "|" + semester, k -> new ArrayList<>()).add(r);
                if (isSelected && (sem == null || sem.equals(semester))) department.add(score(r));
            }

            for (String semester : List.of("FIRST", "SECOND")) {
                if (sem != null && !sem.equals(semester)) continue;

                for (CourseAllocation a : allocationRepository.findByAcademicSessionIdAndSemester(s.getId(), semester)) {
                    User lec = a.getLecturer();
                    if (lec == null) continue;
                    if (!hod && !lec.getId().equals(user.getId())) continue;
                    if (!seen.add(s.getId() + "|" + lec.getId() + "|" + a.getCourse().getId() + "|" + semester)) continue;

                    List<Result> list = byCourse.getOrDefault(a.getCourse().getId() + "|" + semester, List.of());

                    Tally course = new Tally();
                    Tally forTrend = trend.computeIfAbsent(lec.getId(), k -> new HashMap<>())
                            .computeIfAbsent(s.getId(), k -> new Tally());
                    for (Result r : list) {
                        course.add(score(r));
                        forTrend.add(score(r));
                    }
                    names.putIfAbsent(lec.getId(), lec.getFullName());

                    if (!isSelected) continue;

                    int registered = (int) registrationRepository
                            .findByCourseIdAndAcademicSessionIdAndSemester(a.getCourse().getId(), s.getId(), semester)
                            .stream().filter(r -> "REGISTERED".equalsIgnoreCase(r.getStatus())).count();

                    Tally total = overall.computeIfAbsent(lec.getId(), k -> new Tally());
                    for (Result r : list) total.add(score(r));

                    int[] counts = completion.computeIfAbsent(lec.getId(), k -> new int[2]);
                    counts[0] += registered;
                    counts[1] += list.size();

                    courseRows.computeIfAbsent(lec.getId(), k -> new ArrayList<>()).add(new LecturerPerformanceDTOs.CourseStat(
                            a.getCourse().getId(), a.getCourse().getCode(), a.getCourse().getTitle(),
                            a.getCourse().getCreditUnit() == null ? 0 : a.getCourse().getCreditUnit(),
                            semester, registered, list.size(),
                            registered > 0 ? (int) Math.round(list.size() * 100.0 / registered) : null,
                            course.average(), course.passRate(), course.highest(), course.lowest(),
                            Arrays.stream(course.bands).boxed().toList()
                    ));
                }
            }
        }

        List<LecturerPerformanceDTOs.LecturerStat> lecturers = new ArrayList<>();

        for (Map.Entry<Long, List<LecturerPerformanceDTOs.CourseStat>> entry : courseRows.entrySet()) {
            Long id = entry.getKey();
            Tally o = overall.getOrDefault(id, new Tally());
            int[] counts = completion.getOrDefault(id, new int[2]);

            List<LecturerPerformanceDTOs.CourseStat> courses = new ArrayList<>(entry.getValue());
            courses.sort(Comparator.comparing(LecturerPerformanceDTOs.CourseStat::code));

            List<LecturerPerformanceDTOs.TrendPoint> points = new ArrayList<>();
            Map<Long, Tally> perSession = trend.get(id);
            for (AcademicSession s : sessions) {
                Tally t = perSession == null ? null : perSession.get(s.getId());
                if (t != null && t.n > 0) points.add(new LecturerPerformanceDTOs.TrendPoint(s.getName(), t.n, t.average(), t.passRate()));
            }

            lecturers.add(new LecturerPerformanceDTOs.LecturerStat(
                    id, names.get(id), o.n, o.average(), o.passRate(),
                    counts[0] > 0 ? (int) Math.round(counts[1] * 100.0 / counts[0]) : null,
                    courses, points
            ));
        }
        lecturers.sort(Comparator.comparing(LecturerPerformanceDTOs.LecturerStat::name));

        String note = "Scores come from approved results. A course with more than one lecturer is counted for each of them. "
                + "Results reflect the class, the course and the cohort as well as the teaching, so read them alongside student feedback.";

        return new LecturerPerformanceDTOs.Report(
                selected.getId(), selected.getName(), sem == null ? "ALL" : sem,
                department.n, department.average(), department.passRate(), BANDS, lecturers, note
        );
    }

    // ---------------------------------------------------------------- helpers

    private double score(Result r) {
        return Double.parseDouble(r.getTotalScore().toString());
    }

    private String semOf(Object value) {
        String s = String.valueOf(value).trim().toUpperCase();
        return s.startsWith("F") || s.equals("1") ? "FIRST" : "SECOND";
    }

    private String clean(String semester) {
        String s = semester.trim().toUpperCase();
        if (!s.equals("FIRST") && !s.equals("SECOND")) throw bad("Semester must be ALL, FIRST or SECOND.");
        return s;
    }

    private IllegalArgumentException bad(String message) {
        return new IllegalArgumentException(message);
    }
}