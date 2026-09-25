package com.kdu.csdtdas.backend.dto;

import java.util.List;

public class SaveReportedCgpaRequest {
    private Long programmeId;
    private Long levelId;
    private Long academicSessionId;
    private List<ParsedCgpaRow> rows;

    public SaveReportedCgpaRequest() {}
    public Long getProgrammeId() { return programmeId; }
    public void setProgrammeId(Long programmeId) { this.programmeId = programmeId; }
    public Long getLevelId() { return levelId; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }
    public Long getAcademicSessionId() { return academicSessionId; }
    public void setAcademicSessionId(Long academicSessionId) { this.academicSessionId = academicSessionId; }
    public List<ParsedCgpaRow> getRows() { return rows; }
    public void setRows(List<ParsedCgpaRow> rows) { this.rows = rows; }
}