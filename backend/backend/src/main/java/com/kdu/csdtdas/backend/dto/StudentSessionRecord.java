package com.kdu.csdtdas.backend.dto;

public class StudentSessionRecord {
    private Long studentId;
    private String matricNumber;
    private String fullName;
    private String programmeCode;
    private String programmeName;
    private String levelCode;

    public StudentSessionRecord() {}
    public StudentSessionRecord(Long studentId, String matricNumber, String fullName, String programmeCode, String programmeName, String levelCode) {
        this.studentId = studentId; this.matricNumber = matricNumber; this.fullName = fullName;
        this.programmeCode = programmeCode; this.programmeName = programmeName; this.levelCode = levelCode;
    }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getMatricNumber() { return matricNumber; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getProgrammeCode() { return programmeCode; }
    public void setProgrammeCode(String programmeCode) { this.programmeCode = programmeCode; }
    public String getProgrammeName() { return programmeName; }
    public void setProgrammeName(String programmeName) { this.programmeName = programmeName; }
    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
}