package com.solomon.epiforecaster.backend.dto;

import java.time.LocalDateTime;

public class DiseaseRecordResponse {

    private Long id;
    private String diseaseName;
    private String country;
    private String state;
    private String lga;
    private Integer year;
    private Integer epiWeek;
    private Integer suspectedCases;
    private Integer confirmedCases;
    private Integer deaths;
    private LocalDateTime createdAt;

    public DiseaseRecordResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getLga() {
        return lga;
    }

    public void setLga(String lga) {
        this.lga = lga;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getEpiWeek() {
        return epiWeek;
    }

    public void setEpiWeek(Integer epiWeek) {
        this.epiWeek = epiWeek;
    }

    public Integer getSuspectedCases() {
        return suspectedCases;
    }

    public void setSuspectedCases(Integer suspectedCases) {
        this.suspectedCases = suspectedCases;
    }

    public Integer getConfirmedCases() {
        return confirmedCases;
    }

    public void setConfirmedCases(Integer confirmedCases) {
        this.confirmedCases = confirmedCases;
    }

    public Integer getDeaths() {
        return deaths;
    }

    public void setDeaths(Integer deaths) {
        this.deaths = deaths;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}