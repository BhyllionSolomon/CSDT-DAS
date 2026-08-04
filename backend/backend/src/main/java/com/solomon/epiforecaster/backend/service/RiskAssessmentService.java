package com.solomon.epiforecaster.backend.service;

import org.springframework.stereotype.Service;

@Service
public class RiskAssessmentService {

    public String determineRiskLevel(
            Double predictedCases,
            Double confidenceScore) {

        if (predictedCases == null) {
            return "UNKNOWN";
        }

        if (predictedCases >= 200) {
            return "CRITICAL";
        }

        if (predictedCases >= 100) {
            return "HIGH";
        }

        if (predictedCases >= 50) {
            return "MEDIUM";
        }

        return "LOW";
    }

    public String generateRecommendation(String riskLevel) {

        return switch (riskLevel) {

            case "CRITICAL" ->
                    "Immediately activate emergency response, deploy rapid response teams, initiate mass drug administration, intensify laboratory confirmation, and notify national surveillance authorities.";

            case "HIGH" ->
                    "Increase surveillance activities, expand laboratory testing, prepare treatment centers, and strengthen community awareness.";

            case "MEDIUM" ->
                    "Continue enhanced surveillance, monitor disease trends closely, and reinforce preventive interventions.";

            case "LOW" ->
                    "Maintain routine surveillance and continue preventive public health measures.";

            default ->
                    "No recommendation available.";
        };
    }

}