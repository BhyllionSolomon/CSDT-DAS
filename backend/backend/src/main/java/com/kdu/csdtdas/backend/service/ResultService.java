package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.Result;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.ResultRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.util.GradeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class ResultService {

    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final AcademicSessionRepository academicSessionRepository;

    public ResultService(
            ResultRepository resultRepository,
            StudentRepository studentRepository,
            CourseRepository courseRepository,
            AcademicSessionRepository academicSessionRepository
    ) {
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.academicSessionRepository = academicSessionRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result createResult(
            Long studentId,
            Long courseId,
            Long academicSessionId,
            String semester,
            Double caScore,
            Double examScore
    ) {

        validateScores(caScore, examScore);

        if (semester == null || semester.isBlank()) {
            throw new IllegalArgumentException("Semester is required.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));

        AcademicSession academicSession =
                academicSessionRepository.findById(academicSessionId)
                        .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        String cleanSemester = semester.trim().toUpperCase();

        boolean exists = resultRepository
                .existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
                        studentId, courseId, academicSessionId, cleanSemester
                );

        if (exists) {
            throw new IllegalArgumentException(
                    "A result already exists for this student, course, session and semester."
            );
        }

        Result result = new Result();
        result.setStudent(student);
        result.setCourse(course);
        result.setAcademicSession(academicSession);
        result.setSemester(cleanSemester);
        result.setCaScore(caScore);
        result.setExamScore(examScore);

        calculateGrade(result);

        result.setStatus("PENDING");

        return resultRepository.save(result);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Result updateResult(
            Long resultId,
            Double caScore,
            Double examScore
    ) {

        validateScores(caScore, examScore);

        Result result = getResult(resultId);

        result.setCaScore(caScore);
        result.setExamScore(examScore);

        calculateGrade(result);

        result.setStatus("PENDING");

        return resultRepository.save(result);
    }

    @Transactional(readOnly = true)
    public Result getResult(Long resultId) {
        return resultRepository.findByIdWithDetails(resultId)
                .orElseThrow(() -> new IllegalArgumentException("Result not found."));
    }

    @Transactional(readOnly = true)
    public List<Result> getStudentResults(Long studentId) {
        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Student not found.");
        }
        return resultRepository.findByStudentIdWithDetails(studentId);
    }

    @Transactional(readOnly = true)
    public List<Result> getStudentResultsForSession(
            Long studentId,
            Long academicSessionId
    ) {
        if (!studentRepository.existsById(studentId)) {
            throw new IllegalArgumentException("Student not found.");
        }
        if (!academicSessionRepository.existsById(academicSessionId)) {
            throw new IllegalArgumentException("Academic session not found.");
        }
        return resultRepository.findByStudentIdAndAcademicSessionIdWithDetails(
                studentId, academicSessionId
        );
    }

    public Result approveResult(Long resultId) {
        Result result = getResult(resultId);
        result.setStatus("APPROVED");
        return resultRepository.save(result);
    }

    public Result rejectResult(Long resultId) {
        Result result = getResult(resultId);
        result.setStatus("REJECTED");
        return resultRepository.save(result);
    }

    public int approveAllPending(Long courseId, Long academicSessionId, String semester) {
        List<Result> pending = resultRepository
                .findByCourseIdAndAcademicSessionIdAndSemesterAndStatus(
                        courseId, academicSessionId, semester.trim().toUpperCase(), "PENDING"
                );
        for (Result result : pending) {
            result.setStatus("APPROVED");
        }
        resultRepository.saveAll(pending);
        return pending.size();
    }

    public int deleteAllForCourseSessionSemester(
            Long courseId,
            Long academicSessionId,
            String semester
    ) {
        List<Result> results = resultRepository
                .findByCourseIdAndAcademicSessionIdAndSemester(
                        courseId, academicSessionId, semester.trim().toUpperCase()
                );
        resultRepository.deleteAll(results);
        return results.size();
    }



    private void calculateGrade(Result result) {

        double ca = result.getCaScore() == null ? 0.0 : result.getCaScore();
        double exam = result.getExamScore() == null ? 0.0 : result.getExamScore();
        double total = ca + exam;

        result.setTotalScore(total);

        BigDecimal score = BigDecimal.valueOf(total);

        result.setGrade(GradeUtil.getLetterGrade(score));
        result.setGradePoint(GradeUtil.getGradePoint(score));
        result.setRemark(getGradeRemark(GradeUtil.getLetterGrade(score)));
    }

    private String getGradeRemark(String grade) {
        return switch (grade) {
            case "A" -> "Excellent";
            case "B" -> "Very Good";
            case "C" -> "Good";
            case "D" -> "Fair";
            case "E" -> "Pass";
            case "F" -> "Fail";
            default -> "Unknown";
        };
    }

    public ResultCsvService.CsvUploadResult bulkSave(
            Long courseId,
            Long academicSessionId,
            String semester,
            java.util.List<com.kdu.csdtdas.backend.dto.BulkResultEntry> entries
    ) {
        String cleanSemester = semester.trim().toUpperCase();
        java.util.List<String> errors = new java.util.ArrayList<>();
        int created = 0;
        int updated = 0;

        for (com.kdu.csdtdas.backend.dto.BulkResultEntry entry : entries) {

            if (entry.getStudentId() == null
                    || entry.getCaScore() == null
                    || entry.getExamScore() == null) {
                continue;
            }

            try {
                java.util.Optional<Result> existing =
                        resultRepository.findByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
                                entry.getStudentId(), courseId, academicSessionId, cleanSemester
                        );

                if (existing.isPresent()) {
                    updateResult(existing.get().getId(), entry.getCaScore(), entry.getExamScore());
                    updated++;
                } else {
                    createResult(
                            entry.getStudentId(), courseId, academicSessionId, cleanSemester,
                            entry.getCaScore(), entry.getExamScore()
                    );
                    created++;
                }

            } catch (IllegalArgumentException e) {
                errors.add("Student ID " + entry.getStudentId() + ": " + e.getMessage());
            }
        }

        return new ResultCsvService.CsvUploadResult(created, updated, errors);
    }


    private void validateScores(Double caScore, Double examScore) {

        if (caScore == null) throw new IllegalArgumentException("CA score is required.");
        if (examScore == null) throw new IllegalArgumentException("Exam score is required.");
        if (caScore < 0) throw new IllegalArgumentException("CA score cannot be negative.");
        if (examScore < 0) throw new IllegalArgumentException("Exam score cannot be negative.");

        if (caScore > 30) {
            throw new IllegalArgumentException("CA score cannot exceed 30.");
        }

        if (examScore > 70) {
            throw new IllegalArgumentException("Exam score cannot exceed 70.");
        }

        double total = caScore + examScore;

        if (total > 100) {
            throw new IllegalArgumentException("CA score plus Exam score cannot exceed 100.");
        }

        if (isBlockedBorderlineTotal(total)) {
            throw new IllegalArgumentException(
                    "Total score of " + total + " is a blocked borderline value. "
                            + "Please review and re-enter scores that clearly place the student "
                            + "above or below this boundary."
            );
        }
    }

    private boolean isBlockedBorderlineTotal(double total) {
        int rounded = (int) Math.round(total);
        return rounded == 34 || rounded == 44 || rounded == 49
                || rounded == 59 || rounded == 69 || rounded == 79;
    }
}