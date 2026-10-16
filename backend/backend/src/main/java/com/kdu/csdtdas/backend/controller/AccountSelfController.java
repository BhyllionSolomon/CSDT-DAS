package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.AccountDTOs;
import com.kdu.csdtdas.backend.service.AccountSecurityService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountSelfController {

    private final AccountSecurityService service;

    public AccountSelfController(AccountSecurityService service) {
        this.service = service;
    }

    @GetMapping("/profile")
    public AccountDTOs.Profile profile(Authentication authentication) {
        return service.profile(username(authentication));
    }

    @PostMapping("/request-change")
    public AccountDTOs.Message requestChange(@RequestBody AccountDTOs.ChangeRequest body, Authentication authentication) {
        return service.requestChange(username(authentication), body.purpose(), body.newValue(), body.currentPassword());
    }

    @PostMapping("/confirm-change")
    public AccountDTOs.ChangeResult confirmChange(@RequestBody AccountDTOs.ConfirmRequest body, Authentication authentication) {
        return service.confirmChange(username(authentication), body.purpose(), body.code(), body.newPassword());
    }

    private String username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}