package com.kdu.csdtdas.backend.service;

import com.kdu.csdtdas.backend.dto.CurrentUserResponse;
import com.kdu.csdtdas.backend.entity.Student;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.repository.CourseAllocationRepository;
import com.kdu.csdtdas.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CurrentUserService {

    private final UserRepository userRepository;
    private final CourseAllocationRepository allocationRepository;

    public CurrentUserService(UserRepository userRepository, CourseAllocationRepository allocationRepository) {
        this.userRepository = userRepository;
        this.allocationRepository = allocationRepository;
    }

    public CurrentUserResponse build(String username) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        CurrentUserResponse r = new CurrentUserResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setRole(user.getRole());

        if (user.getProgramme() != null) {
            r.setProgrammeId(user.getProgramme().getId());
            r.setProgrammeName(user.getProgramme().getName());
        }

        Student s = user.getStudent();
        if (s != null) {
            r.setStudentId(s.getId());
            r.setMatricNumber(s.getMatricNumber());

            // A student's programme and level come from their student record.
            if (s.getProgramme() != null) {
                r.setProgrammeId(s.getProgramme().getId());
                r.setProgrammeName(s.getProgramme().getName());
            }
            if (s.getLevel() != null) {
                r.setLevelId(s.getLevel().getId());
                r.setLevelName(s.getLevel().getName());
            }
        }

        String role = user.getRole() == null ? "" : user.getRole().toUpperCase();
        boolean privileged = role.equals("HOD") || role.equals("ADMIN") || role.equals("LEVEL_ADVISER");
        r.setAssigned(privileged || !allocationRepository.findByLecturerId(user.getId()).isEmpty());

        return r;
    }
}