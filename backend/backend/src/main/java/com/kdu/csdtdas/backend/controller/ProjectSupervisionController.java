package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ProjectDTOs;
import com.kdu.csdtdas.backend.service.ProjectSupervisionService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProjectSupervisionController {

    private final ProjectSupervisionService service;

    public ProjectSupervisionController(ProjectSupervisionService service) {
        this.service = service;
    }

    // ---- H.O.D (the service refuses anyone else)

    @GetMapping("/projects/staff")
    public List<ProjectDTOs.StaffRow> staff(Authentication a) {
        return service.staff(name(a));
    }

    @PutMapping("/projects/staff")
    public List<ProjectDTOs.StaffRow> saveStaff(@RequestBody List<ProjectDTOs.StaffUpdate> body, Authentication a) {
        return service.saveStaff(name(a), body);
    }

    @GetMapping("/projects/proposal")
    public ProjectDTOs.Proposal proposal(@RequestParam(required = false) Long sessionId, Authentication a) {
        return service.propose(name(a), sessionId);
    }

    @PostMapping("/projects/confirm")
    public ProjectDTOs.Message confirm(@RequestBody ProjectDTOs.ConfirmRequest body, Authentication a) {
        return service.confirm(name(a), body);
    }

    @GetMapping("/projects/assignments")
    public List<ProjectDTOs.AssignmentRow> assignments(@RequestParam(required = false) Long sessionId, Authentication a) {
        return service.all(name(a), sessionId);
    }

    @PostMapping("/projects/drafts")
    public ProjectDTOs.DraftView createDraft(@RequestParam(required = false) Long sessionId, Authentication a) {
        return service.createDraft(name(a), sessionId);
    }

    @GetMapping("/projects/drafts/{draftId}")
    public ProjectDTOs.DraftView getDraft(@PathVariable Long draftId, Authentication a) {
        return service.getDraft(name(a), draftId);
    }

    @GetMapping("/projects/drafts/latest")
    public ProjectDTOs.DraftView latestDraft(@RequestParam(required = false) Long sessionId, Authentication a) {
        return service.latestDraft(name(a), sessionId);
    }

    @PatchMapping("/projects/drafts/{draftId}/items/{itemId}")
    public ProjectDTOs.DraftView moveDraftItem(@PathVariable Long draftId, @PathVariable Long itemId,
                                               @RequestBody ProjectDTOs.MoveRequest body, Authentication a) {
        return service.moveDraftItem(name(a), draftId, itemId, body);
    }

    @PostMapping("/projects/drafts/{draftId}/publish")
    public ProjectDTOs.DraftView publishDraft(@PathVariable Long draftId, Authentication a) {
        return service.publishDraft(name(a), draftId);
    }

    @GetMapping("/projects/export/preview")
    public ProjectDTOs.AllocationExportPreview allocationPreview(
            @RequestParam(required = false) Long sessionId, Authentication a) {
        return service.allocationExportPreview(name(a), sessionId);
    }

    @GetMapping("/projects/export/word")
    public ResponseEntity<byte[]> allocationWord(
            @RequestParam(required = false) Long sessionId, Authentication a) {
        byte[] file = service.exportAllocationWord(name(a), sessionId);
        String filename = "supervision-allocation.docx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .contentLength(file.length)
                .body(file);
    }

    @GetMapping("/projects/public-schedule")
    public List<ProjectDTOs.PublicScheduleRow> publicSchedule(@RequestParam(required = false) Long sessionId,
                                                               Authentication a) {
        name(a);
        return service.publicSchedule(sessionId);
    }

    @DeleteMapping("/projects/assignments/{id}")
    public ResponseEntity<Void> remove(@PathVariable Long id, Authentication a) {
        service.remove(name(a), id);
        return ResponseEntity.noContent().build();
    }

    // ---- supervisors see their own students

    @GetMapping("/projects/my-students")
    public List<ProjectDTOs.MyStudent> myStudents(Authentication a) {
        return service.myStudents(name(a));
    }

    // ---- students see their own supervisor. Kept under /academic-sessions, a path students are already allowed to read;
    //      the student comes from the sign-in, never from the request.

    @GetMapping("/academic-sessions/project-supervisor/mine")
    public ProjectDTOs.MyAssignment mine(Authentication a) {
        return service.mine(name(a));
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}