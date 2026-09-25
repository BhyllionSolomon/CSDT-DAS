package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CourseRosterRow;
import com.kdu.csdtdas.backend.dto.CourseRosterSaveResult;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.Department;
import com.kdu.csdtdas.backend.entity.Level;
import com.kdu.csdtdas.backend.entity.Programme;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import com.kdu.csdtdas.backend.repository.DepartmentRepository;
import com.kdu.csdtdas.backend.repository.LevelRepository;
import com.kdu.csdtdas.backend.repository.ProgrammeRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class CourseRosterUploadService {

    private final CourseRepository courseRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgrammeRepository programmeRepository;
    private final LevelRepository levelRepository;

    public CourseRosterUploadService(
            CourseRepository courseRepository,
            DepartmentRepository departmentRepository,
            ProgrammeRepository programmeRepository,
            LevelRepository levelRepository
    ) {
        this.courseRepository = courseRepository;
        this.departmentRepository = departmentRepository;
        this.programmeRepository = programmeRepository;
        this.levelRepository = levelRepository;
    }

    @Transactional(readOnly = true)
    public List<CourseRosterRow> extractRoster(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) return extractFromExcel(file);
        return extractFromCsv(file);
    }

    private List<CourseRosterRow> extractFromExcel(MultipartFile file) {
        List<CourseRosterRow> rows = new ArrayList<>();
        try (var in = file.getInputStream(); Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());

            int codeCol = -1, titleCol = -1, unitCol = -1;
            for (Cell cell : headerRow) {
                String h = formatter.formatCellValue(cell).toUpperCase();
                int idx = cell.getColumnIndex();
                if (h.contains("CODE")) codeCol = idx;
                else if (h.contains("TITLE") || h.contains("NAME")) titleCol = idx;
                else if (h.contains("UNIT")) unitCol = idx;
            }
            if (codeCol == -1) throw new IllegalArgumentException("Could not find a 'Course Code' column.");

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String code = getCell(row, codeCol, formatter);
                if (code.isBlank()) continue;
                rows.add(new CourseRosterRow(code.trim().toUpperCase().replaceAll("\\s+", ""),
                        getCell(row, titleCol, formatter), getCell(row, unitCol, formatter)));
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

    private List<CourseRosterRow> extractFromCsv(MultipartFile file) {
        List<CourseRosterRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("File is empty.");
            List<String> headers = splitCsv(headerLine);

            int codeCol = -1, titleCol = -1, unitCol = -1;
            for (int i = 0; i < headers.size(); i++) {
                String h = headers.get(i).toUpperCase();
                if (h.contains("CODE")) codeCol = i;
                else if (h.contains("TITLE") || h.contains("NAME")) titleCol = i;
                else if (h.contains("UNIT")) unitCol = i;
            }
            if (codeCol == -1) throw new IllegalArgumentException("Could not find a 'Course Code' column.");

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                List<String> cells = splitCsv(line);
                String code = codeCol < cells.size() ? cells.get(codeCol).trim() : "";
                if (code.isEmpty()) continue;
                rows.add(new CourseRosterRow(
                        code.toUpperCase().replaceAll("\\s+", ""),
                        titleCol != -1 && titleCol < cells.size() ? cells.get(titleCol).trim() : "",
                        unitCol != -1 && unitCol < cells.size() ? cells.get(unitCol).trim() : ""
                ));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read CSV file.", e);
        }
        return rows;
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

    public CourseRosterSaveResult saveRoster(
            Long programmeId, Long levelId, String semester, List<CourseRosterRow> rows
    ) {
        Department department = departmentRepository.findByCode("CSDT")
                .orElseThrow(() -> new IllegalArgumentException("Department CSDT not found."));
        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new IllegalArgumentException("Programme not found."));
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new IllegalArgumentException("Level not found."));
        String cleanSemester = semester.trim().toUpperCase();

        int created = 0, linked = 0;
        List<String> errors = new ArrayList<>();

        for (CourseRosterRow row : rows) {
            try {
                Optional<Course> existing = courseRepository.findByCode(row.getCode());

                if (existing.isPresent()) {
                    Course course = existing.get();
                    Set<Programme> programmes = new HashSet<>(course.getProgrammes());
                    if (programmes.stream().noneMatch(p -> p.getId().equals(programmeId))) {
                        programmes.add(programme);
                        course.setProgrammes(programmes);
                        courseRepository.save(course);
                        linked++;
                    }
                } else {
                    int unit = 3;
                    try { unit = Integer.parseInt(row.getCreditUnit().trim()); } catch (Exception ignored) {}

                    Course course = new Course();
                    course.setCode(row.getCode());
                    course.setTitle(row.getTitle().isBlank() ? row.getCode() : row.getTitle());
                    course.setCreditUnit(unit);
                    course.setDepartment(department);
                    course.setLevel(level);
                    course.setSemester(cleanSemester);
                    course.setActive(true);
                    Set<Programme> programmes = new HashSet<>();
                    programmes.add(programme);
                    course.setProgrammes(programmes);
                    courseRepository.save(course);
                    created++;
                }
            } catch (Exception e) {
                errors.add(row.getCode() + ": " + e.getMessage());
            }
        }

        return new CourseRosterSaveResult(created, linked, errors);
    }
}