package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.TimetableDTOs;
import com.kdu.csdtdas.backend.service.TimetableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TimetableController {

    private final TimetableService service;

    public TimetableController(TimetableService service) {
        this.service = service;
    }

    // ---- rooms

    @GetMapping("/timetable/rooms")
    public List<TimetableDTOs.RoomDto> rooms() {
        return service.rooms();
    }

    @PostMapping("/timetable/rooms")
    public ResponseEntity<TimetableDTOs.RoomDto> addRoom(@RequestBody TimetableDTOs.RoomRequest body, Authentication a) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addRoom(name(a), body.name(), body.type()));
    }

    @PutMapping("/timetable/rooms/{id}/active")
    public TimetableDTOs.RoomDto setRoomActive(
            @PathVariable Long id, @RequestBody TimetableDTOs.ActiveRequest body, Authentication a
    ) {
        return service.setRoomActive(name(a), id, Boolean.TRUE.equals(body.active()));
    }

    // ---- H.O.D

    @GetMapping("/timetable/status")
    public TimetableDTOs.AdminStatus status(
            @RequestParam Long sessionId, @RequestParam String semester, Authentication a
    ) {
        return service.adminStatus(name(a), sessionId, semester);
    }

    @PostMapping("/timetable/windows")
    public ResponseEntity<TimetableDTOs.WindowInfo> open(@RequestBody TimetableDTOs.OpenWindow body, Authentication a) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.open(name(a), body.sessionId(), body.semester(), body.deadline()));
    }

    @PutMapping("/timetable/windows/{id}/deadline")
    public TimetableDTOs.WindowInfo deadline(
            @PathVariable Long id, @RequestBody TimetableDTOs.DeadlineRequest body, Authentication a
    ) {
        return service.updateDeadline(name(a), id, body.deadline());
    }

    @PostMapping("/timetable/windows/{id}/reopen")
    public TimetableDTOs.WindowInfo reopen(
            @PathVariable Long id, @RequestBody TimetableDTOs.DeadlineRequest body, Authentication a
    ) {
        return service.reopen(name(a), id, body.deadline());
    }

    @PostMapping("/timetable/windows/{id}/auto-fill")
    public TimetableDTOs.AutoFillResult autoFill(@PathVariable Long id, Authentication a) {
        return service.autoFill(name(a), id);
    }

    @PostMapping("/timetable/windows/{id}/publish")
    public TimetableDTOs.Message publish(
            @PathVariable Long id, @RequestBody(required = false) TimetableDTOs.PublishRequest body, Authentication a
    ) {
        return service.publish(name(a), id, body != null && Boolean.TRUE.equals(body.force()));
    }

    // ---- lecturers and the H.O.D

    @GetMapping("/timetable/mine")
    public TimetableDTOs.MineView mine(@RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.mine(name(a), sessionId, semester);
    }

    @GetMapping("/timetable/view")
    public TimetableDTOs.StudentView view(@RequestParam Long sessionId, @RequestParam String semester, Authentication a) {
        return service.view(name(a), sessionId, semester);
    }

    @PostMapping("/timetable/entries")
    public ResponseEntity<TimetableDTOs.EntryDto> addEntry(@RequestBody TimetableDTOs.EntryRequest body, Authentication a) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addEntry(name(a), body));
    }

    @DeleteMapping("/timetable/entries/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id, Authentication a) {
        service.deleteEntry(name(a), id);
        return ResponseEntity.noContent().build();
    }

    // ---- students. Kept under /academic-sessions, a path students are already allowed to read.
    //      The student comes from the sign-in, never from the request.

    @GetMapping("/academic-sessions/{sessionId}/timetable/mine")
    public TimetableDTOs.StudentView studentView(
            @PathVariable Long sessionId, @RequestParam String semester, Authentication a
    ) {
        return service.studentView(name(a), sessionId, semester);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}