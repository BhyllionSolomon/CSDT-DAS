package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ClaimCourseRequest;
import com.kdu.csdtdas.backend.dto.CourseAllocationRequest;
import com.kdu.csdtdas.backend.dto.CourseAllocationResponse;
import com.kdu.csdtdas.backend.dto.CourseResponse;
import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.mapper.CourseAllocationMapper;
import com.kdu.csdtdas.backend.mapper.CourseMapper;
import com.kdu.csdtdas.backend.service.CourseAllocationDocxService;
import com.kdu.csdtdas.backend.service.CourseAllocationService;
import com.kdu.csdtdas.backend.service.CourseService;
import com.kdu.csdtdas.backend.service.ResultCsvService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/allocations")
@CrossOrigin(origins = "*")
public class CourseAllocationController {

    private final CourseAllocationService allocationService;
    private final CourseAllocationMapper allocationMapper;
    private final CourseAllocationDocxService allocationDocxService;
    private final CourseService courseService;
    private final CourseMapper courseMapper;

    public CourseAllocationController(
            CourseAllocationService allocationService,
            CourseAllocationMapper allocationMapper,
            CourseAllocationDocxService allocationDocxService,
            CourseService courseService,
            CourseMapper courseMapper
    ) {
        this.allocationService = allocationService;
        this.allocationMapper = allocationMapper;
        this.allocationDocxService = allocationDocxService;
        this.courseService = courseService;
        this.courseMapper = courseMapper;
    }

    @PostMapping
    public ResponseEntity<CourseAllocationResponse> allocate(
            @RequestBody CourseAllocationRequest request
    ) {
        CourseAllocation allocation = allocationService.allocate(
                request.getLecturerId(), request.getCourseId(),
                request.getAcademicSessionId(), request.getSemester()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(allocationMapper.toResponse(allocation));
    }

    @PostMapping("/claim")
    public ResponseEntity<CourseAllocationResponse> claimCourse(
            @RequestBody ClaimCourseRequest request, Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be logged in to claim a course.");
        }

        CourseAllocation allocation = allocationService.claimCourse(
                authentication.getName(), request.getCourseId(),
                request.getAcademicSessionId(), request.getSemester()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(allocationMapper.toResponse(allocation));
    }

    @PostMapping("/upload-docx")
    public ResponseEntity<ResultCsvService.CsvUploadResult> uploadDocx(
            @RequestParam Long academicSessionId,
            @RequestParam String semester,
            @RequestParam MultipartFile file
    ) {
        ResultCsvService.CsvUploadResult result =
                allocationDocxService.processUpload(academicSessionId, semester, file);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/available")
    public ResponseEntity<List<CourseResponse>> getAvailableCourses(
            @RequestParam Long academicSessionId, @RequestParam String semester
    ) {
        List<Course> allCourses = courseService.getAllCourses();
        List<Course> available = allocationService.getAvailableCourses(academicSessionId, semester, allCourses);
        List<CourseResponse> response = available.stream().map(courseMapper::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public List<CourseAllocationResponse> getAll() {
        return allocationService.getAll().stream().map(allocationMapper::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/session/{academicSessionId}/semester/{semester}")
    public List<CourseAllocationResponse> getForSession(
            @PathVariable Long academicSessionId, @PathVariable String semester
    ) {
        return allocationService.getForSession(academicSessionId, semester)
                .stream().map(allocationMapper::toResponse).collect(Collectors.toList());
    }

    @GetMapping("/lecturer/{lecturerId}")
    public List<CourseAllocationResponse> getAllForLecturer(@PathVariable Long lecturerId) {
        return allocationService.getAllForLecturer(lecturerId)
                .stream().map(allocationMapper::toResponse).collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        allocationService.remove(id);
        return ResponseEntity.noContent().build();
    }
}