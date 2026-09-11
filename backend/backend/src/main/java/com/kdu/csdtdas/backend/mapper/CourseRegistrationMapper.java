package com.kdu.csdtdas.backend.mapper;

import com.kdu.csdtdas.backend.dto.CourseRegistrationResponse;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import org.springframework.stereotype.Component;

@Component
public class CourseRegistrationMapper {

    public CourseRegistrationResponse toResponse(
            CourseRegistration registration
    ) {

        if (registration == null) {
            return null;
        }

        CourseRegistrationResponse response =
                new CourseRegistrationResponse();

        response.setId(registration.getId());
        response.setSemester(registration.getSemester());
        response.setStatus(registration.getStatus());

        if (registration.getStudent() != null) {

            response.setStudentId(
                    registration.getStudent().getId()
            );

            response.setMatricNumber(
                    registration.getStudent().getMatricNumber()
            );

            response.setStudentName(
                    registration.getStudent().getFullName()
            );
        }

        if (registration.getCourse() != null) {

            response.setCourseId(
                    registration.getCourse().getId()
            );

            response.setCourseCode(
                    registration.getCourse().getCode()
            );

            response.setCourseTitle(
                    registration.getCourse().getTitle()
            );

            response.setCreditUnit(
                    registration.getCourse().getCreditUnit()
            );
        }

        if (registration.getAcademicSession() != null) {

            response.setAcademicSessionId(
                    registration.getAcademicSession().getId()
            );

            response.setAcademicSessionName(
                    registration.getAcademicSession().getName()
            );
        }

        return response;
    }
}
