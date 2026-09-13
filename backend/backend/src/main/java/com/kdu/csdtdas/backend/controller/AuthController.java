package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.LoginRequest;
import com.kdu.csdtdas.backend.dto.LoginResponse;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {

        String token = userService.authenticate(
                request.getUsername(),
                request.getPassword()
        );

        User user = userService.getByUsername(request.getUsername());

        LoginResponse response = new LoginResponse(
                token,
                user.getUsername(),
                user.getFullName(),
                user.getRole()
        );

        return ResponseEntity.ok(response);
    }
}