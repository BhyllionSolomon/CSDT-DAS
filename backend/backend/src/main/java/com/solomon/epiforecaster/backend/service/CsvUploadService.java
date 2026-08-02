package com.solomon.epiforecaster.backend.service;

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

    public CsvUploadService(
            DiseaseRecordRepository diseaseRecordRepository,
            RawDatasetRepository rawDatasetRepository,
            CsvValidationService csvValidationService) {

        this.diseaseRecordRepository = diseaseRecordRepository;
        this.rawDatasetRepository = rawDatasetRepository;
        this.csvValidationService = csvValidationService;
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
                        new BufferedReader(new InputStreamReader(file.getInputStream()))
        ) {

            // =====================================================
            // STEP 1: Read and validate the CSV header
            // =====================================================

            String headerLine = reader.readLine();

            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty.");
            }

            String[] headers = headerLine.split(",");

            csvValidationService.validateHeaders(dataset, headers);

            // =====================================================
            // STEP 2: Validate each row
            // =====================================================

            int rowNumber = 1;

            String line;

            while ((line = reader.readLine()) != null) {

                rowNumber++;

                String[] columns = line.split(",", -1);

                for (int i = 0; i < columns.length; i++) {

                    String value = columns[i].trim();

                    String fieldName;

                    if (i < headers.length) {
                        fieldName = headers[i];
                    } else {
                        fieldName = "Column " + (i + 1);
                    }

                    csvValidationService.validateField(
                            dataset,
                            rowNumber,
                            fieldName,
                            value,
                            !value.isBlank(),
                            "EMPTY_VALUE",
                            "Field cannot be empty"
                    );

                }

                /*
                 * DiseaseRecord mapping will be added later.
                 */

            }

            dataset.setStatus("IMPORTED");
            dataset.setRemarks("File uploaded successfully");

        } catch (Exception e) {

            dataset.setStatus("FAILED");
            dataset.setRemarks(e.getMessage());

            rawDatasetRepository.save(dataset);

            throw new RuntimeException("CSV Import Failed", e);

        }

        rawDatasetRepository.save(dataset);

    }

}