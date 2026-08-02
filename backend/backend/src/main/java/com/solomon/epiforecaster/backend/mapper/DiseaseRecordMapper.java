package com.solomon.epiforecaster.backend.mapper;

import com.solomon.epiforecaster.backend.dto.DiseaseRecordRequest;
import com.solomon.epiforecaster.backend.dto.DiseaseRecordResponse;
import com.solomon.epiforecaster.backend.entity.DiseaseRecord;

public class DiseaseRecordMapper {

    public static DiseaseRecord toEntity(DiseaseRecordRequest request) {

        DiseaseRecord entity = new DiseaseRecord();

        entity.setDiseaseName(request.getDiseaseName());
        entity.setCountry(request.getCountry());
        entity.setState(request.getState());
        entity.setLga(request.getLga());
        entity.setYear(request.getYear());
        entity.setEpiWeek(request.getEpiWeek());
        entity.setSuspectedCases(request.getSuspectedCases());
        entity.setConfirmedCases(request.getConfirmedCases());
        entity.setDeaths(request.getDeaths());

        return entity;
    }

    public static DiseaseRecordResponse toResponse(DiseaseRecord entity) {

        DiseaseRecordResponse response = new DiseaseRecordResponse();

        response.setId(entity.getId());
        response.setDiseaseName(entity.getDiseaseName());
        response.setCountry(entity.getCountry());
        response.setState(entity.getState());
        response.setLga(entity.getLga());
        response.setYear(entity.getYear());
        response.setEpiWeek(entity.getEpiWeek());
        response.setSuspectedCases(entity.getSuspectedCases());
        response.setConfirmedCases(entity.getConfirmedCases());
        response.setDeaths(entity.getDeaths());
        response.setCreatedAt(entity.getCreatedAt());

        return response;
    }
}