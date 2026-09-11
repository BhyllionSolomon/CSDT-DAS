
        package com.kdu.csdtdas.backend.mapper;

import com.kdu.csdtdas.backend.dto.AcademicSessionResponse;
import com.kdu.csdtdas.backend.entity.AcademicSession;
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

