package com.kdu.csdtdas.backend.dto;

import java.util.List;

public final class ProgressDTOs {

    private ProgressDTOs() {}

    public record CourseLine(String code, String title, int unit, Double score, String grade, boolean passed) {}

    public record SemesterPoint(
            String session, int semester, String label,
            Double gpa, Double cgpa, String degreeClass,
            int unitsOffered, int unitsPassed, int unitsFailed,
            List<CourseLine> courses
    ) {}

    public record Highlight(String code, String title, String session, int semester, Double score) {}

    public record Progress(
            String studentName, String matricNumber, String programme, String level,
            Double cgpa, String degreeClass, Integer totalUnits,
            int carryOvers, List<String> carryOverCodes,
            String trend, Double nextClassAt, String nextClassName, Double gap,
            List<SemesterPoint> semesters, List<Highlight> strongest, List<Highlight> weakest,
            List<Integer> bands, List<String> bandLabels, String note
    ) {}
}