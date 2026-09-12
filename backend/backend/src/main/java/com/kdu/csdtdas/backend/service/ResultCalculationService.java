package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CourseResultDTO;
import com.kdu.csdtdas.backend.dto.CumulativeSummaryDTO;
import com.kdu.csdtdas.backend.dto.SemesterResultDTO;
import com.kdu.csdtdas.backend.dto.SemesterSummaryDTO;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Result;
import com.kdu.csdtdas.backend.repository.ResultRepository;
import com.kdu.csdtdas.backend.util.DegreeClassUtil;
import com.kdu.csdtdas.backend.util.GradeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ResultCalculationService {

    private static final int SCALE = 8;
    private static final BigDecimal GOOD_STANDING_THRESHOLD = BigDecimal.valueOf(1.50);

    private final ResultRepository resultRepository;

    public ResultCalculationService(ResultRepository resultRepository) {
        this.resultRepository = resultRepository;
    }

    public SemesterResultDTO calculateSemesterResult(
            Long studentId,
            Long sessionId,
            String semester
    ) {

        List<Result> results =
                resultRepository.findSemesterResults(
                        studentId,
                        sessionId,
                        semester
                );

        if (results.isEmpty()) {
            throw new IllegalArgumentException(
                    "No results found for the selected student, session and semester"
            );
        }

        Result first = results.get(0);

        List<CourseResultDTO> courses =
                results.stream()
                        .map(this::toCourseResult)
                        .toList();

        SemesterSummaryDTO semesterSummary =
                calculateSemesterSummary(results);

        List<Result> previousResults =
                getPreviousResults(
                        studentId,
                        first.getAcademicSession(),
                        semester
                );

        CumulativeSummaryDTO previousCumulative =
                calculatePreviousCumulative(previousResults);

        CumulativeSummaryDTO currentCumulative =
                calculateCurrentCumulative(
                        previousCumulative,
                        semesterSummary
                );

        List<Result> allUpToAndIncluding =
                new ArrayList<>(previousResults);
        allUpToAndIncluding.addAll(results);

        Set<String> outstandingCourses =
                new LinkedHashSet<>();
        updateOutstanding(
                outstandingCourses,
                allUpToAndIncluding
        );

        String remark =
                buildRemark(
                        currentCumulative.cgpa(),
                        outstandingCourses
                );

        return new SemesterResultDTO(
                first.getStudent().getId(),
                first.getStudent().getMatricNumber(),
                first.getStudent().getFullName(),
                first.getAcademicSession().getId(),
                first.getAcademicSession().getName(),
                parseSemester(semester),
                courses,
                semesterSummary,
                previousCumulative,
                currentCumulative,
                remark
        );
    }

    public List<SemesterResultDTO> calculateFullAcademicHistory(
            Long studentId
    ) {

        List<Result> allResults =
                resultRepository.findFullAcademicHistory(studentId);

        if (allResults.isEmpty()) {
            throw new IllegalArgumentException(
                    "No academic results found for this student"
            );
        }

        Map<String, List<Result>> grouped =
                allResults.stream()
                        .collect(
                                LinkedHashMap::new,
                                (map, result) -> {

                                    String key =
                                            result.getAcademicSession().getId()
                                                    + "-"
                                                    + result.getSemester();

                                    map.computeIfAbsent(
                                            key,
                                            k -> new ArrayList<>()
                                    ).add(result);
                                },
                                Map::putAll
                        );

        List<List<Result>> semesters =
                new ArrayList<>(grouped.values());

        semesters.sort(
                Comparator
                        .comparing(
                                (List<Result> list) ->
                                        list.get(0)
                                                .getAcademicSession()
                                                .getStartDate()
                        )
                        .thenComparing(
                                list -> parseSemester(
                                        list.get(0).getSemester()
                                )
                        )
        );

        List<SemesterResultDTO> history =
                new ArrayList<>();

        CumulativeSummaryDTO previous =
                new CumulativeSummaryDTO(
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        DegreeClassUtil.classify(BigDecimal.ZERO)
                );

        Set<String> outstandingCourses =
                new LinkedHashSet<>();

        for (List<Result> semesterResults : semesters) {

            Result first = semesterResults.get(0);

            SemesterSummaryDTO semesterSummary =
                    calculateSemesterSummary(semesterResults);

            CumulativeSummaryDTO current =
                    calculateCurrentCumulative(
                            previous,
                            semesterSummary
                    );

            List<CourseResultDTO> courses =
                    semesterResults.stream()
                            .map(this::toCourseResult)
                            .toList();

            updateOutstanding(
                    outstandingCourses,
                    semesterResults
            );

            String remark =
                    buildRemark(
                            current.cgpa(),
                            outstandingCourses
                    );

            history.add(
                    new SemesterResultDTO(
                            first.getStudent().getId(),
                            first.getStudent().getMatricNumber(),
                            first.getStudent().getFullName(),
                            first.getAcademicSession().getId(),
                            first.getAcademicSession().getName(),
                            parseSemester(first.getSemester()),
                            courses,
                            semesterSummary,
                            previous,
                            current,
                            remark
                    )
            );

            previous = current;
        }

        return history;
    }

    private CourseResultDTO toCourseResult(Result result) {

        BigDecimal score =
                toBigDecimal(result.getTotalScore());

        Integer gradePoint =
                GradeUtil.getGradePoint(score);

        String letterGrade =
                GradeUtil.getLetterGrade(score);

        BigDecimal weightedGradePoint =
                BigDecimal.valueOf(
                        result.getCourse().getCreditUnit()
                ).multiply(
                        BigDecimal.valueOf(gradePoint)
                );

        return new CourseResultDTO(
                result.getId(),
                result.getCourse().getId(),
                result.getCourse().getCode(),
                result.getCourse().getTitle(),
                result.getCourse().getCreditUnit(),
                score,
                letterGrade,
                gradePoint,
                weightedGradePoint,
                GradeUtil.isPassed(score)
        );
    }

    private SemesterSummaryDTO calculateSemesterSummary(
            List<Result> results
    ) {

        int tuo = 0;
        int tup = 0;
        int tuf = 0;

        BigDecimal twgp =
                BigDecimal.ZERO;

        for (Result result : results) {

            int creditUnit =
                    result.getCourse().getCreditUnit();

            BigDecimal score =
                    toBigDecimal(result.getTotalScore());

            int gradePoint =
                    GradeUtil.getGradePoint(score);

            BigDecimal weightedGradePoint =
                    BigDecimal.valueOf(creditUnit)
                            .multiply(
                                    BigDecimal.valueOf(gradePoint)
                            );

            tuo += creditUnit;
            twgp = twgp.add(weightedGradePoint);

            if (GradeUtil.isPassed(score)) {
                tup += creditUnit;
            } else {
                tuf += creditUnit;
            }
        }

        BigDecimal gpa =
                tuo == 0
                        ? BigDecimal.ZERO
                        : twgp.divide(
                        BigDecimal.valueOf(tuo),
                        SCALE,
                        RoundingMode.HALF_UP
                );

        return new SemesterSummaryDTO(
                results.get(0).getAcademicSession().getId(),
                results.get(0).getAcademicSession().getName(),
                parseSemester(
                        results.get(0).getSemester()
                ),
                tuo,
                tup,
                tuf,
                twgp.setScale(
                        SCALE,
                        RoundingMode.HALF_UP
                ),
                gpa.setScale(
                        SCALE,
                        RoundingMode.HALF_UP
                )
        );
    }

    private List<Result> getPreviousResults(
            Long studentId,
            AcademicSession targetSession,
            String semester
    ) {

        List<Result> allResults =
                resultRepository.findFullAcademicHistory(studentId);

        return allResults.stream()
                .filter(result ->
                        isBefore(
                                result,
                                targetSession,
                                semester
                        )
                )
                .toList();
    }

    private CumulativeSummaryDTO calculatePreviousCumulative(
            List<Result> previousResults
    ) {

        if (previousResults.isEmpty()) {
            return new CumulativeSummaryDTO(
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    DegreeClassUtil.classify(BigDecimal.ZERO)
            );
        }

        int ctu = 0;
        BigDecimal cumulativeTwgp =
                BigDecimal.ZERO;

        for (Result result : previousResults) {

            int creditUnit =
                    result.getCourse().getCreditUnit();

            BigDecimal score =
                    toBigDecimal(result.getTotalScore());

            int gradePoint =
                    GradeUtil.getGradePoint(score);

            ctu += creditUnit;

            cumulativeTwgp =
                    cumulativeTwgp.add(
                            BigDecimal.valueOf(creditUnit)
                                    .multiply(
                                            BigDecimal.valueOf(
                                                    gradePoint
                                            )
                                    )
                    );
        }

        BigDecimal cgpa =
                ctu == 0
                        ? BigDecimal.ZERO
                        : cumulativeTwgp.divide(
                        BigDecimal.valueOf(ctu),
                        SCALE,
                        RoundingMode.HALF_UP
                );

        return new CumulativeSummaryDTO(
                ctu,
                cumulativeTwgp.setScale(
                        SCALE,
                        RoundingMode.HALF_UP
                ),
                cgpa.setScale(
                        SCALE,
                        RoundingMode.HALF_UP
                ),
                DegreeClassUtil.classify(cgpa)
        );
    }

    private CumulativeSummaryDTO calculateCurrentCumulative(
            CumulativeSummaryDTO previous,
            SemesterSummaryDTO currentSemester
    ) {

        int ctu =
                previous.ctu()
                        + currentSemester.tuo();

        BigDecimal cumulativeTwgp =
                previous.cumulativeTwgp()
                        .add(currentSemester.twgp());

        BigDecimal cgpa =
                ctu == 0
                        ? BigDecimal.ZERO
                        : cumulativeTwgp.divide(
                        BigDecimal.valueOf(ctu),
                        SCALE,
                        RoundingMode.HALF_UP
                );

        return new CumulativeSummaryDTO(
                ctu,
                cumulativeTwgp.setScale(
                        SCALE,
                        RoundingMode.HALF_UP
                ),
                cgpa.setScale(
                        SCALE,
                        RoundingMode.HALF_UP
                ),
                DegreeClassUtil.classify(cgpa)
        );
    }

    private boolean isBefore(
            Result result,
            AcademicSession targetSession,
            String semester
    ) {

        if (result.getAcademicSession().getId()
                .equals(targetSession.getId())) {

            return parseSemester(result.getSemester())
                    < parseSemester(semester);
        }

        return result.getAcademicSession()
                .getStartDate()
                .isBefore(
                        targetSession.getStartDate()
                );
    }

    /**
     * Walks the given results (must be in chronological order) and updates
     * the outstanding-carryover set in place: a failed course is added, a
     * passed course removes it. A course that was failed once and later
     * passed on retake no longer appears in the outstanding set.
     */
    private void updateOutstanding(
            Set<String> outstanding,
            List<Result> resultsInOrder
    ) {

        for (Result result : resultsInOrder) {

            String courseCode =
                    result.getCourse().getCode();

            BigDecimal score =
                    toBigDecimal(result.getTotalScore());

            if (GradeUtil.isPassed(score)) {
                outstanding.remove(courseCode);
            } else {
                outstanding.add(courseCode);
            }
        }
    }

    /**
     * GS/NGS is based on cumulative CGPA crossing the university's Fail
     * threshold (1.50), matching DegreeClassUtil's boundary — not on
     * whether any course failed in this specific semester. The course list
     * shows every currently-outstanding carryover (failed and not yet
     * passed on a later attempt), which may include courses failed in
     * earlier semesters.
     */
    private String buildRemark(
            BigDecimal cgpa,
            Set<String> outstandingCourses
    ) {

        String status =
                cgpa.compareTo(GOOD_STANDING_THRESHOLD) >= 0
                        ? "GS"
                        : "NGS";

        if (outstandingCourses.isEmpty()) {
            return status;
        }

        return status
                + " - "
                + String.join(
                ", ",
                outstandingCourses
        );
    }

    private int parseSemester(String semester) {

        if (semester == null) {
            throw new IllegalArgumentException(
                    "Semester cannot be null"
            );
        }

        String normalized =
                semester.trim().toUpperCase();

        if (normalized.equals("1")
                || normalized.equals("FIRST")
                || normalized.equals("FIRST SEMESTER")
                || normalized.equals("SEMESTER 1")) {
            return 1;
        }

        if (normalized.equals("2")
                || normalized.equals("SECOND")
                || normalized.equals("SECOND SEMESTER")
                || normalized.equals("SEMESTER 2")) {
            return 2;
        }

        throw new IllegalArgumentException(
                "Invalid semester value: " + semester
        );
    }

    private BigDecimal toBigDecimal(
            Double value
    ) {

        return value == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(value);
    }
}