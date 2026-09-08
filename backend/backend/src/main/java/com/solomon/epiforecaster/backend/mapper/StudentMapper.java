package com.solomon.epiforecaster.backend.mapper;

import com.solomon.epiforecaster.backend.dto.StudentResponse;
import com.solomon.epiforecaster.backend.entity.Student;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    public StudentResponse toResponse(Student student) {

        if (student == null) {
            return null;
        }

        StudentResponse response = new StudentResponse();

        response.setId(student.getId());
        response.setMatricNumber(student.getMatricNumber());
        response.setFullName(student.getFullName());
        response.setPhoneNumber(student.getPhoneNumber());
        response.setStatus(student.getStatus());

        if (student.getDepartment() != null) {
            response.setDepartmentId(
                    student.getDepartment().getId()
            );

            response.setDepartmentCode(
                    student.getDepartment().getCode()
            );

            response.setDepartmentName(
                    student.getDepartment().getName()
            );
        }

        if (student.getProgramme() != null) {
            response.setProgrammeId(
                    student.getProgramme().getId()
            );

            response.setProgrammeCode(
                    student.getProgramme().getCode()
            );

            response.setProgrammeName(
                    student.getProgramme().getName()
            );
        }

        if (student.getLevel() != null) {
            response.setLevelId(
                    student.getLevel().getId()
            );

            response.setLevelCode(
                    student.getLevel().getCode()
            );

            response.setLevelName(
                    student.getLevel().getName()
            );
        }

        if (student.getAcademicSession() != null) {
            response.setAcademicSessionId(
                    student.getAcademicSession().getId()
            );

            response.setAcademicSessionName(
                    student.getAcademicSession().getName()
            );
        }

        return response;
    }
}