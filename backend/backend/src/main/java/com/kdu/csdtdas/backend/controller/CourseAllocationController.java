package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.CourseAllocationRequest;
import com.kdu.csdtdas.backend.dto.CourseAllocationResponse;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.mapper.CourseAllocationMapper;
import com.kdu.csdtdas.backend.service.CourseAllocationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/allocations")
@CrossOrigin(origins = "*")
public class CourseAllocationController {

    private final CourseAllocationService allocationService;
    private final CourseAllocationMapper allocationMapper;

    public CourseAllocationController(
            CourseAllocationService allocationService,
            CourseAllocationMapper allocationMapper
    ) {
        this.allocationService = allocationService;
        this.allocationMapper = allocationMapper;
    }

    @PostMapping
    public ResponseEntity<CourseAllocationResponse> allocate(
            @RequestBody CourseAllocationRequest request
    ) {

        CourseAllocation allocation = allocationService.allocate(
                request.getLecturerId(),
                request.getCourseId(),
                request.getAcademicSessionId(),
                request.getSemester()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(allocationMapper.toResponse(allocation));
    }

    @GetMapping
    public List<CourseAllocationResponse> getAll() {
        return allocationService.getAll()
                .stream()
                .map(allocationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/lecturer/{lecturerId}")
    public List<CourseAllocationResponse> getAllForLecturer(
            @PathVariable Long lecturerId
    ) {
        return allocationService.getAllForLecturer(lecturerId)
                .stream()
                .map(allocationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/lecturer/{lecturerId}/session/{sessionId}/semester/{semester}")
    public List<CourseAllocationResponse> getForLecturer(
            @PathVariable Long lecturerId,
            @PathVariable Long sessionId,
            @PathVariable String semester
    ) {
        return allocationService.getForLecturer(lecturerId, sessionId, semester)
                .stream()
                .map(allocationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        allocationService.remove(id);
        return ResponseEntity.noContent().build();
    }
}