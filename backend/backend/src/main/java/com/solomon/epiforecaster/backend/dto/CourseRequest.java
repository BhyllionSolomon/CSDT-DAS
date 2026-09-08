package com.solomon.epiforecaster.backend.dto;

public class CourseRequest {

    private String code;
    private String title;
    private Integer creditUnit;
    private Long departmentId;
    private Long levelId;
    private String semester;

    public CourseRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getCreditUnit() {
        return creditUnit;
    }

    public void setCreditUnit(Integer creditUnit) {
        this.creditUnit = creditUnit;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getLevelId() {
        return levelId;
    }

    public void setLevelId(Long levelId) {
        this.levelId = levelId;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }
}