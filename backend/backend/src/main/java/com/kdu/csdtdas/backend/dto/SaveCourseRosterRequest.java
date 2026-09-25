package com.kdu.csdtdas.backend.dto;

import java.util.List;

public class SaveCourseRosterRequest {
    private Long programmeId;
    private Long levelId;
    private String semester;
    private List<CourseRosterRow> rows;

    public SaveCourseRosterRequest() {}
    public Long getProgrammeId() { return programmeId; }
    public void setProgrammeId(Long programmeId) { this.programmeId = programmeId; }
    public Long getLevelId() { return levelId; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public List<CourseRosterRow> getRows() { return rows; }
    public void setRows(List<CourseRosterRow> rows) { this.rows = rows; }
}