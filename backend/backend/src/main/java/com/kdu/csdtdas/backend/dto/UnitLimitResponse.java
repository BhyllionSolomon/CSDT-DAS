package com.kdu.csdtdas.backend.dto;

public class UnitLimitResponse {
    private Long programmeId;
    private Long levelId;
    private String semester;
    private Integer requiredUnits;

    public UnitLimitResponse() {}
    public UnitLimitResponse(Long programmeId, Long levelId, String semester, Integer requiredUnits) {
        this.programmeId = programmeId; this.levelId = levelId;
        this.semester = semester; this.requiredUnits = requiredUnits;
    }
    public Long getProgrammeId() { return programmeId; }
    public void setProgrammeId(Long programmeId) { this.programmeId = programmeId; }
    public Long getLevelId() { return levelId; }
    public void setLevelId(Long levelId) { this.levelId = levelId; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public Integer getRequiredUnits() { return requiredUnits; }
    public void setRequiredUnits(Integer requiredUnits) { this.requiredUnits = requiredUnits; }
}