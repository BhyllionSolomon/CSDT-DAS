package com.kdu.csdtdas.backend.dto;

public class StudentRosterRow {
    private String matricNumber;
    private String fullName;
    private String programmeCode;
    private String levelCode;
    private String cgpa;

    public StudentRosterRow() {}
    public StudentRosterRow(String matricNumber, String fullName, String programmeCode, String levelCode, String cgpa) {
        this.matricNumber = matricNumber; this.fullName = fullName;
        this.programmeCode = programmeCode; this.levelCode = levelCode; this.cgpa = cgpa;
    }
    public String getMatricNumber() { return matricNumber; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getProgrammeCode() { return programmeCode; }
    public void setProgrammeCode(String programmeCode) { this.programmeCode = programmeCode; }
    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
    public String getCgpa() { return cgpa; }
    public void setCgpa(String cgpa) { this.cgpa = cgpa; }
}