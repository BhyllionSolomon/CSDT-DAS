package com.kdu.csdtdas.backend.dto;

public class RegistrationStatusRecord {
    private Long studentId;
    private String matricNumber;
    private String fullName;
    private String levelCode;
    private int coursesRegistered;
    private int totalUnits;

    public RegistrationStatusRecord() {}
    public RegistrationStatusRecord(Long studentId, String matricNumber, String fullName, String levelCode, int coursesRegistered, int totalUnits) {
        this.studentId = studentId; this.matricNumber = matricNumber; this.fullName = fullName;
        this.levelCode = levelCode; this.coursesRegistered = coursesRegistered; this.totalUnits = totalUnits;
    }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getMatricNumber() { return matricNumber; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }
    public int getCoursesRegistered() { return coursesRegistered; }
    public void setCoursesRegistered(int coursesRegistered) { this.coursesRegistered = coursesRegistered; }
    public int getTotalUnits() { return totalUnits; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }
}