package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CourseAllocationDocxService {

    private final CourseRepository courseRepository;
    private final AcademicSessionRepository academicSessionRepository;
    private final CourseAllocationRepository allocationRepository;

    public CourseAllocationDocxService(
            CourseRepository courseRepository,
            AcademicSessionRepository academicSessionRepository,
            CourseAllocationRepository allocationRepository
    ) {
        this.courseRepository = courseRepository;
        this.academicSessionRepository = academicSessionRepository;
        this.allocationRepository = allocationRepository;
    }

    public ResultCsvService.CsvUploadResult processUpload(
            Long academicSessionId, String semester, MultipartFile file
    ) {
        String cleanSemester = semester.trim().toUpperCase();

        if (!academicSessionRepository.existsById(academicSessionId)) {
            throw new IllegalArgumentException("Academic session not found.");
        }

        List<String> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;

        try (InputStream in = file.getInputStream(); XWPFDocument doc = new XWPFDocument(in)) {

            XWPFTable table = findLargestTable(doc);
            if (table == null) {
                throw new IllegalArgumentException("No table found in this document.");
            }

            List<XWPFTableRow> rows = table.getRows();
            List<String> headers = cellTexts(rows.get(0));

            int lecturerCol = findColumn(headers, "LECTURER");
            int codeCol = findColumn(headers, "CODE");
            int titleCol = findColumn(headers, "TITLE");
            int unitCol = findColumn(headers, "UNIT");
            int statusCol = findColumn(headers, "STATUS");
            int phoneCol = findColumn(headers, "PHONE");

            if (codeCol == -1) {
                throw new IllegalArgumentException("Could not find a 'Course Code' column.");
            }

            for (int r = 1; r < rows.size(); r++) {
                List<String> cells = cellTexts(rows.get(r));
                if (codeCol >= cells.size()) continue;

                String rawCode = cells.get(codeCol).trim();
                if (rawCode.isEmpty()) continue;

                String firstCode = rawCode.split("/")[0].trim();
                String normalizedCode = firstCode.replaceAll("\\s+", "").toUpperCase();

                Optional<Course> courseOpt = courseRepository.findByCode(normalizedCode);
                if (courseOpt.isEmpty()) {
                    errors.add("Course code '" + rawCode + "' not found in the system yet "
                            + "(create it first via Create Course, then re-upload).");
                    continue;
                }

                Course course = courseOpt.get();
                String lecturerName = lecturerCol != -1 && lecturerCol < cells.size()
                        ? cells.get(lecturerCol).trim() : null;
                String status = statusCol != -1 && statusCol < cells.size()
                        ? cells.get(statusCol).trim() : null;
                String phone = phoneCol != -1 && phoneCol < cells.size()
                        ? cells.get(phoneCol).trim() : null;

                Optional<CourseAllocation> existing = allocationRepository
                        .findByCourseIdAndAcademicSessionIdAndSemester(
                                course.getId(), academicSessionId, cleanSemester
                        );

                if (existing.isPresent()) {
                    CourseAllocation allocation = existing.get();
                    // Don't overwrite a name once a real account has claimed this course.
                    if (allocation.getLecturer() == null && lecturerName != null && !lecturerName.isEmpty()) {
                        allocation.setLecturerName(lecturerName);
                    }
                    if (status != null && !status.isEmpty()) allocation.setStatus(status);
                    if (phone != null && !phone.isEmpty()) allocation.setPhoneNumber(phone);
                    allocationRepository.save(allocation);
                    updated++;
                } else {
                    AcademicSession session = academicSessionRepository.findById(academicSessionId).get();
                    CourseAllocation allocation = new CourseAllocation();
                    allocation.setCourse(course);
                    allocation.setAcademicSession(session);
                    allocation.setSemester(cleanSemester);
                    allocation.setLecturerName(lecturerName);
                    allocation.setStatus(status);
                    allocation.setPhoneNumber(phone);
                    allocationRepository.save(allocation);
                    created++;
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Word document.", e);
        }

        return new ResultCsvService.CsvUploadResult(created, updated, errors);
    }

    private int findColumn(List<String> headers, String keyword) {
        for (int i = 0; i < headers.size(); i++) {
            if (headers.get(i).toUpperCase().contains(keyword)) return i;
        }
        return -1;
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

    private List<String> cellTexts(XWPFTableRow row) {
        List<String> texts = new ArrayList<>();
        for (XWPFTableCell cell : row.getTableCells()) {
            texts.add(cell.getText());
        }
        return texts;
    }
}