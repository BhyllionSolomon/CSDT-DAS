package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.config.ClientIp;
import com.kdu.csdtdas.backend.dto.AuthDTOs;
import com.kdu.csdtdas.backend.dto.CurrentUserResponse;
import com.kdu.csdtdas.backend.dto.LoginRequest;
import com.kdu.csdtdas.backend.dto.LoginResponse;
import com.kdu.csdtdas.backend.dto.SignupRequest;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.service.CurrentUserService;
import com.kdu.csdtdas.backend.service.StudentSignupService;
import com.kdu.csdtdas.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record VerifyRequest(String idNumber, String code) {}

    public record ResendRequest(String idNumber) {}

    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final StudentSignupService studentSignup;

    public AuthController(UserService userService, CurrentUserService currentUserService, StudentSignupService studentSignup) {
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.studentSignup = studentSignup;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginFor(request.getUsername(), request.getPassword()));
    }

    /**
     * Students (IDs starting with KDU) need the department's activation code and then an emailed code.
     * Staff accounts are created inactive and wait for the H.O.D.
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request, HttpServletRequest http) {

        String id = request.getIdNumber() == null ? "" : request.getIdNumber().trim().toUpperCase();

        if (id.startsWith("KDU")) {
            return ResponseEntity.ok(studentSignup.begin(
                    id, request.getFullName(), request.getEmail(), request.getUsername(), request.getPassword(),
                    request.getActivationCode(), ClientIp.of(http)));
        }

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

    @PostMapping("/signup/verify")
    public ResponseEntity<LoginResponse> verify(@RequestBody VerifyRequest body, HttpServletRequest http) {
        return ResponseEntity.ok(studentSignup.verify(body.idNumber(), body.code(), ClientIp.of(http)));
    }

    @PostMapping("/signup/resend")
    public ResponseEntity<StudentSignupService.Started> resend(@RequestBody ResendRequest body) {
        return ResponseEntity.ok(studentSignup.resend(body.idNumber()));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("Not authenticated.");
        }
        return ResponseEntity.ok(currentUserService.build(authentication.getName()));
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