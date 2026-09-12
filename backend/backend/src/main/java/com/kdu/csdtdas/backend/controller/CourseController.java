package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.CourseRequest;
import com.kdu.csdtdas.backend.dto.CourseResponse;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.mapper.CourseMapper;
import com.kdu.csdtdas.backend.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {

    private final CourseService courseService;
    private final CourseMapper courseMapper;

    public CourseController(
            CourseService courseService,
            CourseMapper courseMapper
    ) {
        this.courseService = courseService;
        this.courseMapper = courseMapper;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @RequestBody CourseRequest request
    ) {

        Course course = courseService.createCourse(
                request.getCode(),
                request.getTitle(),
                request.getCreditUnit(),
                request.getDepartmentId(),
                request.getLevelId(),
                request.getSemester(),
                request.getProgrammeIds()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseMapper.toResponse(course));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses() {

        List<CourseResponse> courses =
                courseService.getAllCourses()
                        .stream()
                        .map(courseMapper::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourse(
            @PathVariable Long id
    ) {

        Course course = courseService.getCourse(id);

        return ResponseEntity.ok(
                courseMapper.toResponse(course)
        );
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<CourseResponse> getCourseByCode(
            @PathVariable String code
    ) {

        Course course = courseService.getCourseByCode(code);

        return ResponseEntity.ok(
                courseMapper.toResponse(course)
        );
    }

    @GetMapping("/programme/{programmeId}")
    public ResponseEntity<List<CourseResponse>> getCoursesByProgramme(
            @PathVariable Long programmeId
    ) {

        List<CourseResponse> courses =
                courseService.getCoursesByProgramme(programmeId)
                        .stream()
                        .map(courseMapper::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(courses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long id,
            @RequestBody CourseRequest request
    ) {

        Course course = courseService.updateCourse(
                id,
                request.getTitle(),
                request.getCreditUnit(),
                request.getSemester()
        );

        return ResponseEntity.ok(
                courseMapper.toResponse(course)
        );
    }

    @PutMapping("/{id}/programmes")
    public ResponseEntity<CourseResponse> updateCourseProgrammes(
            @PathVariable Long id,
            @RequestBody CourseRequest request
    ) {

        Course course = courseService.updateCourseProgrammes(
                id,
                request.getProgrammeIds()
        );

        return ResponseEntity.ok(
                courseMapper.toResponse(course)
        );
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<CourseResponse> deactivateCourse(
            @PathVariable Long id
    ) {

        Course course = courseService.deactivateCourse(id);

        return ResponseEntity.ok(
                courseMapper.toResponse(course)
        );
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<CourseResponse> activateCourse(
            @PathVariable Long id
    ) {

        Course course = courseService.activateCourse(id);

        return ResponseEntity.ok(
                courseMapper.toResponse(course)
        );
    }
}