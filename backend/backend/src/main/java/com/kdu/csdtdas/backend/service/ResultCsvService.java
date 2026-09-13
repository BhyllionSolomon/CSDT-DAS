package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.entity.Result;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.repository.CourseRegistrationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.ResultRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ResultCsvService {

    private static final String[] TEMPLATE_HEADERS = {
            "MatricNumber", "FullName", "Level", "CAScore", "ExamScore"
    };

    private final CourseRegistrationRepository courseRegistrationRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final ResultRepository resultRepository;
    private final ResultService resultService;

    public ResultCsvService(
            CourseRegistrationRepository courseRegistrationRepository,
            CourseRepository courseRepository,
            StudentRepository studentRepository,
            ResultRepository resultRepository,
            ResultService resultService
    ) {
        this.courseRegistrationRepository = courseRegistrationRepository;
        this.courseRepository = courseRepository;
        this.studentRepository = studentRepository;
        this.resultRepository = resultRepository;
        this.resultService = resultService;
    }

    @Transactional(readOnly = true)
    public byte[] generateTemplate(
            Long courseId,
            Long academicSessionId,
            String semester
    ) {

        List<CourseRegistration> registrations =
                courseRegistrationRepository
                        .findByCourseIdAndAcademicSessionIdAndSemester(
                                courseId,
                                academicSessionId,
                                semester.trim().toUpperCase()
                        );

        StringBuilder csv = new StringBuilder();
        csv.append(String.join(",", TEMPLATE_HEADERS)).append("\n");

        for (CourseRegistration registration : registrations) {

            Student student = registration.getStudent();

            csv.append(csvField(student.getMatricNumber())).append(",");
            csv.append(csvField(student.getFullName())).append(",");
            csv.append(csvField(
                    student.getLevel() != null
                            ? student.getLevel().getCode()
                            : ""
            )).append(",");
            csv.append(",");
            csv.append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public CsvUploadResult processUpload(
            Long courseId,
            Long academicSessionId,
            String semester,
            MultipartFile file
    ) {

        String cleanSemester = semester.trim().toUpperCase();

        courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));

        List<String> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();

            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty.");
            }

            String line;
            int rowNumber = 1;

            while ((line = reader.readLine()) != null) {

                rowNumber++;

                if (line.isBlank()) {
                    continue;
                }

                List<String> fields = parseCsvLine(line);

                if (fields.size() < 5) {
                    errors.add(
                            "Row " + rowNumber + ": expected 5 columns, found "
                                    + fields.size()
                    );
                    continue;
                }

                String matricNumber = fields.get(0).trim();
                String caScoreText = fields.get(3).trim();
                String examScoreText = fields.get(4).trim();

                if (matricNumber.isEmpty()) {
                    errors.add("Row " + rowNumber + ": matric number is missing.");
                    continue;
                }

                if (caScoreText.isEmpty() || examScoreText.isEmpty()) {
                    errors.add(
                            "Row " + rowNumber + " (" + matricNumber
                                    + "): CA or Exam score is missing."
                    );
                    continue;
                }

                Double caScore;
                Double examScore;

                try {
                    caScore = Double.parseDouble(caScoreText);
                    examScore = Double.parseDouble(examScoreText);
                } catch (NumberFormatException e) {
                    errors.add(
                            "Row " + rowNumber + " (" + matricNumber
                                    + "): scores must be numeric."
                    );
                    continue;
                }

                try {
                    Optional<Student> studentOpt =
                            studentRepository.findByMatricNumber(matricNumber);

                    if (studentOpt.isEmpty()) {
                        errors.add(
                                "Row " + rowNumber
                                        + ": no student found with matric number "
                                        + matricNumber
                        );
                        continue;
                    }

                    Student student = studentOpt.get();

                    Optional<Result> existing = resultRepository
                            .findByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
                                    student.getId(),
                                    courseId,
                                    academicSessionId,
                                    cleanSemester
                            );

                    if (existing.isPresent()) {
                        resultService.updateResult(
                                existing.get().getId(),
                                caScore,
                                examScore
                        );
                        updated++;
                    } else {
                        resultService.createResult(
                                student.getId(),
                                courseId,
                                academicSessionId,
                                cleanSemester,
                                caScore,
                                examScore
                        );
                        created++;
                    }

                } catch (IllegalArgumentException e) {
                    errors.add(
                            "Row " + rowNumber + " (" + matricNumber + "): "
                                    + e.getMessage()
                    );
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV file.", e);
        }

        return new CsvUploadResult(created, updated, errors);
    }

    private String csvField(String value) {

        if (value == null) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    private List<String> parseCsvLine(String line) {

        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {

            char ch = line.charAt(i);

            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (ch == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(ch);
                }
            }
        }

        fields.add(current.toString());
        return fields;
    }

    public static class CsvUploadResult {

        private final int created;
        private final int updated;
        private final List<String> errors;

        public CsvUploadResult(int created, int updated, List<String> errors) {
            this.created = created;
            this.updated = updated;
            this.errors = errors;
        }

        public int getCreated() {
            return created;
        }

        public int getUpdated() {
            return updated;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}