package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.entity.ValidationResult;
import com.solomon.epiforecaster.backend.repository.ValidationResultRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    // DUPLICATE DETECTION MEMORY
    // ==========================================================

    private final Set<String> uniqueRecords = new HashSet<>();

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

    // ==========================================================
    // DUPLICATE RECORD VALIDATION
    // ==========================================================

    public void validateDuplicateRecord(
            RawDataset dataset,
            int rowNumber,
            String disease,
            String country,
            String state,
            String lga,
            String year,
            String epiWeek) {

        String recordKey =
                disease.trim().toLowerCase() + "|" +
                        country.trim().toLowerCase() + "|" +
                        state.trim().toLowerCase() + "|" +
                        lga.trim().toLowerCase() + "|" +
                        year.trim() + "|" +
                        epiWeek.trim();

        if (uniqueRecords.contains(recordKey)) {

            ValidationResult result = new ValidationResult();

            result.setRawDataset(dataset);
            result.setRowNumber(rowNumber);
            result.setFieldName("ROW");
            result.setFieldValue(recordKey);
            result.setValidationType("DUPLICATE_RECORD");
            result.setMessage("Duplicate disease record detected.");
            result.setSeverity("ERROR");

            validationResultRepository.save(result);

        } else {

            uniqueRecords.add(recordKey);

        }

    }

    // ==========================================================
    // RESET BEFORE EVERY NEW FILE
    // ==========================================================

    public void resetDuplicateDetector() {
        uniqueRecords.clear();
    }

}