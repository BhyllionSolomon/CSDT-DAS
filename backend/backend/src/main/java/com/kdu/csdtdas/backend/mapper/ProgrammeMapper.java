
        package com.kdu.csdtdas.backend.mapper;

import com.kdu.csdtdas.backend.dto.ProgrammeResponse;
import com.kdu.csdtdas.backend.entity.Programme;
import org.springframework.stereotype.Component;

@Component
public class ProgrammeMapper {

    public ProgrammeResponse toResponse(Programme programme) {

        ProgrammeResponse response = new ProgrammeResponse();

        response.setId(programme.getId());
        response.setCode(programme.getCode());
        response.setName(programme.getName());
        response.setDescription(programme.getDescription());
        response.setActive(programme.getActive());
        response.setCreatedAt(programme.getCreatedAt());
        response.setUpdatedAt(programme.getUpdatedAt());

        if (programme.getDepartment() != null) {
            response.setDepartmentId(programme.getDepartment().getId());
            response.setDepartmentCode(programme.getDepartment().getCode());
            response.setDepartmentName(programme.getDepartment().getName());
        }

        return response;
    }
}

