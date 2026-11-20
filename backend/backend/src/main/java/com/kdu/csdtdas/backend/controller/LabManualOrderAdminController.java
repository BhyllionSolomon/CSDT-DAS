package com.kdu.csdtdas.backend.controller;
import com.kdu.csdtdas.backend.dto.LabManualOrderAdminRow;
import com.kdu.csdtdas.backend.service.LabManualOrderAdminService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/lab/orders")
public class LabManualOrderAdminController {
    private final LabManualOrderAdminService service;
    private final com.kdu.csdtdas.backend.service.PaystackLabManualService payments;
    public LabManualOrderAdminController(LabManualOrderAdminService service, com.kdu.csdtdas.backend.service.PaystackLabManualService payments) {
        this.service=service; this.payments=payments;
    }
    @GetMapping public List<LabManualOrderAdminRow> list() { return service.list(); }

    @PutMapping("/{orderId}/issue")
    public com.kdu.csdtdas.backend.dto.LabStudentDTOs.OrderRow issue(
            @PathVariable Long orderId, org.springframework.security.core.Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new IllegalArgumentException("Please sign in.");
        return payments.issue(orderId, authentication.getName());
    }
}
