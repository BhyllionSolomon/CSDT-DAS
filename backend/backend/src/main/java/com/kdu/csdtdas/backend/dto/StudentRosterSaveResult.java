package com.kdu.csdtdas.backend.dto;

import java.util.List;

public class StudentRosterSaveResult {
    private int studentsCreated;
    private int studentsSkipped;
    private int cgpaRecorded;
    private List<String> errors;

    public StudentRosterSaveResult() {}
    public StudentRosterSaveResult(int studentsCreated, int studentsSkipped, int cgpaRecorded, List<String> errors) {
        this.studentsCreated = studentsCreated; this.studentsSkipped = studentsSkipped;
        this.cgpaRecorded = cgpaRecorded; this.errors = errors;
    }
    public int getStudentsCreated() { return studentsCreated; }
    public void setStudentsCreated(int studentsCreated) { this.studentsCreated = studentsCreated; }
    public int getStudentsSkipped() { return studentsSkipped; }
    public void setStudentsSkipped(int studentsSkipped) { this.studentsSkipped = studentsSkipped; }
    public int getCgpaRecorded() { return cgpaRecorded; }
    public void setCgpaRecorded(int cgpaRecorded) { this.cgpaRecorded = cgpaRecorded; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}