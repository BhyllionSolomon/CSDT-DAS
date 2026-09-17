package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Result;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.repository.ResultRepository;
import com.kdu.csdtdas.backend.repository.StudentRepository;
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
public class ResultDocxService {

    private final StudentRepository studentRepository;
    private final ResultRepository resultRepository;
    private final ResultService resultService;

    public ResultDocxService(
            StudentRepository studentRepository,
            ResultRepository resultRepository,
            ResultService resultService
    ) {
        this.studentRepository = studentRepository;
        this.resultRepository = resultRepository;
        this.resultService = resultService;
    }

    public ResultCsvService.CsvUploadResult processUpload(
            Long courseId,
            Long academicSessionId,
            String semester,
            MultipartFile file
    ) {

        String cleanSemester = semester.trim().toUpperCase();
        List<String> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;

        try (InputStream in = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(in)) {

            XWPFTable table = findScoreTable(doc);

            if (table == null) {
                throw new IllegalArgumentException("No table found in this document.");
            }

            List<XWPFTableRow> rows = table.getRows();

            if (rows.isEmpty()) {
                throw new IllegalArgumentException("Table is empty.");
            }

            int headerRowIndex = -1;
            int matricCol = -1;
            int caCol = -1;
            int examCol = -1;

            int searchLimit = Math.min(5, rows.size());

            for (int r = 0; r < searchLimit; r++) {

                List<String> headers = cellTexts(rows.get(r));
                int foundMatric = -1;
                int foundCa = -1;
                int foundExam = -1;

                for (int i = 0; i < headers.size(); i++) {
                    String h = headers.get(i).toUpperCase().trim();
                    if (h.contains("MATRIC")) foundMatric = i;
                    if (h.contains("C.A") || h.equals("CA") || h.contains("CA SCORE") || h.contains("CA(")) foundCa = i;
                    if (h.contains("70") || h.contains("EXAM SCORE") || h.equals("EXAM")) foundExam = i;
                }

                if (foundMatric != -1) {
                    headerRowIndex = r;
                    matricCol = foundMatric;
                    caCol = foundCa;
                    examCol = foundExam;
                    break;
                }
            }

            if (headerRowIndex == -1) {
                throw new IllegalArgumentException(
                        "Could not find a row containing a 'Matric' column header in the first "
                                + searchLimit + " rows of the table."
                );
            }

            if (caCol == -1 || examCol == -1) {
                List<Integer> numericColsInSomeDataRow = detectLikelyScoreColumns(
                        rows, headerRowIndex, matricCol
                );

                if (numericColsInSomeDataRow.size() >= 2) {
                    int size = numericColsInSomeDataRow.size();
                    if (examCol == -1) examCol = numericColsInSomeDataRow.get(size - 2);
                    if (caCol == -1) caCol = numericColsInSomeDataRow.get(size - 1);
                }
            }

            if (caCol == -1 || examCol == -1) {
                throw new IllegalArgumentException(
                        "Found the Matric column but could not detect CA/Exam score columns. "
                                + "Please use the CSV upload instead for this document."
                );
            }

            for (int r = headerRowIndex + 1; r < rows.size(); r++) {

                List<String> cells = cellTexts(rows.get(r));

                if (matricCol >= cells.size()) continue;

                String matricNumber = cells.get(matricCol).trim();

                if (matricNumber.isEmpty() || !matricNumber.toUpperCase().startsWith("KDU")) {
                    continue;
                }

                Double caScore = parseScore(cells, caCol);
                Double examScore = parseScore(cells, examCol);

                if (caScore == null || examScore == null) {
                    errors.add("Row for " + matricNumber + ": could not read CA/Exam score.");
                    continue;
                }

                Optional<Student> studentOpt = studentRepository.findByMatricNumber(matricNumber);

                if (studentOpt.isEmpty()) {
                    errors.add("No student found with matric number " + matricNumber);
                    continue;
                }

                Long studentId = studentOpt.get().getId();

                try {
                    Optional<Result> existing =
                            resultRepository.findByStudentIdAndCourseIdAndAcademicSessionIdAndSemester(
                                    studentId, courseId, academicSessionId, cleanSemester
                            );

                    if (existing.isPresent()) {
                        resultService.updateResult(existing.get().getId(), caScore, examScore);
                        updated++;
                    } else {
                        resultService.createResult(
                                studentId, courseId, academicSessionId, cleanSemester,
                                caScore, examScore
                        );
                        created++;
                    }

                } catch (IllegalArgumentException e) {
                    errors.add(matricNumber + ": " + e.getMessage());
                }
            }

        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Word document.", e);
        }

        return new ResultCsvService.CsvUploadResult(created, updated, errors);
    }

    /**
     * Fallback: if header keywords didn't reveal CA/Exam columns, look at the
     * first real data row and find the last two numeric columns after the
     * matric column — in these scoresheets that's reliably [examTotal, ca].
     */
    private List<Integer> detectLikelyScoreColumns(
            List<XWPFTableRow> rows, int headerRowIndex, int matricCol
    ) {
        List<Integer> numericCols = new ArrayList<>();

        for (int r = headerRowIndex + 1; r < rows.size(); r++) {
            List<String> cells = cellTexts(rows.get(r));

            if (matricCol >= cells.size()) continue;
            if (!cells.get(matricCol).trim().toUpperCase().startsWith("KDU")) continue;

            for (int c = 0; c < cells.size(); c++) {
                if (c == matricCol) continue;
                String text = cells.get(c).trim();
                if (!text.isEmpty() && isNumeric(text)) {
                    numericCols.add(c);
                }
            }
            break;
        }

        return numericCols;
    }

    private boolean isNumeric(String text) {
        try {
            Double.parseDouble(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private XWPFTable findScoreTable(XWPFDocument doc) {
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

    private Double parseScore(List<String> cells, int col) {
        if (col < 0 || col >= cells.size()) return null;
        String text = cells.get(col).trim();
        if (text.isEmpty()) return null;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}