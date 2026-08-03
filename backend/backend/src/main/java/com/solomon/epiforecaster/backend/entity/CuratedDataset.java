package com.solomon.epiforecaster.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "curated_dataset")
public class CuratedDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String diseaseName;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String lga;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer epiWeek;

    @Column(nullable = false)
    private Integer suspectedCases;

    @Column(nullable = false)
    private Integer confirmedCases;

    @Column(nullable = false)
    private Integer deaths;

    @Column(nullable = false)
    private LocalDateTime curatedAt;

    public CuratedDataset() {
        this.curatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCuratedAt() {
        return curatedAt;
    }

    public void setCuratedAt(LocalDateTime curatedAt) {
        this.curatedAt = curatedAt;
    }
}