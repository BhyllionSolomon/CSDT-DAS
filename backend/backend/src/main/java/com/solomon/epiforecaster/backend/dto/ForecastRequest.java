package com.solomon.epiforecaster.backend.dto;

public class ForecastRequest {

    private String diseaseName;
    private String country;
    private String state;
    private String lga;
    private Integer forecastWeeks;

    public ForecastRequest() {
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

    public Integer getForecastWeeks() {
        return forecastWeeks;
    }

    public void setForecastWeeks(Integer forecastWeeks) {
        this.forecastWeeks = forecastWeeks;
    }
}