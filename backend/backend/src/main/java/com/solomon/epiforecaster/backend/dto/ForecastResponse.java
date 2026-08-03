package com.solomon.epiforecaster.backend.dto;

import java.util.List;

public class ForecastResponse {

    private String diseaseName;
    private String location;
    private List<Double> predictions;

    public ForecastResponse() {
    }

    public String getDiseaseName() {
        return diseaseName;
    }

    public void setDiseaseName(String diseaseName) {
        this.diseaseName = diseaseName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<Double> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<Double> predictions) {
        this.predictions = predictions;
    }
}