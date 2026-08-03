package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.entity.RawDataset;
import com.solomon.epiforecaster.backend.repository.DiseaseRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CuratedDatasetGeneratorService {

    private final DiseaseRecordRepository diseaseRecordRepository;

    public CuratedDatasetGeneratorService(
            DiseaseRecordRepository diseaseRecordRepository) {

        this.diseaseRecordRepository = diseaseRecordRepository;
    }

    /**
     * Returns every VALID disease record imported from one upload.
     */
    public List<DiseaseRecord> generate(RawDataset dataset) {

        return diseaseRecordRepository.findByDataset(dataset);

    }

}