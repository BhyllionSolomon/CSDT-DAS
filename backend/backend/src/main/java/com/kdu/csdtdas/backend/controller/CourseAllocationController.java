package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.AllocationDTOs;
import com.kdu.csdtdas.backend.service.AllocationDocumentService;
import com.kdu.csdtdas.backend.service.CourseAllocationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/allocations")
@CrossOrigin(origins = "*")
public class CourseAllocationController {

    private final CourseAllocationService allocationService;
    private final AllocationDocumentService documentService;

    public CourseAllocationController(
            CourseAllocationService allocationService,
            AllocationDocumentService documentService
    ) {
        this.allocationService = allocationService;
        this.documentService = documentService;
    }

    @GetMapping
    public List<AllocationDTOs.Entry> getAll() {
        return allocationService.getAll();
    }

    @GetMapping("/session/{academicSessionId}/semester/{semester}")
    public List<AllocationDTOs.Entry> getForSession(
            @PathVariable Long academicSessionId, @PathVariable String semester
    ) {
        return allocationService.getForSession(academicSessionId, semester);
    }

    @GetMapping("/lecturer/{lecturerId}")
    public List<AllocationDTOs.Entry> getForLecturer(@PathVariable Long lecturerId) {
        return allocationService.getAllForLecturer(lecturerId);
    }

    @GetMapping("/my-options")
    public AllocationDTOs.MyOptions myOptions(
            @RequestParam Long academicSessionId, @RequestParam String semester, Authentication authentication
    ) {
        return allocationService.optionsFor(username(authentication), academicSessionId, semester);
    }

    @PostMapping("/claim")
    public AllocationDTOs.Entry claim(@RequestBody AllocationDTOs.Claim body, Authentication authentication) {
        return allocationService.claim(
                username(authentication), body.courseId(), body.academicSessionId(), body.semester()
        );
    }

    @PostMapping("/offer")
    public AllocationDTOs.Entry offer(@RequestBody AllocationDTOs.Offer body) {
        return allocationService.offer(body.courseId(), body.academicSessionId(), body.semester());
    }

    @PostMapping("/add-lecturer")
    public AllocationDTOs.Entry addLecturer(@RequestBody AllocationDTOs.AddLecturer body) {
        return allocationService.addLecturer(
                body.courseId(), body.academicSessionId(), body.semester(),
                body.lecturerId(), body.lecturerName()
        );
    }

    @PutMapping("/{id}/link/{userId}")
    public AllocationDTOs.Entry link(@PathVariable Long id, @PathVariable Long userId) {
        return allocationService.linkToAccount(id, userId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id) {
        allocationService.remove(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/docx/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<AllocationDTOs.PreviewRow> preview(@RequestParam MultipartFile file) {
        return documentService.preview(file);
    }

    @PostMapping("/docx/commit")
    public AllocationDTOs.CommitResult commit(@RequestBody AllocationDTOs.CommitRequest body) {
        return documentService.commit(body);
    }

    @PostMapping(value = "/docx/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AllocationDTOs.ImportResult importDocument(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Long academicSessionId,
            @RequestParam(required = false) String semester
    ) {
        return documentService.importDocument(file, academicSessionId, semester);
    }

    private String username(Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be logged in.");
        }
        return authentication.getName();
    }
}