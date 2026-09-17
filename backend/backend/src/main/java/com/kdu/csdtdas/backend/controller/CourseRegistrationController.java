package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.BulkRegistrationRequest;
import com.kdu.csdtdas.backend.dto.BulkRegistrationResult;
import com.kdu.csdtdas.backend.dto.CourseRegistrationRequest;
import com.kdu.csdtdas.backend.dto.CourseRegistrationResponse;
import com.kdu.csdtdas.backend.dto.CourseResponse;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseRegistration;
import com.kdu.csdtdas.backend.mapper.CourseMapper;
import com.kdu.csdtdas.backend.mapper.CourseRegistrationMapper;
import com.kdu.csdtdas.backend.service.CourseRegistrationService;
import com.kdu.csdtdas.backend.service.ResultCalculationService;
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
    private final ResultCalculationService resultCalculationService;
    private final CourseMapper courseMapper;

    public CourseRegistrationController(
            CourseRegistrationService courseRegistrationService,
            CourseRegistrationMapper courseRegistrationMapper,
            ResultCalculationService resultCalculationService,
            CourseMapper courseMapper
    ) {
        this.courseRegistrationService = courseRegistrationService;
        this.courseRegistrationMapper = courseRegistrationMapper;
        this.resultCalculationService = resultCalculationService;
        this.courseMapper = courseMapper;
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

    @GetMapping("/course/{courseId}/session/{academicSessionId}/semester/{semester}")
    public ResponseEntity<List<CourseRegistrationResponse>> getRegistrationsForCourse(
            @PathVariable Long courseId,
            @PathVariable Long academicSessionId,
            @PathVariable String semester
    ) {
        List<CourseRegistrationResponse> registrations =
                courseRegistrationService
                        .getRegistrationsForCourse(courseId, academicSessionId, semester)
                        .stream()
                        .map(courseRegistrationMapper::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(registrations);
    }

    @PostMapping("/course/{courseId}/session/{academicSessionId}/semester/{semester}/register-all")
    public ResponseEntity<Integer> registerAllMatching(
            @PathVariable Long courseId,
            @PathVariable Long academicSessionId,
            @PathVariable String semester
    ) {
        int count = courseRegistrationService.registerAllMatching(
                courseId, academicSessionId, semester
        );
        return ResponseEntity.ok(count);
    }

    @GetMapping("/student/{studentId}/outstanding-courses")
    public ResponseEntity<List<CourseResponse>> getOutstandingCourses(
            @PathVariable Long studentId
    ) {
        List<Course> outstanding = resultCalculationService.getOutstandingCourses(studentId);

        List<CourseResponse> response = outstanding.stream()
                .map(courseMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkRegistrationResult> registerMultiple(
            @RequestBody BulkRegistrationRequest request
    ) {
        BulkRegistrationResult result = courseRegistrationService.registerMultiple(
                request.getStudentId(),
                request.getAcademicSessionId(),
                request.getSemester(),
                request.getCourseIds()
        );

        return ResponseEntity.ok(result);
    }
}