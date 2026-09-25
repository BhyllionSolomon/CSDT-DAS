package com.kdu.csdtdas.backend.dto;

import java.util.List;
import java.util.Map;

public class ResultAnalytics {
    private double averageScore;
    private Map<String, Long> gradeDistribution;
    private List<SelfReportedResultRow> weakCourses;
    private List<SelfReportedResultRow> strongCourses;
    private String advice;

    public ResultAnalytics() {}
    public double getAverageScore() { return averageScore; }
    public void setAverageScore(double averageScore) { this.averageScore = averageScore; }
    public Map<String, Long> getGradeDistribution() { return gradeDistribution; }
    public void setGradeDistribution(Map<String, Long> gradeDistribution) { this.gradeDistribution = gradeDistribution; }
    public List<SelfReportedResultRow> getWeakCourses() { return weakCourses; }
    public void setWeakCourses(List<SelfReportedResultRow> weakCourses) { this.weakCourses = weakCourses; }
    public List<SelfReportedResultRow> getStrongCourses() { return strongCourses; }
    public void setStrongCourses(List<SelfReportedResultRow> strongCourses) { this.strongCourses = strongCourses; }
    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
}