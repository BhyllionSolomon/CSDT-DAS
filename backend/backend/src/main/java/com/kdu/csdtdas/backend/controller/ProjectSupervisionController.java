package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ProjectDTOs;
import com.kdu.csdtdas.backend.service.ProjectSupervisionService;
import org.springframework.http.ResponseEntity;
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