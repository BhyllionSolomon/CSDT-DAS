package com.kdu.csdtdas.backend.dto;

public class SelfReportedResultRow {
    private String courseCode;
    private String courseTitle;
    private String totalScore;
    private String grade;
    private String semester;
    private String sessionLabel;

    public SelfReportedResultRow() {}
    public SelfReportedResultRow(String courseCode, String courseTitle, String totalScore, String grade, String semester, String sessionLabel) {
        this.courseCode = courseCode; this.courseTitle = courseTitle; this.totalScore = totalScore;
        this.grade = grade; this.semester = semester; this.sessionLabel = sessionLabel;
    }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public String getTotalScore() { return totalScore; }
    public void setTotalScore(String totalScore) { this.totalScore = totalScore; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getSessionLabel() { return sessionLabel; }
    public void setSessionLabel(String sessionLabel) { this.sessionLabel = sessionLabel; }
}