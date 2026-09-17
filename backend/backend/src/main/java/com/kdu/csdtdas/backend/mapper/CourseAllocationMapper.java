package com.kdu.csdtdas.backend.mapper;

import com.kdu.csdtdas.backend.dto.CourseAllocationResponse;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import org.springframework.stereotype.Component;

@Component
public class CourseAllocationMapper {

    public CourseAllocationResponse toResponse(CourseAllocation allocation) {
        if (allocation == null) return null;

        CourseAllocationResponse response = new CourseAllocationResponse();
        response.setId(allocation.getId());
        response.setSemester(allocation.getSemester());
        response.setLecturerName(allocation.getLecturerName());
        response.setStatus(allocation.getStatus());
        response.setPhoneNumber(allocation.getPhoneNumber());

        if (allocation.getLecturer() != null) {
            response.setLecturerId(allocation.getLecturer().getId());
            response.setLecturerUsername(allocation.getLecturer().getUsername());
        }

        if (allocation.getCourse() != null) {
            response.setCourseId(allocation.getCourse().getId());
            response.setCourseCode(allocation.getCourse().getCode());
            response.setCourseTitle(allocation.getCourse().getTitle());
        }

        if (allocation.getAcademicSession() != null) {
            response.setAcademicSessionId(allocation.getAcademicSession().getId());
            response.setAcademicSessionName(allocation.getAcademicSession().getName());
        }

        return response;
    }
}