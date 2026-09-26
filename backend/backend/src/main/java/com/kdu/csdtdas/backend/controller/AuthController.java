package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.CurrentUserResponse;
import com.kdu.csdtdas.backend.dto.LoginRequest;
import com.kdu.csdtdas.backend.dto.LoginResponse;
import com.kdu.csdtdas.backend.dto.SignupRequest;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.service.CourseAllocationService;
import com.kdu.csdtdas.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final CourseAllocationService courseAllocationService;

    public AuthController(UserService userService, CourseAllocationService courseAllocationService) {
        this.userService = userService;
        this.courseAllocationService = courseAllocationService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        String token = userService.authenticate(request.getUsername(), request.getPassword());
        User user = userService.getByUsername(request.getUsername());

        LoginResponse response = new LoginResponse(token, user.getUsername(), user.getFullName(), user.getRole());
        if (user.getProgramme() != null) {
            response.setProgrammeId(user.getProgramme().getId());
            response.setProgrammeName(user.getProgramme().getName());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<LoginResponse> signup(@RequestBody SignupRequest request) {
        userService.signup(
                request.getIdNumber(), request.getFullName(), request.getEmail(),
                request.getUsername(), request.getPassword()
        );

        String token = userService.authenticate(request.getUsername(), request.getPassword());
        User user = userService.getByUsername(request.getUsername());

        LoginResponse response = new LoginResponse(token, user.getUsername(), user.getFullName(), user.getRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("Not authenticated.");


        }

        User user = userService.getByUsername(authentication.getName());

        CurrentUserResponse response = new CurrentUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullName(user.getFullName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());

        if (user.getProgramme() != null) {
            response.setProgrammeId(user.getProgramme().getId());
            response.setProgrammeName(user.getProgramme().getName());
        }



        if (user.getStudent() != null) {

            if (user.getStudent().getLevel() != null) {
                response.setLevelId(user.getStudent().getLevel().getId());
                response.setLevelName(user.getStudent().getLevel().getName());
            }
            response.setStudentId(user.getStudent().getId());
            response.setMatricNumber(user.getStudent().getMatricNumber());


        }

        boolean assigned = "LEVEL_ADVISER".equalsIgnoreCase(user.getRole())
                || "HOD".equalsIgnoreCase(user.getRole())
                || "ADMIN".equalsIgnoreCase(user.getRole())
                || !courseAllocationService.getAllForLecturer(user.getId()).isEmpty();

        response.setAssigned(assigned);

        return ResponseEntity.ok(response);
    }
}