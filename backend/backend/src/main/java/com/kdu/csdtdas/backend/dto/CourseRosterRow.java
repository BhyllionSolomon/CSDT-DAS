package com.kdu.csdtdas.backend.dto;

public class CourseRosterRow {
    private String code;
    private String title;
    private String creditUnit;

    public CourseRosterRow() {}
    public CourseRosterRow(String code, String title, String creditUnit) {
        this.code = code; this.title = title; this.creditUnit = creditUnit;
    }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCreditUnit() { return creditUnit; }
    public void setCreditUnit(String creditUnit) { this.creditUnit = creditUnit; }
}