package com.solomon.epiforecaster.backend.dto;

public class PredictionResponse {

    private Double predictedCases;

    private Double confidenceScore;

    private String riskLevel;

    private String recommendation;

    private Integer forecastHorizon;

    private String modelVersion;

    public PredictionResponse() {
    }

    public PredictionResponse(
            Double predictedCases,
            Double confidenceScore,
            String riskLevel,
            String recommendation,
            Integer forecastHorizon,
            String modelVersion) {

        this.predictedCases = predictedCases;
        this.confidenceScore = confidenceScore;
        this.riskLevel = riskLevel;
        this.recommendation = recommendation;
        this.forecastHorizon = forecastHorizon;
        this.modelVersion = modelVersion;
    }

    public Double getPredictedCases() {
        return predictedCases;
    }

    public void setPredictedCases(Double predictedCases) {
        this.predictedCases = predictedCases;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Integer getForecastHorizon() {
        return forecastHorizon;
    }

    public void setForecastHorizon(Integer forecastHorizon) {
        this.forecastHorizon = forecastHorizon;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

}