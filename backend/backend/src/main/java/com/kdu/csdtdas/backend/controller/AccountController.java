package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.AuthDTOs;
import com.kdu.csdtdas.backend.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<AuthDTOs.Message> resetPassword(
            @PathVariable Long id, @RequestBody Map<String, String> body
    ) {
        accountService.adminResetPassword(id, body.get("newPassword"));
        return ResponseEntity.ok(new AuthDTOs.Message("Password updated."));
    }
}