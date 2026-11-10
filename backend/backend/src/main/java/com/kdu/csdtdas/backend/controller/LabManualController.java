package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.LabDTOs;
import com.kdu.csdtdas.backend.service.LabManualService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lab")
@CrossOrigin(origins = "*")
public class LabManualController {

    private final LabManualService service;

    public LabManualController(LabManualService service) {
        this.service = service;
    }

    @GetMapping("/courses")
    public List<LabDTOs.CourseHit> courses(Authentication a) {
        return service.courses(name(a));
    }

    @GetMapping("/manuals")
    public List<LabDTOs.ManualRow> manuals(Authentication a) {
        return service.manuals(name(a));
    }

    @PostMapping("/manuals")
    public ResponseEntity<LabDTOs.ManualRow> saveManual(@RequestBody LabDTOs.ManualRequest body, Authentication a) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveManual(name(a), body));
    }

    @PostMapping("/manuals/{id}/purchases")
    public LabDTOs.Message addPurchase(@PathVariable Long id, @RequestBody LabDTOs.PurchaseRequest body, Authentication a) {
        return service.addPurchase(name(a), id, body);
    }

    @GetMapping("/students/search")
    public List<LabDTOs.StudentHit> search(@RequestParam String q, Authentication a) {
        return service.searchStudents(name(a), q);
    }

    @PostMapping("/sales")
    public LabDTOs.Message sell(@RequestBody LabDTOs.SaleRequest body, Authentication a) {
        return service.recordSale(name(a), body);
    }

    @GetMapping("/sales")
    public List<LabDTOs.SaleRow> sales(Authentication a) {
        return service.sales(name(a));
    }

    @DeleteMapping("/sales/{id}")
    public LabDTOs.Message voidSale(@PathVariable Long id, Authentication a) {
        return service.voidSale(name(a), id);
    }

    @GetMapping("/finance")
    public LabDTOs.Finance finance(Authentication a) {
        return service.finance(name(a));
    }

    @GetMapping("/impact")
    public LabDTOs.Impact impact(@RequestParam Long sessionId, @RequestParam Long manualId, Authentication a) {
        return service.impact(name(a), sessionId, manualId);
    }

    private String name(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException("You must be signed in.");
        }
        return authentication.getName();
    }
}