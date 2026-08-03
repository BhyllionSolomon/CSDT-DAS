package com.solomon.epiforecaster.backend.service;

import com.solomon.epiforecaster.backend.entity.DiseaseRecord;
import com.solomon.epiforecaster.backend.entity.RawDataset;
import org.springframework.stereotype.Service;

@Service
public class DiseaseRecordMapper {

    public DiseaseRecord map(
            RawDataset dataset,
            String[] columns) {

        DiseaseRecord record = new DiseaseRecord();

        record.setDataset(dataset);

        record.setDiseaseName(columns[0].trim());
        record.setCountry(columns[1].trim());
        record.setState(columns[2].trim());
        record.setLga(columns[3].trim());

        record.setYear(Integer.parseInt(columns[4].trim()));
        record.setEpiWeek(Integer.parseInt(columns[5].trim()));

        record.setSuspectedCases(Integer.parseInt(columns[6].trim()));
        record.setConfirmedCases(Integer.parseInt(columns[7].trim()));
        record.setDeaths(Integer.parseInt(columns[8].trim()));

        return record;
    }
}