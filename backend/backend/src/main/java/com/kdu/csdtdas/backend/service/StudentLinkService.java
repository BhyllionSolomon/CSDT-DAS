package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.StudentRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StudentLinkService {

    private static final Logger log = LoggerFactory.getLogger(StudentLinkService.class);

    public record UnlinkedAccount(Long id, String username, String fullName, String email, LocalDateTime createdAt) {}

    public record Result(String message) {}

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    public StudentLinkService(UserRepository userRepository, StudentRepository studentRepository) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
    }

    /** Student accounts that are not attached to any student record. */
    @Transactional(readOnly = true)
    public List<UnlinkedAccount> listUnlinked() {
        return userRepository.findAll().stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole()) && u.getStudent() == null)
                .map(u -> new UnlinkedAccount(
                        u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getCreatedAt()))
                .toList();
    }

    public Result link(Long userId, Long studentId, String actor) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found."));

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new IllegalArgumentException("Only student accounts can be linked to a student record.");
        }
        if (user.getStudent() != null) {
            throw new IllegalArgumentException("This account is already linked to a student record.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student record not found."));

        boolean taken = userRepository.findAll().stream()
                .anyMatch(u -> u.getStudent() != null && u.getStudent().getId().equals(studentId));
        if (taken) {
            throw new IllegalArgumentException(
                    "Another account is already linked to " + student.getMatricNumber() + "."
            );
        }

        user.setStudent(student);
        userRepository.save(user);

        log.warn("SECURITY LINK account '{}' linked to student {} by {}", user.getUsername(), student.getMatricNumber(), actor);

        return new Result(user.getUsername() + " is now linked to " + student.getMatricNumber()
                + ". They must sign out and sign in again.");
    }
}