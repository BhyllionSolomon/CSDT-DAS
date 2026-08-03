package com.solomon.epiforecaster.backend.dto;

import java.util.List;

public class PredictionResult {

    private List<Double> predictions;

    public PredictionResult() {
    }

    public List<Double> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<Double> predictions) {
        this.predictions = predictions;
    }
}