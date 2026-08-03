package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

@Service
public class CuratedDatasetGeneratorService {

    private final DiseaseRecordRepository diseaseRecordRepository;

    public CuratedDatasetGeneratorService(
            DiseaseRecordRepository diseaseRecordRepository) {

        this.diseaseRecordRepository = diseaseRecordRepository;
    }

    /**
     * Existing method.
     * Returns records belonging to one uploaded dataset.
     */
    public List<DiseaseRecord> generate(RawDataset dataset) {

        return diseaseRecordRepository.findByDataset(dataset);

    }

    /**
     * Exports every validated DiseaseRecord into
     * prediction-service/datasets/curated_dataset.csv
     */
    public void generate() {

        try {

            List<DiseaseRecord> records =
                    diseaseRecordRepository.findAll();

            if (records.isEmpty()) {
                throw new RuntimeException(
                        "No disease records found in the database."
                );
            }

            // Absolute path (temporary until everything works)
            File folder = new File(
                    "E:\\GitHub\\SolomonAi_Projects\\Epidemiological-Forecaster\\prediction-service\\datasets"
            );

            if (!folder.exists()) {

                boolean created = folder.mkdirs();

                if (!created) {
                    throw new RuntimeException(
                            "Unable to create folder: "
                                    + folder.getAbsolutePath()
                    );
                }
            }

            File csv =
                    new File(folder, "curated_dataset.csv");

            try (PrintWriter writer =
                         new PrintWriter(new FileWriter(csv))) {

                writer.println(
                        "diseaseName,country,state,lga,year,epiWeek,suspectedCases,confirmedCases,deaths"
                );

                for (DiseaseRecord r : records) {

                    writer.printf(
                            "%s,%s,%s,%s,%d,%d,%d,%d,%d%n",
                            r.getDiseaseName(),
                            r.getCountry(),
                            r.getState(),
                            r.getLga(),
                            r.getYear(),
                            r.getEpiWeek(),
                            r.getSuspectedCases(),
                            r.getConfirmedCases(),
                            r.getDeaths()
                    );

                }

            }

            System.out.println("----------------------------------------");
            System.out.println("Curated dataset exported successfully.");
            System.out.println(csv.getAbsolutePath());
            System.out.println("----------------------------------------");

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new RuntimeException(
                    "Unable to generate curated dataset.",
                    ex
            );

        }

    }

}