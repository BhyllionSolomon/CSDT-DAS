package com.solomon.epiforecaster.backend.dto;

public class ResultResponse {

    private Long id;

    private Long studentId;
    private String matricNumber;
    private String studentName;

    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private Integer creditUnit;

    private Long academicSessionId;
    private String academicSessionName;

    private String semester;

    private Double caScore;
    private Double examScore;
    private Double totalScore;

    private String grade;
    private Double gradePoint;
    private String remark;
    private String status;

    public ResultResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getMatricNumber() {
        return matricNumber;
    }

    public void setMatricNumber(String matricNumber) {
        this.matricNumber = matricNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public Integer getCreditUnit() {
        return creditUnit;
    }

    public void setCreditUnit(Integer creditUnit) {
        this.creditUnit = creditUnit;
    }

    public Long getAcademicSessionId() {
        return academicSessionId;
    }

    public void setAcademicSessionId(Long academicSessionId) {
        this.academicSessionId = academicSessionId;
    }

    public String getAcademicSessionName() {
        return academicSessionName;
    }

    public void setAcademicSessionName(String academicSessionName) {
        this.academicSessionName = academicSessionName;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Double getCaScore() {
        return caScore;
    }

    public void setCaScore(Double caScore) {
        this.caScore = caScore;
    }

    public Double getExamScore() {
        return examScore;
    }

    public void setExamScore(Double examScore) {
        this.examScore = examScore;
    }

    public Double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Double totalScore) {
        this.totalScore = totalScore;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public Double getGradePoint() {
        return gradePoint;
    }

    public void setGradePoint(Double gradePoint) {
        this.gradePoint = gradePoint;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}