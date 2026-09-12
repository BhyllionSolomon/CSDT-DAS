package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CourseResultDTO;
import com.kdu.csdtdas.backend.dto.CumulativeSummaryDTO;
import com.kdu.csdtdas.backend.dto.SemesterResultDTO;
import com.kdu.csdtdas.backend.dto.SemesterSummaryDTO;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Generates a student's full academic broadsheet as Excel, Word, or PDF.
 * All three formats share the same section layout: one block per semester
 * (course table + semester summary + cumulative summary + remark), followed
 * by an overall final CGPA / class of degree summary.
 */
@Service
public class ExportService {

    private static final String[] COURSE_HEADERS = {
            "Course Code", "Course Title", "Unit", "Score",
            "Grade", "GP", "WGP", "Status"
    };

    // =====================================================================
    // EXCEL
    // =====================================================================

    public byte[] exportToExcel(
            List<SemesterResultDTO> history
    ) {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Academic History");

            CellStyle titleStyle = boldStyle(workbook, 14);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle labelStyle = boldStyle(workbook, 11);

            int rowIndex = 0;

            if (!history.isEmpty()) {
                SemesterResultDTO firstEntry = history.get(0);

                rowIndex = writeExcelCell(
                        sheet, rowIndex,
                        "Student: " + firstEntry.studentName()
                                + " (" + firstEntry.matricNumber() + ")",
                        titleStyle
                );
                rowIndex++;
            }

            for (SemesterResultDTO semester : history) {

                rowIndex = writeExcelCell(
                        sheet, rowIndex,
                        semester.session()
                                + " - Semester "
                                + semester.semester(),
                        labelStyle
                );

                Row headerRow = sheet.createRow(rowIndex++);
                for (int i = 0; i < COURSE_HEADERS.length; i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(COURSE_HEADERS[i]);
                    cell.setCellStyle(headerStyle);
                }

                for (CourseResultDTO course : semester.courses()) {
                    Row row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(course.courseCode());
                    row.createCell(1).setCellValue(course.courseTitle());
                    row.createCell(2).setCellValue(course.creditUnit());
                    row.createCell(3).setCellValue(
                            course.score().doubleValue()
                    );
                    row.createCell(4).setCellValue(course.letterGrade());
                    row.createCell(5).setCellValue(course.gradePoint());
                    row.createCell(6).setCellValue(
                            course.weightedGradePoint().doubleValue()
                    );
                    row.createCell(7).setCellValue(
                            course.passed() ? "PASS" : "FAIL"
                    );
                }

                rowIndex = writeSemesterSummaryExcel(
                        sheet, rowIndex, semester.semesterSummary()
                );

                rowIndex = writeCumulativeSummaryExcel(
                        sheet, rowIndex,
                        "Previous Cumulative",
                        semester.previousCumulative()
                );

                rowIndex = writeCumulativeSummaryExcel(
                        sheet, rowIndex,
                        "Current Cumulative",
                        semester.currentCumulative()
                );

                rowIndex = writeExcelCell(
                        sheet, rowIndex,
                        "Remark: " + semester.remark(),
                        labelStyle
                );

                rowIndex += 2;
            }

            if (!history.isEmpty()) {
                CumulativeSummaryDTO finalCgpa =
                        history.get(history.size() - 1)
                                .currentCumulative();

                rowIndex = writeExcelCell(
                        sheet, rowIndex,
                        "FINAL CGPA: " + finalCgpa.cgpa()
                                + " (" + finalCgpa.degreeClass() + ")",
                        titleStyle
                );
            }

            for (int i = 0; i < COURSE_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to generate Excel export.", e
            );
        }
    }

    private int writeExcelCell(
            Sheet sheet,
            int rowIndex,
            String value,
            CellStyle style
    ) {
        Row row = sheet.createRow(rowIndex);
        Cell cell = row.createCell(0);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        return rowIndex + 1;
    }

    private int writeSemesterSummaryExcel(
            Sheet sheet,
            int rowIndex,
            SemesterSummaryDTO summary
    ) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(
                "TUO: " + summary.tuo()
                        + "  TUP: " + summary.tup()
                        + "  TUF: " + summary.tuf()
                        + "  TWGP: " + summary.twgp()
                        + "  GPA: " + summary.gpa()
        );
        return rowIndex + 1;
    }

    private int writeCumulativeSummaryExcel(
            Sheet sheet,
            int rowIndex,
            String label,
            CumulativeSummaryDTO summary
    ) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(
                label
                        + " - CTU: " + summary.ctu()
                        + "  TWGP: " + summary.cumulativeTwgp()
                        + "  CGPA: " + summary.cgpa()
                        + "  Class: " + summary.degreeClass()
        );
        return rowIndex + 1;
    }

    private CellStyle boldStyle(XSSFWorkbook workbook, int fontSize) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) fontSize);
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(
                org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT
                        .getIndex()
        );
        style.setFillPattern(
                org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
        );
        return style;
    }

    // =====================================================================
    // WORD (.docx)
    // =====================================================================

    public byte[] exportToWord(
            List<SemesterResultDTO> history
    ) {

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            if (!history.isEmpty()) {
                SemesterResultDTO firstEntry = history.get(0);

                addWordHeading(
                        document,
                        "Academic History - " + firstEntry.studentName()
                                + " (" + firstEntry.matricNumber() + ")",
                        16
                );
            }

            for (SemesterResultDTO semester : history) {

                addWordHeading(
                        document,
                        semester.session()
                                + " - Semester "
                                + semester.semester(),
                        13
                );

                XWPFTable table = document.createTable(
                        semester.courses().size() + 1,
                        COURSE_HEADERS.length
                );

                XWPFTableRow headerRow = table.getRow(0);
                for (int i = 0; i < COURSE_HEADERS.length; i++) {
                    setWordCell(headerRow.getCell(i), COURSE_HEADERS[i], true);
                }

                for (int i = 0; i < semester.courses().size(); i++) {

                    CourseResultDTO course = semester.courses().get(i);
                    XWPFTableRow row = table.getRow(i + 1);

                    setWordCell(row.getCell(0), course.courseCode(), false);
                    setWordCell(row.getCell(1), course.courseTitle(), false);
                    setWordCell(row.getCell(2), String.valueOf(course.creditUnit()), false);
                    setWordCell(row.getCell(3), course.score().toPlainString(), false);
                    setWordCell(row.getCell(4), course.letterGrade(), false);
                    setWordCell(row.getCell(5), String.valueOf(course.gradePoint()), false);
                    setWordCell(row.getCell(6), course.weightedGradePoint().toPlainString(), false);
                    setWordCell(row.getCell(7), course.passed() ? "PASS" : "FAIL", false);
                }

                addWordParagraph(
                        document,
                        buildSemesterSummaryLine(semester.semesterSummary())
                );

                addWordParagraph(
                        document,
                        "Previous Cumulative - "
                                + buildCumulativeLine(semester.previousCumulative())
                );

                addWordParagraph(
                        document,
                        "Current Cumulative - "
                                + buildCumulativeLine(semester.currentCumulative())
                );

                addWordParagraph(
                        document,
                        "Remark: " + semester.remark()
                );

                document.createParagraph();
            }

            if (!history.isEmpty()) {
                CumulativeSummaryDTO finalCgpa =
                        history.get(history.size() - 1)
                                .currentCumulative();

                addWordHeading(
                        document,
                        "FINAL CGPA: " + finalCgpa.cgpa()
                                + " (" + finalCgpa.degreeClass() + ")",
                        14
                );
            }

            document.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to generate Word export.", e
            );
        }
    }

    private void addWordHeading(
            XWPFDocument document,
            String text,
            int fontSize
    ) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(fontSize);
    }

    private void addWordParagraph(
            XWPFDocument document,
            String text
    ) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(10);
    }

    private void setWordCell(
            XWPFTableCell cell,
            String text,
            boolean bold
    ) {
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(9);
    }

    // =====================================================================
    // PDF
    // =====================================================================

    public byte[] exportToPdf(
            List<SemesterResultDTO> history
    ) {

        Document document = new Document(PageSize.A4, 36, 36, 54, 54);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 14
            );
            Font headingFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 12
            );
            Font normalFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 10
            );
            Font smallFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 9
            );

            if (!history.isEmpty()) {
                SemesterResultDTO firstEntry = history.get(0);

                Paragraph title = new Paragraph(
                        "Academic History - " + firstEntry.studentName()
                                + " (" + firstEntry.matricNumber() + ")",
                        titleFont
                );
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(12);
                document.add(title);
            }

            for (SemesterResultDTO semester : history) {

                Paragraph heading = new Paragraph(
                        semester.session()
                                + " - Semester "
                                + semester.semester(),
                        headingFont
                );
                heading.setSpacingBefore(10);
                heading.setSpacingAfter(6);
                document.add(heading);

                PdfPTable table = new PdfPTable(COURSE_HEADERS.length);
                table.setWidthPercentage(100);

                for (String header : COURSE_HEADERS) {
                    PdfPCell cell = new PdfPCell(
                            new Paragraph(header, smallFont)


                    );
                    cell.setBackgroundColor(
                            new java.awt.Color(220, 220, 220)
                    );
                }

                for (CourseResultDTO course : semester.courses()) {
                    table.addCell(new Paragraph(course.courseCode(), smallFont));
                    table.addCell(new Paragraph(course.courseTitle(), smallFont));
                    table.addCell(new Paragraph(String.valueOf(course.creditUnit()), smallFont));
                    table.addCell(new Paragraph(course.score().toPlainString(), smallFont));
                    table.addCell(new Paragraph(course.letterGrade(), smallFont));
                    table.addCell(new Paragraph(String.valueOf(course.gradePoint()), smallFont));
                    table.addCell(new Paragraph(course.weightedGradePoint().toPlainString(), smallFont));
                    table.addCell(new Paragraph(course.passed() ? "PASS" : "FAIL", smallFont));
                }

                document.add(table);

                Paragraph semesterSummary = new Paragraph(
                        buildSemesterSummaryLine(semester.semesterSummary()),
                        normalFont
                );
                semesterSummary.setSpacingBefore(6);
                document.add(semesterSummary);

                document.add(new Paragraph(
                        "Previous Cumulative - "
                                + buildCumulativeLine(semester.previousCumulative()),
                        normalFont
                ));

                document.add(new Paragraph(
                        "Current Cumulative - "
                                + buildCumulativeLine(semester.currentCumulative()),
                        normalFont
                ));

                Paragraph remark = new Paragraph(
                        "Remark: " + semester.remark(),
                        normalFont
                );
                remark.setSpacingAfter(10);
                document.add(remark);
            }

            if (!history.isEmpty()) {
                CumulativeSummaryDTO finalCgpa =
                        history.get(history.size() - 1)
                                .currentCumulative();

                Paragraph finalLine = new Paragraph(
                        "FINAL CGPA: " + finalCgpa.cgpa()
                                + " (" + finalCgpa.degreeClass() + ")",
                        titleFont
                );
                finalLine.setSpacingBefore(14);
                document.add(finalLine);
            }

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate PDF export.", e
            );
        }
    }

    // =====================================================================
    // SHARED HELPERS
    // =====================================================================

    private String buildSemesterSummaryLine(SemesterSummaryDTO summary) {
        return "TUO: " + summary.tuo()
                + "  TUP: " + summary.tup()
                + "  TUF: " + summary.tuf()
                + "  TWGP: " + trim(summary.twgp())
                + "  GPA: " + trim(summary.gpa());
    }

    private String buildCumulativeLine(CumulativeSummaryDTO summary) {
        return "CTU: " + summary.ctu()
                + "  TWGP: " + trim(summary.cumulativeTwgp())
                + "  CGPA: " + trim(summary.cgpa())
                + "  Class: " + summary.degreeClass();
    }

    private String trim(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.HALF_UP)
                .toPlainString();
    }
}