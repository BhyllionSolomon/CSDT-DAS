package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.SecurityLogRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SecurityLogService {

    public record Event(String at, String username, String role, String ip, String action, String detail) {}

    private final SecurityLogRepository repository;
    private final UserRepository userRepository;

    public SecurityLogService(SecurityLogRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    public List<Event> recent(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (!Set.of("HOD", "ADMIN").contains(String.valueOf(user.getRole()).toUpperCase())) {
            throw new IllegalArgumentException("Only the H.O.D or an admin can see the security log.");
        }

        return repository.findAll(PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "occurredAt")))
                .getContent().stream()
                .map(e -> new Event(String.valueOf(e.getOccurredAt()), e.getUsername(), e.getRole(), e.getIp(), e.getAction(), e.getDetail()))
                .toList();
    }
}