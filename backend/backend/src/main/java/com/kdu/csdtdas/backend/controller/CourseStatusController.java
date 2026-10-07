package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.repository.CourseRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseStatusController {

    private final CourseRepository courseRepository;

    public CourseStatusController(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<Map<String, String>> setStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        String status = body.get("status") == null ? "" : body.get("status").trim().toUpperCase();

        if (!Set.of("C", "R", "E").contains(status)) {
            throw new IllegalArgumentException("Status must be C (Compulsory), R (Required) or E (Elective).");
        }

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));

        course.setStatus(status);
        courseRepository.save(course);

        return ResponseEntity.ok(Map.of("status", status));
    }
}