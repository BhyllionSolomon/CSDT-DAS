package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.CourseRegistrationRequest;
import com.kdu.csdtdas.backend.dto.CourseRegistrationResponse;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.mapper.CourseRegistrationMapper;
import com.kdu.csdtdas.backend.service.CourseRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/course-registrations")
@CrossOrigin(origins = "*")
public class CourseRegistrationController {

    private final CourseRegistrationService courseRegistrationService;
    private final CourseRegistrationMapper courseRegistrationMapper;

    public CourseRegistrationController(
            CourseRegistrationService courseRegistrationService,
            CourseRegistrationMapper courseRegistrationMapper
    ) {
        this.courseRegistrationService = courseRegistrationService;
        this.courseRegistrationMapper = courseRegistrationMapper;
    }

    @PostMapping
    public ResponseEntity<CourseRegistrationResponse> registerCourse(
            @RequestBody CourseRegistrationRequest request
    ) {

        CourseRegistration registration =
                courseRegistrationService.registerCourse(
                        request.getStudentId(),
                        request.getCourseId(),
                        request.getAcademicSessionId(),
                        request.getSemester()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseRegistrationMapper.toResponse(registration));
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<CourseRegistrationResponse>> getStudentRegistrations(
            @PathVariable Long studentId
    ) {

        List<CourseRegistrationResponse> registrations =
                courseRegistrationService
                        .getStudentRegistrations(studentId)
                        .stream()
                        .map(courseRegistrationMapper::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(registrations);
    }

    @GetMapping("/student/{studentId}/session/{academicSessionId}")
    public ResponseEntity<List<CourseRegistrationResponse>> getStudentRegistrationsForSession(
            @PathVariable Long studentId,
            @PathVariable Long academicSessionId
    ) {

        List<CourseRegistrationResponse> registrations =
                courseRegistrationService
                        .getStudentRegistrationsForSession(
                                studentId,
                                academicSessionId
                        )
                        .stream()
                        .map(courseRegistrationMapper::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(registrations);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelRegistration(
            @PathVariable Long id
    ) {

        courseRegistrationService.cancelRegistration(id);

        return ResponseEntity.noContent().build();
    }
}
