package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.ResultAnalytics;
import com.kdu.csdtdas.backend.dto.SelfReportedResultRow;
import com.kdu.csdtdas.backend.entity.SelfReportedResult;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.repository.SelfReportedResultRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SelfReportedResultService {

    private final StudentRepository studentRepository;
    private final SelfReportedResultRepository resultRepository;

    public SelfReportedResultService(StudentRepository studentRepository, SelfReportedResultRepository resultRepository) {
        this.studentRepository = studentRepository;
        this.resultRepository = resultRepository;
    }

    @Transactional(readOnly = true)
    public List<SelfReportedResultRow> extractResults(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) return extractFromExcel(file);
        return extractFromCsv(file);
    }

    private List<SelfReportedResultRow> extractFromExcel(MultipartFile file) {
        List<SelfReportedResultRow> rows = new ArrayList<>();
        try (var in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());

            int codeCol = -1, titleCol = -1, scoreCol = -1, gradeCol = -1, semCol = -1, sessCol = -1;
            for (Cell cell : headerRow) {
                String h = formatter.formatCellValue(cell).toUpperCase();
                int idx = cell.getColumnIndex();
                if (h.contains("CODE")) codeCol = idx;
                else if (h.contains("TITLE") || h.contains("COURSE NAME")) titleCol = idx;
                else if (h.contains("SCORE") || h.contains("TOTAL")) scoreCol = idx;
                else if (h.contains("GRADE")) gradeCol = idx;
                else if (h.contains("SEMESTER")) semCol = idx;
                else if (h.contains("SESSION")) sessCol = idx;
            }
            if (codeCol == -1) throw new IllegalArgumentException("Could not find a 'Course Code' column.");

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String code = getCell(row, codeCol, formatter);
                if (code.isBlank()) continue;
                rows.add(new SelfReportedResultRow(
                        code.trim().toUpperCase(), getCell(row, titleCol, formatter),
                        getCell(row, scoreCol, formatter), getCell(row, gradeCol, formatter),
                        getCell(row, semCol, formatter), getCell(row, sessCol, formatter)
                ));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Excel file.", e);
        }
        return rows;
    }

    private String getCell(Row row, int col, DataFormatter formatter) {
        if (col == -1) return "";
        Cell cell = row.getCell(col);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private List<SelfReportedResultRow> extractFromCsv(MultipartFile file) {
        List<SelfReportedResultRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("File is empty.");
            List<String> headers = splitCsv(headerLine);

            int codeCol = -1, titleCol = -1, scoreCol = -1, gradeCol = -1, semCol = -1, sessCol = -1;
            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i).toUpperCase();
                if (h.contains("CODE")) codeCol = i;
                else if (h.contains("TITLE") || h.contains("COURSE NAME")) titleCol = i;
                else if (h.contains("SCORE") || h.contains("TOTAL")) scoreCol = i;
                else if (h.contains("GRADE")) gradeCol = i;
                else if (h.contains("SEMESTER")) semCol = i;
                else if (h.contains("SESSION")) sessCol = i;
            }
            if (codeCol == -1) throw new IllegalArgumentException("Could not find a 'Course Code' column.");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> cells = splitCsv(line);
                String code = codeCol < cells.size() ? cells.get(codeCol).trim() : "";
                if (code.isEmpty()) continue;
                rows.add(new SelfReportedResultRow(
                        code.toUpperCase(), field(cells, titleCol), field(cells, scoreCol),
                        field(cells, gradeCol), field(cells, semCol), field(cells, sessCol)
                ));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV file.", e);
        }
        return rows;
    }

    private String field(List<String> cells, int col) { return col != -1 && col < cells.size() ? cells.get(col).trim() : ""; }

    private List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') { if (i + 1 < line.length() && line.charAt(i + 1) == '"') { current.append('"'); i++; } else inQuotes = false; }
                else current.append(ch);
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == ',') { fields.add(current.toString()); current.setLength(0); }
                else current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    public int saveResults(Long studentId, List<SelfReportedResultRow> rows) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found."));

        int saved = 0;
        for (SelfReportedResultRow row : rows) {
            try {
                SelfReportedResult entity = new SelfReportedResult();
                entity.setStudent(student);
                entity.setCourseCode(row.getCourseCode());
                entity.setCourseTitle(row.getCourseTitle());
                entity.setTotalScore(Double.parseDouble(row.getTotalScore().trim()));
                entity.setGrade(row.getGrade());
                entity.setSemester(row.getSemester());
                entity.setSessionLabel(row.getSessionLabel());
                resultRepository.save(entity);
                saved++;
            } catch (Exception ignored) {}
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public ResultAnalytics getAnalytics(Long studentId) {
        List<SelfReportedResult> results = resultRepository.findByStudentIdOrderByUploadedAtDesc(studentId);

        ResultAnalytics analytics = new ResultAnalytics();

        if (results.isEmpty()) {
            analytics.setAverageScore(0);
            analytics.setGradeDistribution(Collections.emptyMap());
            analytics.setWeakCourses(Collections.emptyList());
            analytics.setStrongCourses(Collections.emptyList());
            analytics.setAdvice("Upload your results to see personalised analytics.");
            return analytics;
        }

        double avg = results.stream().mapToDouble(SelfReportedResult::getTotalScore).average().orElse(0);
        analytics.setAverageScore(Math.round(avg * 100.0) / 100.0);

        Map<String, Long> distribution = results.stream()
                .filter(r -> r.getGrade() != null && !r.getGrade().isBlank())
                .collect(Collectors.groupingBy(SelfReportedResult::getGrade, Collectors.counting()));
        analytics.setGradeDistribution(distribution);

        List<SelfReportedResultRow> weak = results.stream()
                .filter(r -> r.getTotalScore() < 50)
                .sorted(Comparator.comparingDouble(SelfReportedResult::getTotalScore))
                .limit(5)
                .map(r -> new SelfReportedResultRow(r.getCourseCode(), r.getCourseTitle(),
                        String.valueOf(r.getTotalScore()), r.getGrade(), r.getSemester(), r.getSessionLabel()))
                .collect(Collectors.toList());
        analytics.setWeakCourses(weak);

        List<SelfReportedResultRow> strong = results.stream()
                .filter(r -> r.getTotalScore() >= 70)
                .sorted(Comparator.comparingDouble(SelfReportedResult::getTotalScore).reversed())
                .limit(5)
                .map(r -> new SelfReportedResultRow(r.getCourseCode(), r.getCourseTitle(),
                        String.valueOf(r.getTotalScore()), r.getGrade(), r.getSemester(), r.getSessionLabel()))
                .collect(Collectors.toList());
        analytics.setStrongCourses(strong);

        String advice;
        if (!weak.isEmpty()) {
            String codes = weak.stream().map(SelfReportedResultRow::getCourseCode).collect(Collectors.joining(", "));
            advice = "You're below 50% in " + weak.size() + " course(s): " + codes
                    + ". Consider speaking with your Level Adviser or the lecturers for these courses.";
        } else if (avg >= 70) {
            advice = "Strong performance across the board — keep it up!";
        } else {
            advice = "Your average is " + Math.round(avg) + "%. Steady, consistent study across all courses will help push this higher.";
        }
        analytics.setAdvice(advice);

        return analytics;
    }
}