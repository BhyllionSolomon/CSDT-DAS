package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.StudentLabManualOption;
import com.kdu.csdtdas.backend.repository.LabManualRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/lab-manuals")
public class StudentLabManualCatalogueController {
    private final LabManualRepository manuals;

    public StudentLabManualCatalogueController(LabManualRepository manuals) {
        this.manuals = manuals;
    }

    @GetMapping
    public List<StudentLabManualOption> catalogue(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getAuthorities().stream().noneMatch(a -> "ROLE_STUDENT".equals(a.getAuthority()))) {
            throw new AccessDeniedException("Only signed-in students can view the student lab manual catalogue.");
        }

        return manuals.findAllWithCourse().stream()
                .filter(m -> m.isActive())
                .map(m -> new StudentLabManualOption(
                        m.getId(),
                        m.getTitle(),
                        m.getCourse() == null ? "" : m.getCourse().getCode(),
                        m.getCourse() == null ? "" : m.getCourse().getTitle(),
                        m.getUnitPrice()
                ))
                .toList();
    }
}
