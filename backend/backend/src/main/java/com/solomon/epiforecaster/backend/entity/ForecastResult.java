package com.solomon.epiforecaster.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "forecast_result")
public class ForecastResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "forecast_run_id", nullable = false)
    private ForecastRun forecastRun;

    @ManyToOne
    @JoinColumn(name = "disease_type_id", nullable = false)
    private DiseaseType diseaseType;

    @ManyToOne
    @JoinColumn(name = "lga_id", nullable = false)
    private Lga lga;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer epiWeek;

    @Column(nullable = false)
    private Double predictedCases;

    private Double actualCases;

    private Double mae;

    private Double rmse;

    private Double mape;

    private Double smape;

    private Double r2;

    private LocalDateTime createdAt;

    public ForecastResult() {
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ForecastRun getForecastRun() {
        return forecastRun;
    }

    public DiseaseType getDiseaseType() {
        return diseaseType;
    }

    public Lga getLga() {
        return lga;
    }

    public Integer getYear() {
        return year;
    }

    public Integer getEpiWeek() {
        return epiWeek;
    }

    public Double getPredictedCases() {
        return predictedCases;
    }

    public Double getActualCases() {
        return actualCases;
    }

    public Double getMae() {
        return mae;
    }

    public Double getRmse() {
        return rmse;
    }

    public Double getMape() {
        return mape;
    }

    public Double getSmape() {
        return smape;
    }

    public Double getR2() {
        return r2;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setForecastRun(ForecastRun forecastRun) {
        this.forecastRun = forecastRun;
    }

    public void setDiseaseType(DiseaseType diseaseType) {
        this.diseaseType = diseaseType;
    }

    public void setLga(Lga lga) {
        this.lga = lga;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public void setEpiWeek(Integer epiWeek) {
        this.epiWeek = epiWeek;
    }

    public void setPredictedCases(Double predictedCases) {
        this.predictedCases = predictedCases;
    }

    public void setActualCases(Double actualCases) {
        this.actualCases = actualCases;
    }

    public void setMae(Double mae) {
        this.mae = mae;
    }

    public void setRmse(Double rmse) {
        this.rmse = rmse;
    }

    public void setMape(Double mape) {
        this.mape = mape;
    }

    public void setSmape(Double smape) {
        this.smape = smape;
    }

    public void setR2(Double r2) {
        this.r2 = r2;
    }
}