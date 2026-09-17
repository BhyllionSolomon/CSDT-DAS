package com.kdu.csdtdas.backend.dto;

import java.util.List;

public class BulkResultRequest {

    private Long courseId;
    private Long academicSessionId;
    private String semester;
    private List<BulkResultEntry> entries;

    public BulkResultRequest() {
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getAcademicSessionId() {
        return academicSessionId;
    }

    public void setAcademicSessionId(Long academicSessionId) {
        this.academicSessionId = academicSessionId;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public List<BulkResultEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<BulkResultEntry> entries) {
        this.entries = entries;
    }
}