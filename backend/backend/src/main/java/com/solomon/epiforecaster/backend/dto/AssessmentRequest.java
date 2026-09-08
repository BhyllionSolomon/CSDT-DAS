package com.solomon.epiforecaster.backend.dto;

public class AssessmentRequest {

    private Long courseRegistrationId;
    private Double caScore;
    private Double examScore;

    public AssessmentRequest() {
    }

    public Long getCourseRegistrationId() {
        return courseRegistrationId;
    }

    public void setCourseRegistrationId(Long courseRegistrationId) {
        this.courseRegistrationId = courseRegistrationId;
    }

    public Double getCaScore() {
        return caScore;
    }

    public void setCaScore(Double caScore) {
        this.caScore = caScore;
    }

    public Double getExamScore() {
        return examScore;
    }

    public void setExamScore(Double examScore) {
        this.examScore = examScore;
    }
}