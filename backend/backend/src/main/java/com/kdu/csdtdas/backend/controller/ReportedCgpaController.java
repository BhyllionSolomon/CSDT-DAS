package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ParsedCgpaRow;
import com.kdu.csdtdas.backend.dto.ReportedCgpaResponse;
import com.kdu.csdtdas.backend.dto.SaveReportedCgpaRequest;
import com.kdu.csdtdas.backend.entity.ReportedCgpa;
import com.kdu.csdtdas.backend.entity.User;
import com.kdu.csdtdas.backend.service.ReportedCgpaPdfService;
import com.kdu.csdtdas.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reported-cgpa")
@CrossOrigin(origins = "*")
public class ReportedCgpaController {

    private final ReportedCgpaPdfService pdfService;
    private final UserService userService;

    public ReportedCgpaController(ReportedCgpaPdfService pdfService, UserService userService) {
        this.pdfService = pdfService;
        this.userService = userService;
    }

    @PostMapping("/extract-pdf")
    public ResponseEntity<List<ParsedCgpaRow>> extractPdf(@RequestParam MultipartFile file) {
        List<ReportedCgpaPdfService.ParsedRow> rows = pdfService.extractRows(file);
        List<ParsedCgpaRow> response = rows.stream()
                .map(r -> new ParsedCgpaRow(r.matricNumber, r.fullName, r.cgpa))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save")
    public ResponseEntity<Integer> save(
            @RequestBody SaveReportedCgpaRequest request,
            Authentication authentication
    ) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            User currentUser = userService.getByUsername(authentication.getName());
            Long userProgrammeId = currentUser.getProgramme() != null ? currentUser.getProgramme().getId() : null;
            pdfService.checkAdviserAuthority(currentUser.getRole(), userProgrammeId, request.getProgrammeId());
        }

        List<ReportedCgpaPdfService.ParsedRow> rows = request.getRows().stream()
                .map(r -> new ReportedCgpaPdfService.ParsedRow(r.getMatricNumber(), r.getFullName(), r.getCgpa()))
                .toList();

        String uploadedBy = authentication != null ? authentication.getName() : "unknown";

        int saved = pdfService.saveRows(
                request.getProgrammeId(), request.getLevelId(), request.getAcademicSessionId(),
                rows, uploadedBy
        );
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/programme/{programmeId}/level/{levelId}/session/{academicSessionId}")
    public ResponseEntity<List<ReportedCgpaResponse>> getRoster(
            @PathVariable Long programmeId, @PathVariable Long levelId, @PathVariable Long academicSessionId
    ) {
        List<ReportedCgpa> roster = pdfService.getRoster(programmeId, levelId, academicSessionId);
        List<ReportedCgpaResponse> response = roster.stream().map(r -> {
            ReportedCgpaResponse dto = new ReportedCgpaResponse();
            dto.setId(r.getId());
            dto.setMatricNumber(r.getMatricNumber());
            dto.setFullName(r.getFullName());
            dto.setCgpa(r.getCgpa());
            dto.setUploadedBy(r.getUploadedBy());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}