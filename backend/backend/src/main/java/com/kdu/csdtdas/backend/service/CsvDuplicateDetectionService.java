package com.kdu.csdtdas.backend.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class CsvDuplicateDetectionService {

    private final Set<String> uploadedRecords = new HashSet<>();

    public void reset() {
        uploadedRecords.clear();
    }

    public boolean isDuplicate(
            String disease,
            String country,
            String state,
            String lga,
            Integer year,
            Integer epiWeek) {

        String key =
                disease.trim().toLowerCase() + "|" +
                        country.trim().toLowerCase() + "|" +
                        state.trim().toLowerCase() + "|" +
                        lga.trim().toLowerCase() + "|" +
                        year + "|" +
                        epiWeek;

        if (uploadedRecords.contains(key)) {
            return true;
        }

        uploadedRecords.add(key);

        return false;
    }

}
