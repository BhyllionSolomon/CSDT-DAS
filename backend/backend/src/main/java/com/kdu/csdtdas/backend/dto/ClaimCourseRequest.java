package com.kdu.csdtdas.backend.dto;

public class ClaimCourseRequest {
    private Long courseId;
    private Long academicSessionId;
    private String semester;

    public ClaimCourseRequest() {}
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getAcademicSessionId() { return academicSessionId; }
    public void setAcademicSessionId(Long academicSessionId) { this.academicSessionId = academicSessionId; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
}