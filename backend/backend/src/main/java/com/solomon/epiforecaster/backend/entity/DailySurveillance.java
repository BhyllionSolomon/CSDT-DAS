package com.solomon.epiforecaster.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_surveillance")
public class DailySurveillance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //------------------------------------------------
    // General Information
    //------------------------------------------------

    @Column(nullable = false)
    private String disease;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String lga;

    @Column(nullable = false)
    private String facility;

    @Column(nullable = false)
    private String reportingOfficer;

    @Column(nullable = false)
    private LocalDate reportDate;

    @Column(nullable = false)
    private Integer epiWeek;

    //------------------------------------------------
    // Case Information
    //------------------------------------------------

    @Column(nullable = false)
    private Integer suspectedCases;

    @Column(nullable = false)
    private Integer confirmedCases;

    @Column(nullable = false)
    private Integer deaths;

    @Column(nullable = false)
    private Integer recovered;

    //------------------------------------------------
    // Demographics
    //------------------------------------------------

    private Integer male;

    private Integer female;

    private Integer children;

    private Integer adults;

    private Integer pregnantWomen;

    //------------------------------------------------
    // Laboratory
    //------------------------------------------------

    private Integer samplesCollected;

    private Integer positiveSamples;

    private Integer negativeSamples;

    private Integer pendingSamples;

    //------------------------------------------------
    // Environmental Variables
    //------------------------------------------------

    private Double rainfall;

    private Double temperature;

    private Double humidity;

    //------------------------------------------------
    // Metadata
    //------------------------------------------------

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public DailySurveillance() {
        this.createdAt = LocalDateTime.now();
    }

    //------------------------------------------------
    // Getters and Setters
    //------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
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

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public String getReportingOfficer() {
        return reportingOfficer;
    }

    public void setReportingOfficer(String reportingOfficer) {
        this.reportingOfficer = reportingOfficer;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
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

    public Integer getRecovered() {
        return recovered;
    }

    public void setRecovered(Integer recovered) {
        this.recovered = recovered;
    }

    public Integer getMale() {
        return male;
    }

    public void setMale(Integer male) {
        this.male = male;
    }

    public Integer getFemale() {
        return female;
    }

    public void setFemale(Integer female) {
        this.female = female;
    }

    public Integer getChildren() {
        return children;
    }

    public void setChildren(Integer children) {
        this.children = children;
    }

    public Integer getAdults() {
        return adults;
    }

    public void setAdults(Integer adults) {
        this.adults = adults;
    }

    public Integer getPregnantWomen() {
        return pregnantWomen;
    }

    public void setPregnantWomen(Integer pregnantWomen) {
        this.pregnantWomen = pregnantWomen;
    }

    public Integer getSamplesCollected() {
        return samplesCollected;
    }

    public void setSamplesCollected(Integer samplesCollected) {
        this.samplesCollected = samplesCollected;
    }

    public Integer getPositiveSamples() {
        return positiveSamples;
    }

    public void setPositiveSamples(Integer positiveSamples) {
        this.positiveSamples = positiveSamples;
    }

    public Integer getNegativeSamples() {
        return negativeSamples;
    }

    public void setNegativeSamples(Integer negativeSamples) {
        this.negativeSamples = negativeSamples;
    }

    public Integer getPendingSamples() {
        return pendingSamples;
    }

    public void setPendingSamples(Integer pendingSamples) {
        this.pendingSamples = pendingSamples;
    }

    public Double getRainfall() {
        return rainfall;
    }

    public void setRainfall(Double rainfall) {
        this.rainfall = rainfall;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}