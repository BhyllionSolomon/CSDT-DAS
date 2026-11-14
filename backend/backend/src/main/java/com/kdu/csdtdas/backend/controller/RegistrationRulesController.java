package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.service.DirectEntryService;
import com.kdu.csdtdas.backend.service.RegistrationRulesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RegistrationRulesController {

    public record EntryRequest(Integer entryLevel) {}

    public record GrantRequest(Long studentId, Long sessionId, String semester, Integer units, String note) {}

    private final RegistrationRulesService rules;
    private final DirectEntryService direct;

    public RegistrationRulesController(RegistrationRulesService rules, DirectEntryService direct) {
        this.rules = rules;
        this.direct = direct;
    }

    // A student only gets their own options (the service checks); staff may ask for any student.
    @GetMapping("/course-registrations/student/{studentId}/options")
    public RegistrationRulesService.Options options(
            @PathVariable Long studentId, @RequestParam Long sessionId, @RequestParam String semester, Authentication a
    ) {
        return rules.optionsFor(name(a), studentId, sessionId, semester);
    }

    @GetMapping("/direct-entry")
    public List<DirectEntryService.DirectRow> directEntry(Authentication a) {
        return direct.list(name(a));
    }

    @PutMapping("/direct-entry/{studentId}")
    public ResponseEntity<Void> mark(@PathVariable Long studentId, @RequestBody EntryRequest body, Authentication a) {
        direct.mark(name(a), studentId, body.entryLevel());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/extra-units")
    public List<DirectEntryService.GrantRow> grants(@RequestParam Long sessionId, Authentication a) {
        return direct.grants(name(a), sessionId);
    }

    @PostMapping("/extra-units")
    public ResponseEntity<Void> grant(@RequestBody GrantRequest body, Authentication a) {
        direct.grant(name(a), body.studentId(), body.sessionId(), body.semester(),
                body.units() == null ? 0 : body.units(), body.note());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/extra-units/{id}")
    public ResponseEntity<Void> revoke(@PathVariable Long id, Authentication a) {
        direct.revoke(name(a), id);
        return ResponseEntity.noContent().build();
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}