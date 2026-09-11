
        package com.solomon.epiforecaster.backend.mapper;

import com.solomon.epiforecaster.backend.dto.AcademicSessionResponse;
import com.solomon.epiforecaster.backend.entity.AcademicSession;
import org.springframework.stereotype.Component;

@Component
public class AcademicSessionMapper {

    public AcademicSessionResponse toResponse(
            AcademicSession academicSession
    ) {

        AcademicSessionResponse response =
                new AcademicSessionResponse();

        response.setId(academicSession.getId());
        response.setName(academicSession.getName());
        response.setStartDate(academicSession.getStartDate());
        response.setEndDate(academicSession.getEndDate());
        response.setCurrent(academicSession.getCurrent());
        response.setActive(academicSession.getActive());
        response.setCreatedAt(academicSession.getCreatedAt());
        response.setUpdatedAt(academicSession.getUpdatedAt());

        return response;
    }
}

