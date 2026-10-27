package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.AccountDTOs;
import com.kdu.csdtdas.backend.service.AccountSecurityService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/forgot-password")
@CrossOrigin(origins = "*")
public class PasswordRecoveryController {

    private final AccountSecurityService service;

    public PasswordRecoveryController(AccountSecurityService service) {
        this.service = service;
    }

    @PostMapping("/request")
    public AccountDTOs.Message request(@RequestBody AccountDTOs.ForgotRequest body) {
        service.requestPasswordReset(body.identifier());
        return new AccountDTOs.Message(
                "If an account matches, a 6-digit code has been sent to the email address on file. It expires in 10 minutes."
        );
    }

    @PostMapping("/confirm")
    public AccountDTOs.Message confirm(@RequestBody AccountDTOs.ForgotConfirm body) {
        service.confirmPasswordReset(body.identifier(), body.code(), body.newPassword());
        return new AccountDTOs.Message("Password updated. You can now sign in.");
    }
}