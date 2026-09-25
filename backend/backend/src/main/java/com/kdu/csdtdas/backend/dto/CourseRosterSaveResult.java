package com.kdu.csdtdas.backend.dto;

import java.util.List;

public class CourseRosterSaveResult {
    private int created;
    private int linkedToProgramme;
    private List<String> errors;

    public CourseRosterSaveResult() {}
    public CourseRosterSaveResult(int created, int linkedToProgramme, List<String> errors) {
        this.created = created; this.linkedToProgramme = linkedToProgramme; this.errors = errors;
    }
    public int getCreated() { return created; }
    public void setCreated(int created) { this.created = created; }
    public int getLinkedToProgramme() { return linkedToProgramme; }
    public void setLinkedToProgramme(int linkedToProgramme) { this.linkedToProgramme = linkedToProgramme; }
    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }
}