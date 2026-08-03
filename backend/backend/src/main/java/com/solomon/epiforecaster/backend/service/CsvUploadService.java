package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.repository.RawDatasetRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Service
public class CsvUploadService {

    private final RawDatasetRepository rawDatasetRepository;
    private final CsvValidationService csvValidationService;
    private final CsvDuplicateDetectionService csvDuplicateDetectionService;
    private final CsvRowProcessor csvRowProcessor;
    private final CuratedDatasetGeneratorService curatedDatasetGeneratorService;

    public CsvUploadService(
            RawDatasetRepository rawDatasetRepository,
            CsvValidationService csvValidationService,
            CsvDuplicateDetectionService csvDuplicateDetectionService,
            CsvRowProcessor csvRowProcessor,
            CuratedDatasetGeneratorService curatedDatasetGeneratorService) {

        this.rawDatasetRepository = rawDatasetRepository;
        this.csvValidationService = csvValidationService;
        this.csvDuplicateDetectionService = csvDuplicateDetectionService;
        this.csvRowProcessor = csvRowProcessor;
        this.curatedDatasetGeneratorService = curatedDatasetGeneratorService;
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

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(file.getInputStream()))) {

            //----------------------------------------------------
            // HEADER VALIDATION
            //----------------------------------------------------

            String headerLine = reader.readLine();

            if (headerLine == null) {
                throw new RuntimeException("CSV file is empty.");
            }

            String[] headers = headerLine.split(",");

            csvValidationService.validateHeaders(dataset, headers);

            //----------------------------------------------------
            // Reset duplicate detector for this upload
            //----------------------------------------------------

            csvDuplicateDetectionService.reset();

            //----------------------------------------------------
            // Process each row
            //----------------------------------------------------

            int rowNumber = 1;

            String line;

            while ((line = reader.readLine()) != null) {

                rowNumber++;

                String[] columns = line.split(",", -1);

                csvRowProcessor.processRow(
                        dataset,
                        rowNumber,
                        headers,
                        columns
                );

            }

            //----------------------------------------------------
            // Generate curated dataset
            //----------------------------------------------------

            curatedDatasetGeneratorService.generate(dataset);

            //----------------------------------------------------
            // Mark upload completed
            //----------------------------------------------------

            dataset.setStatus("IMPORTED");
            dataset.setRemarks("Import completed successfully.");

        }

        catch (Exception ex) {

            dataset.setStatus("FAILED");
            dataset.setRemarks(ex.getMessage());

            rawDatasetRepository.save(dataset);

            throw new RuntimeException(
                    "CSV Import Failed",
                    ex
            );

        }

        rawDatasetRepository.save(dataset);

    }

}