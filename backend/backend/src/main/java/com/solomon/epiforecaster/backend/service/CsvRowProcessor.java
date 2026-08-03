package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class CsvRowProcessor {

    private final DiseaseRecordMapper diseaseRecordMapper;
    private final DiseaseRecordRepository diseaseRecordRepository;

    public CsvRowProcessor(
            DiseaseRecordMapper diseaseRecordMapper,
            DiseaseRecordRepository diseaseRecordRepository) {

        this.diseaseRecordMapper = diseaseRecordMapper;
        this.diseaseRecordRepository = diseaseRecordRepository;
    }

    public void processRow(
            RawDataset dataset,
            int rowNumber,
            String[] headers,
            String[] columns) {

        try {

            //--------------------------------------------------
            // Ignore blank rows
            //--------------------------------------------------

            if (columns.length < 9) {

                System.out.println(
                        "Skipping Row " + rowNumber +
                                " (Not enough columns)");

                return;
            }

            //--------------------------------------------------
            // Map directly
            //--------------------------------------------------

            DiseaseRecord record =
                    diseaseRecordMapper.map(dataset, columns);

            //--------------------------------------------------
            // Save directly
            //--------------------------------------------------

            diseaseRecordRepository.save(record);

            System.out.println(
                    "Row " + rowNumber + " imported.");

        }

        catch (Exception ex) {

            System.out.println(
                    "Row " + rowNumber +
                            " FAILED");

            ex.printStackTrace();

        }

    }

}