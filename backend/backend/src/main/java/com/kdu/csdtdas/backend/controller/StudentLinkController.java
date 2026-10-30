package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.StudentLinkService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class StudentLinkController {

    private final StudentLinkService service;

    public StudentLinkController(StudentLinkService service) {
        this.service = service;
    }

    // H.O.D only (covered by the /api/users/** rule).
    @GetMapping("/unlinked-students")
    public List<StudentLinkService.UnlinkedAccount> unlinked() {
        return service.listUnlinked();
    }

    @PutMapping("/{userId}/link-student/{studentId}")
    public StudentLinkService.Result link(
            @PathVariable Long userId, @PathVariable Long studentId, Authentication authentication
    ) {
        return service.link(userId, studentId, authentication.getName());
    }
}