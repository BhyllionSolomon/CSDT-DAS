package com.kdu.csdtdas.backend.dto;

import java.util.List;

public class SaveStudentRosterRequest {
    private Long programmeId;
    private Long levelId;
    private Long academicSessionId;
    private List<StudentRosterRow> rows;

    public SaveStudentRosterRequest() {}
    public Long getProgrammeId() { return programmeId; }
    public void setProgrammeId(Long programmeId) { this.programmeId = programmeId; }
    public Long getLevelId() { return levelId; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }
    public Long getAcademicSessionId() { return academicSessionId; }
    public void setAcademicSessionId(Long academicSessionId) { this.academicSessionId = academicSessionId; }
    public List<StudentRosterRow> getRows() { return rows; }
    public void setRows(List<StudentRosterRow> rows) { this.rows = rows; }
}