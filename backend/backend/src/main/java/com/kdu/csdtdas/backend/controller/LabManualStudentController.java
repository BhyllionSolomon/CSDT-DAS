package com.kdu.csdtdas.backend.controller;

import com.kdu.csdtdas.backend.dto.LabStudentDTOs;
import com.kdu.csdtdas.backend.service.LabManualStudentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@RequestMapping("/api/lab/student/manuals")
public class LabManualStudentController {
    private final LabManualStudentService service;
    private final com.kdu.csdtdas.backend.service.PaystackLabManualService payments;
    public LabManualStudentController(LabManualStudentService service, com.kdu.csdtdas.backend.service.PaystackLabManualService payments) {
        this.service=service; this.payments=payments;
    }

    @GetMapping
    public List<LabStudentDTOs.ManualOption> catalogue(Authentication a) {
        return service.catalogue(username(a));
    }
    @PostMapping("/orders")
    public LabStudentDTOs.OrderRow request(@RequestBody LabStudentDTOs.OrderRequest body, Authentication a) {
        return service.request(username(a), body);
    }

    @PostMapping("/orders/{orderId}/pay")
    public com.kdu.csdtdas.backend.dto.LabStudentDTOs.PaymentInit pay(
            @PathVariable Long orderId, Authentication a) {
        return payments.initialise(username(a), orderId);
    }

    @GetMapping("/payments/verify")
    public com.kdu.csdtdas.backend.dto.LabStudentDTOs.Receipt verify(
            @RequestParam String reference, Authentication a) {
        return payments.verifyForStudent(username(a), reference);
    }

    @GetMapping("/orders/{orderId}/receipt")
    public com.kdu.csdtdas.backend.dto.LabStudentDTOs.Receipt receipt(
            @PathVariable Long orderId, Authentication a) {
        return payments.getReceipt(username(a), orderId);
    }

    private String username(Authentication a) {
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal()))
            throw new IllegalArgumentException("Please sign in.");
        return a.getName();
    }
}
