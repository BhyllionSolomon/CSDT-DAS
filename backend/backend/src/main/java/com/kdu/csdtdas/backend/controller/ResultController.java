package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ResultRequest;
import com.kdu.csdtdas.backend.dto.ResultResponse;
import com.kdu.csdtdas.backend.entity.Result;
import com.kdu.csdtdas.backend.mapper.ResultMapper;
import com.kdu.csdtdas.backend.service.ResultService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/results")
@CrossOrigin(origins = "*")
public class ResultController {

    private final ResultService resultService;
    private final ResultMapper resultMapper;

    public ResultController(
            ResultService resultService,
            ResultMapper resultMapper
    ) {
        this.resultService = resultService;
        this.resultMapper = resultMapper;
    }

    @PostMapping
    public ResponseEntity<ResultResponse> createResult(
            @RequestBody ResultRequest request
    ) {

        Result result = resultService.createResult(
                request.getStudentId(),
                request.getCourseId(),
                request.getAcademicSessionId(),
                request.getSemester(),
                request.getCaScore(),
                request.getExamScore()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resultMapper.toResponse(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResultResponse> getResult(
            @PathVariable Long id
    ) {

        Result result = resultService.getResult(id);

        return ResponseEntity.ok(
                resultMapper.toResponse(result)
        );
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ResultResponse>> getStudentResults(
            @PathVariable Long studentId
    ) {

        List<ResultResponse> results =
                resultService.getStudentResults(studentId)
                        .stream()
                        .map(resultMapper::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @GetMapping("/student/{studentId}/session/{academicSessionId}")
    public ResponseEntity<List<ResultResponse>> getStudentResultsForSession(
            @PathVariable Long studentId,
            @PathVariable Long academicSessionId
    ) {

        List<ResultResponse> results =
                resultService.getStudentResultsForSession(
                                studentId,
                                academicSessionId
                        )
                        .stream()
                        .map(resultMapper::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResultResponse> updateResult(
            @PathVariable Long id,
            @RequestBody ResultRequest request
    ) {

        Result result = resultService.updateResult(
                id,
                request.getCaScore(),
                request.getExamScore()
        );

        return ResponseEntity.ok(
                resultMapper.toResponse(result)
        );
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ResultResponse> approveResult(
            @PathVariable Long id
    ) {

        Result result = resultService.approveResult(id);

        return ResponseEntity.ok(
                resultMapper.toResponse(result)
        );
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ResultResponse> rejectResult(
            @PathVariable Long id
    ) {

        Result result = resultService.rejectResult(id);

        return ResponseEntity.ok(
                resultMapper.toResponse(result)
        );
    }
}
