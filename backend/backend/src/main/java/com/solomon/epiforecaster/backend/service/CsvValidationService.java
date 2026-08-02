package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.entity.ValidationResult;
import com.solomon.epiforecaster.backend.repository.ValidationResultRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CsvValidationService {

    private final ValidationResultRepository validationResultRepository;

    public CsvValidationService(
            ValidationResultRepository validationResultRepository) {

        this.validationResultRepository = validationResultRepository;
    }

    // ==========================================================
    // REQUIRED CSV HEADERS
    // ==========================================================

    private static final List<String> REQUIRED_HEADERS = Arrays.asList(
            "Disease",
            "Country",
            "State",
            "LGA",
            "Year",
            "EpiWeek",
            "SuspectedCases",
            "ConfirmedCases",
            "Deaths"
    );

    // ==========================================================
    // HEADER VALIDATION
    // ==========================================================

    public void validateHeaders(RawDataset dataset, String[] headers) {

        List<String> uploadedHeaders = Arrays.asList(headers);

        for (String requiredHeader : REQUIRED_HEADERS) {

            if (!uploadedHeaders.contains(requiredHeader)) {

                ValidationResult result = new ValidationResult();

                result.setRawDataset(dataset);
                result.setRowNumber(0);
                result.setFieldName(requiredHeader);
                result.setFieldValue("");
                result.setValidationType("MISSING_HEADER");
                result.setMessage("Missing required header: " + requiredHeader);
                result.setSeverity("ERROR");

                validationResultRepository.save(result);

            }

        }

    }

    // ==========================================================
    // FIELD VALIDATION
    // ==========================================================

    public void validateField(
            RawDataset dataset,
            int rowNumber,
            String fieldName,
            String fieldValue,
            boolean valid,
            String validationType,
            String errorMessage) {

        if (valid) {
            return;
        }

        ValidationResult result = new ValidationResult();

        result.setRawDataset(dataset);
        result.setRowNumber(rowNumber);
        result.setFieldName(fieldName);
        result.setFieldValue(fieldValue);
        result.setValidationType(validationType);
        result.setMessage(errorMessage);
        result.setSeverity("ERROR");

        validationResultRepository.save(result);

    }

}