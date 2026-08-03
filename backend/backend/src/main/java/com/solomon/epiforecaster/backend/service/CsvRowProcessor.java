package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class CsvRowProcessor {

    private final CsvValidationService csvValidationService;
    private final NumericValidationService numericValidationService;
    private final BusinessRuleValidationService businessRuleValidationService;
    private final DiseaseValidationService diseaseValidationService;
    private final GeographicValidationService geographicValidationService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final CsvDuplicateDetectionService csvDuplicateDetectionService;
    private final DiseaseRecordMapper diseaseRecordMapper;
    private final DiseaseRecordRepository diseaseRecordRepository;

    public CsvRowProcessor(
            CsvValidationService csvValidationService,
            NumericValidationService numericValidationService,
            BusinessRuleValidationService businessRuleValidationService,
            DiseaseValidationService diseaseValidationService,
            GeographicValidationService geographicValidationService,
            DuplicateDetectionService duplicateDetectionService,
            CsvDuplicateDetectionService csvDuplicateDetectionService,
            DiseaseRecordMapper diseaseRecordMapper,
            DiseaseRecordRepository diseaseRecordRepository) {

        this.csvValidationService = csvValidationService;
        this.numericValidationService = numericValidationService;
        this.businessRuleValidationService = businessRuleValidationService;
        this.diseaseValidationService = diseaseValidationService;
        this.geographicValidationService = geographicValidationService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.csvDuplicateDetectionService = csvDuplicateDetectionService;
        this.diseaseRecordMapper = diseaseRecordMapper;
        this.diseaseRecordRepository = diseaseRecordRepository;
    }

    public void processRow(
            RawDataset dataset,
            int rowNumber,
            String[] headers,
            String[] columns) {

        boolean rowValid = true;

        //--------------------------------------------------------
        // Empty Field Validation
        //--------------------------------------------------------

        for (int i = 0; i < columns.length; i++) {

            String value = columns[i].trim();

            String fieldName =
                    i < headers.length
                            ? headers[i]
                            : "Column " + (i + 1);

            boolean valid = !value.isBlank();

            if (!valid) {
                rowValid = false;
            }

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    fieldName,
                    value,
                    valid,
                    "EMPTY_VALUE",
                    "Field cannot be empty");
        }

        if (!rowValid) {
            return;
        }

        //--------------------------------------------------------
        // Numeric Validation
        //--------------------------------------------------------

        Integer year;
        Integer epiWeek;
        Integer suspected;
        Integer confirmed;
        Integer deaths;

        try {

            year = Integer.parseInt(columns[4].trim());
            epiWeek = Integer.parseInt(columns[5].trim());
            suspected = Integer.parseInt(columns[6].trim());
            confirmed = Integer.parseInt(columns[7].trim());
            deaths = Integer.parseInt(columns[8].trim());

        } catch (Exception ex) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "Numeric Fields",
                    "",
                    false,
                    "INVALID_NUMBER",
                    "One or more numeric fields are invalid.");

            return;
        }

        //--------------------------------------------------------
        // Business Rule Validation
        //--------------------------------------------------------

        if (!businessRuleValidationService.isValidEpiWeek(epiWeek)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "EpiWeek",
                    String.valueOf(epiWeek),
                    false,
                    "INVALID_EPI_WEEK",
                    "EpiWeek must be between 1 and 53.");

            return;
        }

        if (!businessRuleValidationService.isNonNegative(suspected)
                || !businessRuleValidationService.isNonNegative(confirmed)
                || !businessRuleValidationService.isNonNegative(deaths)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "Cases",
                    "",
                    false,
                    "NEGATIVE_VALUE",
                    "Case values cannot be negative.");

            return;
        }

        if (!businessRuleValidationService.confirmedNotGreaterThanSuspected(
                suspected,
                confirmed)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "ConfirmedCases",
                    String.valueOf(confirmed),
                    false,
                    "INVALID_CASE_COUNT",
                    "Confirmed cases cannot exceed suspected cases.");

            return;
        }

        if (!businessRuleValidationService.deathsNotGreaterThanConfirmed(
                deaths,
                confirmed)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "Deaths",
                    String.valueOf(deaths),
                    false,
                    "INVALID_DEATH_COUNT",
                    "Deaths cannot exceed confirmed cases.");

            return;
        }

        //--------------------------------------------------------
        // Disease Validation
        //--------------------------------------------------------

        String disease = columns[0].trim();

        if (!diseaseValidationService.isValidDisease(disease)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "Disease",
                    disease,
                    false,
                    "INVALID_DISEASE",
                    "Disease does not exist in disease_master.");

            return;
        }

        //--------------------------------------------------------
        // Geographic Validation
        //--------------------------------------------------------

        String country = columns[1].trim();
        String state = columns[2].trim();
        String lga = columns[3].trim();

        if (!geographicValidationService.isValidCountry(country)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "Country",
                    country,
                    false,
                    "INVALID_COUNTRY",
                    "Country does not exist.");

            return;
        }

        if (!geographicValidationService.isValidState(state)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "State",
                    state,
                    false,
                    "INVALID_STATE",
                    "State does not exist.");

            return;
        }

        if (!geographicValidationService.isValidLga(lga)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "LGA",
                    lga,
                    false,
                    "INVALID_LGA",
                    "LGA does not exist.");

            return;
        }

        //--------------------------------------------------------
        // Database Duplicate Validation
        //--------------------------------------------------------

        if (duplicateDetectionService.isDuplicate(
                disease,
                country,
                state,
                lga,
                year,
                epiWeek)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "Disease",
                    disease,
                    false,
                    "DATABASE_DUPLICATE",
                    "Duplicate already exists in database.");

            return;
        }

        //--------------------------------------------------------
        // Duplicate Within Uploaded CSV
        //--------------------------------------------------------

        if (csvDuplicateDetectionService.isDuplicate(
                disease,
                country,
                state,
                lga,
                year,
                epiWeek)) {

            csvValidationService.validateField(
                    dataset,
                    rowNumber,
                    "Disease",
                    disease,
                    false,
                    "CSV_DUPLICATE",
                    "Duplicate found within uploaded CSV.");

            return;
        }

        //--------------------------------------------------------
        // Save Valid Record
        //--------------------------------------------------------

        DiseaseRecord record =
                diseaseRecordMapper.map(dataset, columns);

        diseaseRecordRepository.save(record);
    }
}