package com.kdu.csdtdas.backend.mapper;

import com.kdu.csdtdas.backend.dto.CourseResponse;
import com.kdu.csdtdas.backend.entity.Course;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper {

    public CourseResponse toResponse(Course course) {

        if (course == null) {
            return null;
        }

        CourseResponse response = new CourseResponse();

        response.setId(course.getId());
        response.setCode(course.getCode());
        response.setTitle(course.getTitle());
        response.setCreditUnit(course.getCreditUnit());
        response.setSemester(course.getSemester());
        response.setActive(course.getActive());

        if (course.getDepartment() != null) {
            response.setDepartmentId(
                    course.getDepartment().getId()
            );

            response.setDepartmentCode(
                    course.getDepartment().getCode()
            );

            response.setDepartmentName(
                    course.getDepartment().getName()
            );
        }

        if (course.getLevel() != null) {
            response.setLevelId(
                    course.getLevel().getId()
            );

            response.setLevelCode(
                    course.getLevel().getCode()
            );

            response.setLevelName(
                    course.getLevel().getName()
            );
        }

        return response;
    }
}
