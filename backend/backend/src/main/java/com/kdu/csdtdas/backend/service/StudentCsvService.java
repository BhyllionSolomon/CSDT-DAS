package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Department;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.DepartmentRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
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
public class StudentCsvService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final LevelRepository levelRepository;
    private final AcademicSessionRepository academicSessionRepository;

    public StudentCsvService(
            StudentRepository studentRepository,
            DepartmentRepository departmentRepository,
            ProgrammeRepository programmeRepository,
            LevelRepository levelRepository,
            AcademicSessionRepository academicSessionRepository
    ) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.programmeRepository = programmeRepository;
        this.levelRepository = levelRepository;
        this.academicSessionRepository = academicSessionRepository;
    }

    public CsvUploadResult processUpload(
            Long departmentId,
            Long academicSessionId,
            MultipartFile file
    ) {

        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new IllegalArgumentException("Department not found."));

        if (!academicSessionRepository.existsById(academicSessionId)) {
            throw new IllegalArgumentException("Academic session not found.");
        }

        List<String> errors = new ArrayList<>();
        int created = 0;
        int skipped = 0;

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

                if (fields.size() < 4) {
                    errors.add(
                            "Row " + rowNumber + ": expected 4 columns, found "
                                    + fields.size()
                    );
                    continue;
                }

                String matricNumber = fields.get(0).trim();
                String fullName = fields.get(1).trim();
                String programmeCode = fields.get(2).trim();
                String levelCode = fields.get(3).trim();

                if (matricNumber.isEmpty()) {
                    errors.add("Row " + rowNumber + ": matric number is missing.");
                    continue;
                }

                if (studentRepository.existsByMatricNumber(matricNumber)) {
                    skipped++;
                    continue;
                }

                if (fullName.isEmpty()) {
                    fullName = "UNKNOWN - " + matricNumber;
                }

                Optional<Programme> programmeOpt =
                        programmeRepository.findByCode(programmeCode);

                if (programmeOpt.isEmpty()) {
                    errors.add(
                            "Row " + rowNumber + " (" + matricNumber
                                    + "): no programme found with code '"
                                    + programmeCode + "'"
                    );
                    continue;
                }

                Level level = findLevelByCode(levelCode);

                if (level == null) {
                    errors.add(
                            "Row " + rowNumber + " (" + matricNumber
                                    + "): no level found with code '"
                                    + levelCode + "'"
                    );
                    continue;
                }

                try {
                    Student student = new Student();
                    student.setMatricNumber(matricNumber);
                    student.setFullName(fullName);
                    student.setDepartment(department);
                    student.setProgramme(programmeOpt.get());
                    student.setLevel(level);
                    student.setAcademicSession(
                            academicSessionRepository.findById(academicSessionId).get()
                    );
                    student.setStatus("ACTIVE");

                    studentRepository.save(student);
                    created++;

                } catch (Exception e) {
                    errors.add(
                            "Row " + rowNumber + " (" + matricNumber + "): "
                                    + e.getMessage()
                    );
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV file.", e);
        }

        return new CsvUploadResult(created, skipped, errors);
    }

    private Level findLevelByCode(String code) {
        return levelRepository.findAll().stream()
                .filter(l -> l.getCode().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
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
        private final int skipped;
        private final List<String> errors;

        public CsvUploadResult(int created, int skipped, List<String> errors) {
            this.created = created;
            this.skipped = skipped;
            this.errors = errors;
        }

        public int getCreated() {
            return created;
        }

        public int getSkipped() {
            return skipped;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}