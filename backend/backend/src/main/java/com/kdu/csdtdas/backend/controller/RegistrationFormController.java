package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.RegistrationFormDTOs;
import com.kdu.csdtdas.backend.service.RegistrationFormService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RegistrationFormController {

    private final RegistrationFormService service;

    public RegistrationFormController(RegistrationFormService service) {
        this.service = service;
    }

    // A student only ever gets their own form: the student is taken from the sign-in, not from the request.
    @GetMapping("/registration-form/mine")
    public RegistrationFormDTOs.Form mine(Authentication authentication) {
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return service.forUser(authentication.getName());
    }

    // H.O.D only (covered by the /api/clearance/** rule).
    @GetMapping("/clearance/form/{studentId}")
    public RegistrationFormDTOs.Form forStudent(@PathVariable Long studentId) {
        return service.forStudent(studentId);
    }
}