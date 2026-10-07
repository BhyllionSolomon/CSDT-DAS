package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserPhoneController {

    private final UserRepository userRepository;

    public UserPhoneController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PutMapping("/{id}/phone")
    @Transactional
    public ResponseEntity<Map<String, String>> setPhone(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication
    ) {
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be logged in.");
        }

        User caller = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        boolean privileged = "HOD".equalsIgnoreCase(caller.getRole()) || "ADMIN".equalsIgnoreCase(caller.getRole());
        if (!privileged && !caller.getId().equals(id)) {
            throw new IllegalArgumentException("You can only change your own phone number.");
        }

        String phone = body.get("phoneNumber") == null ? "" : body.get("phoneNumber").trim();
        if (phone.length() > 30) {
            throw new IllegalArgumentException("Phone number is too long.");
        }

        User target = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        target.setPhoneNumber(phone.isEmpty() ? null : phone);
        userRepository.save(target);

        return ResponseEntity.ok(Map.of("phoneNumber", phone));
    }
}