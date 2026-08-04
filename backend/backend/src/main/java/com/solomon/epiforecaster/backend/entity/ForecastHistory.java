package com.solomon.epiforecaster.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "forecast_history")
public class ForecastHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String disease;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String lga;

    @Column(nullable = false)
    private Integer forecastHorizon;

    @Column(nullable = false)
    private Double predictedCases;

    @Column(nullable = false)
    private Double confidenceScore;

    @Column(nullable = false)
    private String riskLevel;

    @Column(length = 1000)
    private String recommendation;

    @Column(nullable = false)
    private String modelVersion;

    @Column(nullable = false)
    private LocalDateTime predictionDate;

    public ForecastHistory() {
        this.predictionDate = LocalDateTime.now();
    }

    // ==========================
    // ID
    // ==========================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // ==========================
    // Disease
    // ==========================

    public String getDisease() {
        return disease;
    }

    public void setDisease(String disease) {
        this.disease = disease;
    }

    // ==========================
    // Country
    // ==========================

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    // ==========================
    // State
    // ==========================

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    // ==========================
    // LGA
    // ==========================

    public String getLga() {
        return lga;
    }

    public void setLga(String lga) {
        this.lga = lga;
    }

    // ==========================
    // Forecast Horizon
    // ==========================

    public Integer getForecastHorizon() {
        return forecastHorizon;
    }

    public void setForecastHorizon(Integer forecastHorizon) {
        this.forecastHorizon = forecastHorizon;
    }

    // ==========================
    // Predicted Cases
    // ==========================

    public Double getPredictedCases() {
        return predictedCases;
    }

    public void setPredictedCases(Double predictedCases) {
        this.predictedCases = predictedCases;
    }

    // ==========================
    // Confidence Score
    // ==========================

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    // ==========================
    // Risk Level
    // ==========================

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    // ==========================
    // Recommendation
    // ==========================

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    // ==========================
    // Model Version
    // ==========================

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    // ==========================
    // Prediction Date
    // ==========================

    public LocalDateTime getPredictionDate() {
        return predictionDate;
    }

    public void setPredictionDate(LocalDateTime predictionDate) {
        this.predictionDate = predictionDate;
    }

}