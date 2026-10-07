package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class ReportDTOs {

    private ReportDTOs() {}

    public record CourseColumn(String code, int unit, String status) {}

    public record Totals(int tuo, int tup, int tuf, BigDecimal twgp, BigDecimal gpa) {}

    public record BroadsheetRow(
            int no,
            Long studentId,
            String fullName,
            String matricNumber,
            Map<String, BigDecimal> scores,
            Totals previous,
            Totals current,
            BigDecimal cgpa,
            String degreeClass,
            String remark
    ) {}

    public record StudentRef(Long studentId, String matricNumber, String fullName, String note) {}

    public record Broadsheet(
            String programmeName,
            String levelName,
            String sessionName,
            String semester,
            List<CourseColumn> courses,
            List<BroadsheetRow> rows,
            List<StudentRef> studentsWithoutResults
    ) {}

    public record StandingRow(
            Long studentId,
            String matricNumber,
            String fullName,
            BigDecimal cgpa,
            String degreeClass,
            String standing,
            List<String> outstandingCourses,
            Integer lastSemester
    ) {}

    public record Standing(
            String programmeName,
            String levelName,
            String sessionName,
            List<StandingRow> rows,
            List<StudentRef> studentsWithoutResults
    ) {}
}