package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.ClassMaterialResponse;
import com.kdu.csdtdas.backend.dto.CreateLinkMaterialRequest;
import com.kdu.csdtdas.backend.service.ClassMaterialService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
public class ClassMaterialController {

    private final ClassMaterialService materialService;

    public ClassMaterialController(ClassMaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClassMaterialResponse> addFile(
            @RequestParam Long courseId,
            @RequestParam Long academicSessionId,
            @RequestParam String semester,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile file,
            Authentication authentication
    ) {
        ClassMaterialResponse response = materialService.addFile(
                currentUsername(authentication), courseId, academicSessionId, semester, title, description, file
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/link")
    public ResponseEntity<ClassMaterialResponse> addLink(
            @RequestBody CreateLinkMaterialRequest request, Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(materialService.addLink(currentUsername(authentication), request));
    }

    @GetMapping("/course/{courseId}/session/{sessionId}/semester/{semester}")
    public List<ClassMaterialResponse> listForCourse(
            @PathVariable Long courseId, @PathVariable Long sessionId, @PathVariable String semester,
            Authentication authentication
    ) {
        return materialService.listForCourse(currentUsername(authentication), courseId, sessionId, semester);
    }

    @GetMapping("/mine")
    public List<ClassMaterialResponse> listMine(Authentication authentication) {
        return materialService.listForStudent(currentUsername(authentication));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) {
        ClassMaterialService.DownloadedFile file = materialService.download(currentUsername(authentication), id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(file.filename(), StandardCharsets.UTF_8).build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.resource());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        materialService.delete(currentUsername(authentication), id);
        return ResponseEntity.noContent().build();
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be logged in.");
        }
        return authentication.getName();
    }
}