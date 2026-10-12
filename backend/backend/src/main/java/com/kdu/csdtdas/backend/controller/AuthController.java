package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.AuthDTOs;
import com.kdu.csdtdas.backend.dto.CurrentUserResponse;
import com.kdu.csdtdas.backend.dto.LoginRequest;
import com.kdu.csdtdas.backend.dto.LoginResponse;
import com.kdu.csdtdas.backend.dto.ResetPasswordRequest;
import com.kdu.csdtdas.backend.dto.SignupRequest;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.service.AccountService;
import com.kdu.csdtdas.backend.service.CurrentUserService;
import com.kdu.csdtdas.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final AccountService accountService;

    public AuthController(
            UserService userService,
            CurrentUserService currentUserService,
            AccountService accountService
    ) {
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginFor(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthDTOs.SignupResult> signup(@RequestBody SignupRequest request) {

        User created = userService.signup(
                request.getIdNumber(), request.getFullName(), request.getEmail(),
                request.getUsername(), request.getPassword()
        );

        if (!Boolean.TRUE.equals(created.getActive())) {
            return ResponseEntity.ok(new AuthDTOs.SignupResult(
                    true,
                    "Account created. The H.O.D must approve staff accounts before you can sign in.",
                    null
            ));
        }

        return ResponseEntity.ok(new AuthDTOs.SignupResult(
                false, "Account created.", loginFor(request.getUsername(), request.getPassword())
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("Not authenticated.");
        }
        return ResponseEntity.ok(currentUserService.build(authentication.getName()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthDTOs.Message> resetPassword(@RequestBody ResetPasswordRequest request) {
        accountService.resetPassword(request.identifier(), request.email(), request.newPassword());
        return ResponseEntity.ok(new AuthDTOs.Message("Password updated. You can now sign in."));
    }

    private LoginResponse loginFor(String username, String password) {
        String token = userService.authenticate(username, password);
        User user = userService.getByUsername(username);

        LoginResponse response = new LoginResponse(token, user.getUsername(), user.getFullName(), user.getRole());
        if (user.getProgramme() != null) {
            response.setProgrammeId(user.getProgramme().getId());
            response.setProgrammeName(user.getProgramme().getName());
        }
        return response;
    }
}