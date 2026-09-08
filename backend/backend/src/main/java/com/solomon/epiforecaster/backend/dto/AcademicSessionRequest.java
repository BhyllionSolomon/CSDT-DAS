package com.solomon.epiforecaster.backend.dto;

import java.time.LocalDate;

public class AcademicSessionRequest {

    private String name;
    private LocalDate startDate;
    private LocalDate endDate;

    public AcademicSessionRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}