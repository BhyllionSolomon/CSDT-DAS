package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import com.solomon.epiforecaster.backend.repository.RawDatasetRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class CsvUploadService {

    private final DiseaseRecordRepository diseaseRecordRepository;
    private final RawDatasetRepository rawDatasetRepository;
    private final CsvValidationService csvValidationService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final CsvDuplicateDetectionService csvDuplicateDetectionService;
    private final DiseaseRecordMapper diseaseRecordMapper;
    private final NumericValidationService numericValidationService;

    public CsvUploadService(
            DiseaseRecordRepository diseaseRecordRepository,
            RawDatasetRepository rawDatasetRepository,
            CsvValidationService csvValidationService,
            DuplicateDetectionService duplicateDetectionService,
            CsvDuplicateDetectionService csvDuplicateDetectionService,
            DiseaseRecordMapper diseaseRecordMapper,
            NumericValidationService numericValidationService) {

        this.diseaseRecordRepository = diseaseRecordRepository;
        this.rawDatasetRepository = rawDatasetRepository;
        this.csvValidationService = csvValidationService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.csvDuplicateDetectionService = csvDuplicateDetectionService;
        this.diseaseRecordMapper = diseaseRecordMapper;
        this.numericValidationService = numericValidationService;
    }

    public void importCsv(MultipartFile file) {

        RawDataset dataset = new RawDataset();

        dataset.setFilename(file.getOriginalFilename());
        dataset.setFileSize(file.getSize());
        dataset.setFileType(file.getContentType());
        dataset.setSource("Manual Upload");
        dataset.setStatus("UPLOADED");
        dataset.setRemarks("Awaiting validation");

        rawDatasetRepository.save(dataset);

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(file.getInputStream()))
        ) {

            // =====================================================
            // STEP 1 : HEADER VALIDATION
            // =====================================================

            String headerLine = reader.readLine();

            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty.");
            }

            String[] headers = headerLine.split(",");

            csvValidationService.validateHeaders(dataset, headers);

            // Reset uploaded-file duplicate detector
            csvDuplicateDetectionService.reset();

            // =====================================================
            // STEP 2 : PROCESS EACH ROW
            // =====================================================

            int rowNumber = 1;

            String line;

            while ((line = reader.readLine()) != null) {

                rowNumber++;

                String[] columns = line.split(",", -1);

                boolean rowValid = true;

                // =================================================
                // EMPTY FIELD VALIDATION
                // =================================================

                for (int i = 0; i < columns.length; i++) {

                    String value = columns[i].trim();

                    String fieldName =
                            (i < headers.length)
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
                            "Field cannot be empty"
                    );

                }

                // =================================================
                // NUMERIC VALIDATION
                // =================================================

                if (rowValid && columns.length >= 9) {

                    String[] numericFields = {
                            columns[4],
                            columns[5],
                            columns[6],
                            columns[7],
                            columns[8]
                    };

                    String[] fieldNames = {
                            "Year",
                            "EpiWeek",
                            "SuspectedCases",
                            "ConfirmedCases",
                            "Deaths"
                    };

                    for (int i = 0; i < numericFields.length; i++) {

                        boolean valid =
                                numericValidationService.isInteger(
                                        numericFields[i]
                                );

                        if (!valid) {
                            rowValid = false;
                        }

                        csvValidationService.validateField(
                                dataset,
                                rowNumber,
                                fieldNames[i],
                                numericFields[i],
                                valid,
                                "INVALID_NUMBER",
                                fieldNames[i] + " must be a valid integer."
                        );

                    }

                }

                if (!rowValid) {
                    continue;
                }

                // =================================================
                // Parse values once
                // =================================================

                String disease = columns[0].trim();
                String country = columns[1].trim();
                String state = columns[2].trim();
                String lga = columns[3].trim();

                Integer year = Integer.parseInt(columns[4].trim());
                Integer epiWeek = Integer.parseInt(columns[5].trim());

                // =================================================
                // DATABASE DUPLICATE
                // =================================================

                boolean duplicateInDatabase =
                        duplicateDetectionService.isDuplicate(
                                disease,
                                country,
                                state,
                                lga,
                                year,
                                epiWeek
                        );

                if (duplicateInDatabase) {

                    csvValidationService.validateField(
                            dataset,
                            rowNumber,
                            "Disease",
                            disease,
                            false,
                            "DATABASE_DUPLICATE",
                            "Duplicate disease record already exists."
                    );

                    continue;

                }

                // =================================================
                // DUPLICATE INSIDE CURRENT CSV
                // =================================================

                boolean duplicateInCsv =
                        csvDuplicateDetectionService.isDuplicate(
                                disease,
                                country,
                                state,
                                lga,
                                year,
                                epiWeek
                        );

                if (duplicateInCsv) {

                    csvValidationService.validateField(
                            dataset,
                            rowNumber,
                            "Disease",
                            disease,
                            false,
                            "CSV_DUPLICATE",
                            "Duplicate record found within uploaded CSV."
                    );

                    continue;

                }

                // =================================================
                // SAVE VALID RECORD
                // =================================================

                DiseaseRecord record =
                        diseaseRecordMapper.map(columns);

                diseaseRecordRepository.save(record);

            }

            dataset.setStatus("IMPORTED");
            dataset.setRemarks("Import completed successfully.");

        }

        catch (Exception e) {

            dataset.setStatus("FAILED");
            dataset.setRemarks(e.getMessage());

            rawDatasetRepository.save(dataset);

            throw new RuntimeException("CSV Import Failed", e);

        }

        rawDatasetRepository.save(dataset);

    }

}