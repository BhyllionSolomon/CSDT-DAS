package com.kdu.csdtdas.backend.dto;

import java.util.List;
import java.util.Map;

public final class AnalyticsDTOs {

    private AnalyticsDTOs() {}

    public record Summary(int students, int results, double averageScore, double passRate, double averageGpa) {}

    public record CountItem(String label, long count) {}

    public record CoursePerformance(
            Long courseId, String code, String title, String level,
            int students, double average, double passRate, Map<String, Long> grades
    ) {}

    public record TrendPoint(String session, double average, double passRate, int students) {}

    public record Cohort(
            String sessionName, String programmeName, String levelName,
            Summary summary, List<CountItem> gradeDistribution, List<CountItem> gpaBands,
            List<CoursePerformance> courses, List<TrendPoint> trend
    ) {}
}