package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.CreateOnlineClassRequest;
import com.kdu.csdtdas.backend.dto.OnlineClassResponse;
import com.kdu.csdtdas.backend.service.OnlineClassService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/online-classes")
@CrossOrigin(origins = "*")
public class OnlineClassController {

    private final OnlineClassService classService;

    public OnlineClassController(OnlineClassService classService) {
        this.classService = classService;
    }

    @PostMapping
    public ResponseEntity<OnlineClassResponse> create(
            @RequestBody CreateOnlineClassRequest request, Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classService.create(currentUsername(authentication), request));
    }

    @GetMapping("/course/{courseId}/session/{sessionId}/semester/{semester}")
    public List<OnlineClassResponse> listForCourse(
            @PathVariable Long courseId, @PathVariable Long sessionId, @PathVariable String semester,
            Authentication authentication
    ) {
        return classService.listForCourse(currentUsername(authentication), courseId, sessionId, semester);
    }

    @GetMapping("/mine")
    public List<OnlineClassResponse> listMine(Authentication authentication) {
        return classService.listForStudent(currentUsername(authentication));
    }

    @PutMapping("/{id}/start")
    public OnlineClassResponse.JoinLink start(@PathVariable Long id, Authentication authentication) {
        return classService.start(currentUsername(authentication), id);
    }

    @PutMapping("/{id}/end")
    public ResponseEntity<Void> end(@PathVariable Long id, Authentication authentication) {
        classService.end(currentUsername(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/join")
    public OnlineClassResponse.JoinLink join(@PathVariable Long id, Authentication authentication) {
        return classService.join(currentUsername(authentication), id);
    }

    @GetMapping("/{id}/participants")
    public List<OnlineClassResponse.Participant> participants(@PathVariable Long id, Authentication authentication) {
        return classService.participants(currentUsername(authentication), id);
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be logged in.");
        }
        return authentication.getName();
    }
}