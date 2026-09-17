package com.kdu.csdtdas.backend.dto;

import java.util.List;

public class BulkRegistrationRequest {
    private Long studentId;
    private Long academicSessionId;
    private String semester;
    private List<Long> courseIds;

    public BulkRegistrationRequest() {}
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getAcademicSessionId() { return academicSessionId; }
    public void setAcademicSessionId(Long academicSessionId) { this.academicSessionId = academicSessionId; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public List<Long> getCourseIds() { return courseIds; }
    public void setCourseIds(List<Long> courseIds) { this.courseIds = courseIds; }
}