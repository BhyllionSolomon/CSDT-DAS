package com.solomon.epiforecaster.backend.controller;

import com.solomon.epiforecaster.backend.dto.AssessmentRequest;
import com.solomon.epiforecaster.backend.dto.AssessmentResponse;
import com.solomon.epiforecaster.backend.entity.Assessment;
import com.solomon.epiforecaster.backend.mapper.AssessmentMapper;
import com.solomon.epiforecaster.backend.service.AssessmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assessments")
@CrossOrigin(origins = "*")
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AssessmentMapper assessmentMapper;

    public AssessmentController(
            AssessmentService assessmentService,
            AssessmentMapper assessmentMapper
    ) {
        this.assessmentService = assessmentService;
        this.assessmentMapper = assessmentMapper;
    }

    @PostMapping
    public ResponseEntity<AssessmentResponse> createAssessment(
            @RequestBody AssessmentRequest request
    ) {

        Assessment assessment =
                assessmentService.createAssessment(
                        request.getCourseRegistrationId(),
                        request.getCaScore(),
                        request.getExamScore()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(assessmentMapper.toResponse(assessment));
    }

    @PostMapping("/enter-scores")
    public ResponseEntity<AssessmentResponse> enterScores(
            @RequestBody AssessmentRequest request
    ) {

        Assessment assessment =
                assessmentService.enterScores(
                        request.getCourseRegistrationId(),
                        request.getCaScore(),
                        request.getExamScore()
                );

        return ResponseEntity.ok(
                assessmentMapper.toResponse(assessment)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentResponse> getAssessment(
            @PathVariable Long id
    ) {

        Assessment assessment =
                assessmentService.getAssessment(id);

        return ResponseEntity.ok(
                assessmentMapper.toResponse(assessment)
        );
    }

    @GetMapping("/registration/{courseRegistrationId}")
    public ResponseEntity<AssessmentResponse> getAssessmentByRegistration(
            @PathVariable Long courseRegistrationId
    ) {

        Assessment assessment =
                assessmentService.getAssessmentByRegistration(
                        courseRegistrationId
                );

        return ResponseEntity.ok(
                assessmentMapper.toResponse(assessment)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssessmentResponse> updateAssessment(
            @PathVariable Long id,
            @RequestBody AssessmentRequest request
    ) {

        Assessment assessment =
                assessmentService.updateAssessment(
                        id,
                        request.getCaScore(),
                        request.getExamScore()
                );

        return ResponseEntity.ok(
                assessmentMapper.toResponse(assessment)
        );
    }
}