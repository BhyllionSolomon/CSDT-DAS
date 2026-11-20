package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.AttendanceDTOs;
import com.kdu.csdtdas.backend.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @GetMapping("/attendance/courses")
    public List<AttendanceDTOs.CourseInfo> courses(@RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.courses(name(a), sessionId, semester);
    }

    @GetMapping("/attendance/classes")
    public List<AttendanceDTOs.ClassRow> classes(
            @RequestParam Long courseId, @RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.classes(name(a), courseId, sessionId, semester);
    }

    @GetMapping("/attendance/roster")
    public AttendanceDTOs.Roster roster(
            @RequestParam Long courseId, @RequestParam Long sessionId, @RequestParam String semester,
            @RequestParam String date, @RequestParam String kind, Authentication a) {
        return service.rosterFor(name(a), courseId, sessionId, semester, date, kind);
    }

    @PostMapping("/attendance/save")
    public AttendanceDTOs.ClassRow save(@RequestBody AttendanceDTOs.SaveRequest body, Authentication a) {
        return service.save(name(a), body);
    }

    @DeleteMapping("/attendance/classes/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication a) {
        service.delete(name(a), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/attendance/summary")
    public AttendanceDTOs.Summary summary(
            @RequestParam Long courseId, @RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.summary(name(a), courseId, sessionId, semester);
    }

    // Students: kept under /academic-sessions, a path students are already allowed to read.
    // The student comes from the sign-in, never from the request.
    @GetMapping("/academic-sessions/attendance/mine")
    public AttendanceDTOs.Mine mine(@RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.mine(name(a), sessionId, semester);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}