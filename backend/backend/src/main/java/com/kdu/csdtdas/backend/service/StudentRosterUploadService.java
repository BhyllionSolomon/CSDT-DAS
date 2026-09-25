package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.StudentRosterRow;
import com.kdu.csdtdas.backend.dto.StudentRosterSaveResult;
import com.kdu.csdtdas.backend.entity.*;
import com.kdu.csdtdas.backend.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




@Service
@Transactional
public class StudentRosterUploadService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final LevelRepository levelRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final ReportedCgpaRepository reportedCgpaRepository;

    // Add these constants near the top of the class:
    private static final Pattern MATRIC_PATTERN = Pattern.compile("\\bKDU[A-Z0-9]{6,15}\\b");
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("\\d+\\.\\d{2}");

    public StudentRosterUploadService(
            StudentRepository studentRepository,
            DepartmentRepository departmentRepository,
            ProgrammeRepository programmeRepository,
            LevelRepository levelRepository,
            AcademicSessionRepository academicSessionRepository,
            ReportedCgpaRepository reportedCgpaRepository
    ) {
        this.studentRepository = studentRepository;
        this.departmentRepository = departmentRepository;
        this.programmeRepository = programmeRepository;
        this.levelRepository = levelRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.reportedCgpaRepository = reportedCgpaRepository;
    }
    @Transactional(readOnly = true)
    public List<StudentRosterRow> extractRoster(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();

        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return extractFromExcel(file);
        }
        if (filename.endsWith(".docx")) {
            return extractFromDocx(file);
        }
        if (filename.endsWith(".pdf")) {
            return extractFromPdf(file);
        }
        return extractFromCsv(file);
    }

    private List<StudentRosterRow> extractFromDocx(MultipartFile file) {
        List<StudentRosterRow> rows = new ArrayList<>();

        try (var in = file.getInputStream(); XWPFDocument doc = new XWPFDocument(in)) {

            XWPFTable table = findLargestTable(doc);

            if (table != null) {
                List<XWPFTableRow> tableRows = table.getRows();
                List<String> headers = docxCellTexts(tableRows.get(0));

                int matricCol = -1, nameCol = -1, cgpaCol = -1;
                for (int i = 0; i < headers.size(); i++) {
                    String h = headers.get(i).toUpperCase();
                    if (h.contains("MATRIC")) matricCol = i;
                    else if (h.contains("NAME")) nameCol = i;
                    else if (h.contains("CGPA")) cgpaCol = i;
                }

                if (matricCol != -1) {
                    for (int r = 1; r < tableRows.size(); r++) {
                        List<String> cells = docxCellTexts(tableRows.get(r));
                        if (matricCol >= cells.size()) continue;
                        String matric = cells.get(matricCol).trim();
                        if (matric.isEmpty()) continue;

                        rows.add(new StudentRosterRow(
                                matric.toUpperCase(),
                                nameCol != -1 && nameCol < cells.size() ? cells.get(nameCol).trim() : "",
                                "", "",
                                cgpaCol != -1 && cgpaCol < cells.size() ? cells.get(cgpaCol).trim() : ""
                        ));
                    }
                    return rows;
                }
            }

            // Fallback: no usable table found, scan paragraph text like a PDF.
            StringBuilder text = new StringBuilder();
            doc.getParagraphs().forEach(p -> text.append(p.getText()).append("\n"));
            return extractRowsFromPlainText(text.toString());

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Word document.", e);
        }
    }

    private List<StudentRosterRow> extractFromPdf(MultipartFile file) {
        try (var in = file.getInputStream()) {
            PdfReader reader = new PdfReader(in);
            PdfTextExtractor extractor = new PdfTextExtractor(reader);

            StringBuilder allText = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                allText.append(extractor.getTextFromPage(page)).append("\n");
            }
            reader.close();

            return extractRowsFromPlainText(allText.toString());

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read PDF file.", e);
        }
    }

    private List<StudentRosterRow> extractRowsFromPlainText(String text) {
        List<StudentRosterRow> rows = new ArrayList<>();

        for (String line : text.split("\\r?\\n")) {
            Matcher matricMatcher = MATRIC_PATTERN.matcher(line);
            if (!matricMatcher.find()) continue;

            String matric = matricMatcher.group().toUpperCase();

            String beforeMatric = line.substring(0, matricMatcher.start()).trim();
            String name = beforeMatric.replaceFirst("^\\d+\\s+", "").trim();

            String afterMatric = line.substring(matricMatcher.end());
            Matcher decimalMatcher = DECIMAL_PATTERN.matcher(afterMatric);

            String cgpa = "";
            while (decimalMatcher.find()) {
                BigDecimal candidate = new BigDecimal(decimalMatcher.group());
                if (candidate.compareTo(BigDecimal.ZERO) >= 0 && candidate.compareTo(BigDecimal.valueOf(5.00)) <= 0) {
                    cgpa = decimalMatcher.group();
                }
            }

            rows.add(new StudentRosterRow(matric, name, "", "", cgpa));
        }

        return rows;
    }

    private XWPFTable findLargestTable(XWPFDocument doc) {
        XWPFTable best = null;
        int mostRows = 0;
        for (XWPFTable t : doc.getTables()) {
            if (t.getRows().size() > mostRows) {
                mostRows = t.getRows().size();
                best = t;
            }
        }
        return best;
    }

    private List<String> docxCellTexts(XWPFTableRow row) {
        List<String> texts = new ArrayList<>();
        for (XWPFTableCell cell : row.getTableCells()) {
            texts.add(cell.getText());
        }
        return texts;
    }


    private List<StudentRosterRow> extractFromExcel(MultipartFile file) {
        List<StudentRosterRow> rows = new ArrayList<>();

        try (var in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            int matricCol = -1, nameCol = -1, programmeCol = -1, levelCol = -1, cgpaCol = -1;

            for (Cell cell : headerRow) {
                String header = formatter.formatCellValue(cell).toUpperCase();
                int idx = cell.getColumnIndex();
                if (header.contains("MATRIC")) matricCol = idx;
                else if (header.contains("NAME")) nameCol = idx;
                else if (header.contains("PROGRAM")) programmeCol = idx;
                else if (header.contains("LEVEL")) levelCol = idx;
                else if (header.contains("CGPA")) cgpaCol = idx;
            }

            if (matricCol == -1) {
                throw new IllegalArgumentException("Could not find a 'MatricNumber' column in this file.");
            }

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String matric = getCell(row, matricCol, formatter);
                if (matric.isBlank()) continue;

                rows.add(new StudentRosterRow(
                        matric.trim().toUpperCase(),
                        getCell(row, nameCol, formatter),
                        getCell(row, programmeCol, formatter),
                        getCell(row, levelCol, formatter),
                        getCell(row, cgpaCol, formatter)
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
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private List<StudentRosterRow> extractFromCsv(MultipartFile file) {
        List<StudentRosterRow> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("File is empty.");

            List<String> headers = splitCsv(headerLine);
            int matricCol = -1, nameCol = -1, programmeCol = -1, levelCol = -1, cgpaCol = -1;

            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i).toUpperCase();
                if (h.contains("MATRIC")) matricCol = i;
                else if (h.contains("NAME")) nameCol = i;
                else if (h.contains("PROGRAM")) programmeCol = i;
                else if (h.contains("LEVEL")) levelCol = i;
                else if (h.contains("CGPA")) cgpaCol = i;
            }

            if (matricCol == -1) {
                throw new IllegalArgumentException("Could not find a 'MatricNumber' column in this file.");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> cells = splitCsv(line);

                String matric = matricCol < cells.size() ? cells.get(matricCol).trim() : "";
                if (matric.isEmpty()) continue;

                rows.add(new StudentRosterRow(
                        matric.toUpperCase(),
                        field(cells, nameCol),
                        field(cells, programmeCol),
                        field(cells, levelCol),
                        field(cells, cgpaCol)
                ));
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV file.", e);
        }

        return rows;
    }

    private String field(List<String> cells, int col) {
        return col != -1 && col < cells.size() ? cells.get(col).trim() : "";
    }

    private List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { current.append('"'); i++; }
                    else inQuotes = false;
                } else current.append(ch);
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == ',') { fields.add(current.toString()); current.setLength(0); }
                else current.append(ch);
            }
        }
        fields.add(current.toString());
        return fields;
    }

    public StudentRosterSaveResult saveRoster(
            Long programmeId, Long levelId, Long academicSessionId,
            List<StudentRosterRow> rows, String uploadedBy
    ) {
        Department department = departmentRepository.findByCode("CSDT")
                .orElseThrow(() -> new IllegalArgumentException("Department CSDT not found."));
        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Level not found."));
        AcademicSession session = academicSessionRepository.findById(academicSessionId)
                .orElseThrow(() -> new IllegalArgumentException("Academic session not found."));

        int created = 0, skipped = 0, cgpaRecorded = 0;
        List<String> errors = new ArrayList<>();

        for (StudentRosterRow row : rows) {
            try {
                boolean exists = studentRepository.existsByMatricNumber(row.getMatricNumber());

                if (!exists) {
                    Student student = new Student();
                    student.setMatricNumber(row.getMatricNumber());
                    student.setFullName(row.getFullName().isBlank() ? "UNKNOWN - " + row.getMatricNumber() : row.getFullName());
                    student.setDepartment(department);
                    student.setProgramme(programme);
                    student.setLevel(level);
                    student.setAcademicSession(session);
                    student.setStatus("ACTIVE");
                    studentRepository.save(student);
                    created++;
                } else {
                    skipped++;
                }

                if (row.getCgpa() != null && !row.getCgpa().isBlank()) {
                    try {
                        BigDecimal cgpa = new BigDecimal(row.getCgpa().trim());
                        ReportedCgpa reported = new ReportedCgpa();
                        reported.setMatricNumber(row.getMatricNumber());
                        reported.setFullName(row.getFullName());
                        reported.setProgramme(programme);
                        reported.setLevel(level);
                        reported.setAcademicSession(session);
                        reported.setCgpa(cgpa);
                        reported.setUploadedBy(uploadedBy);
                        reportedCgpaRepository.save(reported);
                        cgpaRecorded++;
                    } catch (NumberFormatException e) {
                        errors.add(row.getMatricNumber() + ": CGPA '" + row.getCgpa() + "' is not a valid number, skipped.");
                    }
                }

            } catch (Exception e) {
                errors.add(row.getMatricNumber() + ": " + e.getMessage());
            }
        }

        return new StudentRosterSaveResult(created, skipped, cgpaRecorded, errors);
    }
}