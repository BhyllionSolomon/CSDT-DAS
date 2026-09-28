package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.StudentRequest;
import com.kdu.csdtdas.backend.dto.StudentResponse;
import com.kdu.csdtdas.backend.dto.StudentSessionRecord;
import com.kdu.csdtdas.backend.entity.AcademicSession;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.mapper.StudentMapper;
import com.kdu.csdtdas.backend.repository.AcademicSessionRepository;
import com.kdu.csdtdas.backend.service.StudentLevelHistoryService;
import com.kdu.csdtdas.backend.service.StudentLevelProgressionService;
import com.kdu.csdtdas.backend.service.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    private final StudentService studentService;
    private final StudentMapper studentMapper;
    private final StudentLevelProgressionService progressionService;
    private final StudentLevelHistoryService studentLevelHistoryService;
    private final AcademicSessionRepository academicSessionRepository;

    public StudentController(
            StudentService studentService,
            StudentMapper studentMapper,
            StudentLevelProgressionService progressionService,
            StudentLevelHistoryService studentLevelHistoryService,
            AcademicSessionRepository academicSessionRepository
    ) {
        this.studentService = studentService;
        this.studentMapper = studentMapper;
        this.progressionService = progressionService;
        this.studentLevelHistoryService = studentLevelHistoryService;
        this.academicSessionRepository = academicSessionRepository;
    }

    @PostMapping
    public ResponseEntity<StudentResponse> createStudent(
            @RequestBody StudentRequest request
    ) {

        Student student = studentService.createStudent(
                request.getMatricNumber(),
                request.getFullName(),
                request.getPhoneNumber(),
                request.getDepartmentId(),
                request.getProgrammeId(),
                request.getLevelId(),
                request.getAcademicSessionId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(studentMapper.toResponse(student));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponse> getStudent(
            @PathVariable Long id
    ) {

        Student student = studentService.getStudent(id);

        return ResponseEntity.ok(
                studentMapper.toResponse(student)
        );
    }

    @GetMapping("/matric/{matricNumber}")
    public ResponseEntity<StudentResponse> getStudentByMatricNumber(
            @PathVariable String matricNumber
    ) {

        Student student = studentService.getStudentByMatricNumber(matricNumber);

        return ResponseEntity.ok(
                studentMapper.toResponse(student)
        );
    }

    @GetMapping
    public ResponseEntity<List<StudentResponse>> getAllStudents() {

        List<StudentResponse> students =
                studentService.getAllStudents()
                        .stream()
                        .map(studentMapper::toResponse)
                        .collect(Collectors.toList());

        return ResponseEntity.ok(students);
    }

    @PutMapping("/{id}/level/{levelId}")
    public ResponseEntity<StudentResponse> updateStudentLevel(
            @PathVariable Long id,
            @PathVariable Long levelId
    ) {

        Student student = studentService.updateStudentLevel(id, levelId);

        return ResponseEntity.ok(
                studentMapper.toResponse(student)
        );
    }

    @PutMapping("/{id}/programme/{programmeId}")
    public ResponseEntity<StudentResponse> updateStudentProgramme(
            @PathVariable Long id,
            @PathVariable Long programmeId
    ) {

        Student student = studentService.updateStudentProgramme(id, programmeId);

        return ResponseEntity.ok(
                studentMapper.toResponse(student)
        );
    }

    @PutMapping("/{id}/admission-session/{academicSessionId}")
    public ResponseEntity<StudentResponse> setAdmissionSession(
            @PathVariable Long id,
            @PathVariable Long academicSessionId
    ) {

        Student student = studentService.setAdmissionSession(id, academicSessionId);

        return ResponseEntity.ok(
                studentMapper.toResponse(student)
        );
    }

    @PostMapping("/advance-levels/session/{currentSessionId}")
    public ResponseEntity<Integer> advanceAllLevels(
            @PathVariable Long currentSessionId
    ) {

        AcademicSession currentSession =
                academicSessionRepository.findById(currentSessionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Session not found.")
                        );

        int advanced = progressionService.advanceAllStudents(currentSession);

        return ResponseEntity.ok(advanced);
    }

    @PostMapping("/advance-one-level/session/{newSessionId}")
    public ResponseEntity<Integer> advanceAllStudentsByOneLevel(
            @PathVariable Long newSessionId
    ) {

        AcademicSession newSession =
                academicSessionRepository.findById(newSessionId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Session not found.")
                        );

        int advanced = progressionService.advanceAllStudentsByOneLevel(newSession);

        return ResponseEntity.ok(advanced);
    }

    @PostMapping("/history/backfill")
    public ResponseEntity<Integer> backfillHistory() {
        return ResponseEntity.ok(studentLevelHistoryService.backfillFromCurrentState());
    }

    @GetMapping("/session/{academicSessionId}/history")
    public ResponseEntity<List<StudentSessionRecord>> getStudentsForSession(
            @PathVariable Long academicSessionId
    ) {
        return ResponseEntity.ok(studentLevelHistoryService.getForSession(academicSessionId));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<StudentResponse> deactivateStudent(
            @PathVariable Long id
    ) {

        Student student = studentService.deactivateStudent(id);

        return ResponseEntity.ok(
                studentMapper.toResponse(student)
        );
    }

    @PutMapping("/{id}/activate")
    public ResponseEntity<StudentResponse> activateStudent(
            @PathVariable Long id
    ) {

        Student student = studentService.activateStudent(id);

        return ResponseEntity.ok(
                studentMapper.toResponse(student)
        );
    }
}