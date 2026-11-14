package com.kdu.csdtdas.backend.dto;

import java.util.List;

public final class LecturerPerformanceDTOs {

    private LecturerPerformanceDTOs() {}

    public record CourseStat(
            Long courseId, String code, String title, int unit, String semester,
            int registered, int resulted, Integer completion,
            Double average, Double passRate, Double highest, Double lowest, List<Integer> bands
    ) {}

    public record TrendPoint(String session, int students, Double average, Double passRate) {}

    public record LecturerStat(
            Long lecturerId, String name, int students, Double average, Double passRate, Integer completion,
            List<CourseStat> courses, List<TrendPoint> trend
    ) {}

    public record Report(
            Long sessionId, String sessionName, String semester,
            int departmentStudents, Double departmentAverage, Double departmentPassRate,
            List<String> bandLabels, List<LecturerStat> lecturers, String note
    ) {}
}