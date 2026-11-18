package com.kdu.csdtdas.backend.controller;
import com.kdu.csdtdas.backend.dto.LabManualOrderAdminRow;
import com.kdu.csdtdas.backend.service.LabManualOrderAdminService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/lab/orders")
public class LabManualOrderAdminController {
    private final LabManualOrderAdminService service;
    public LabManualOrderAdminController(LabManualOrderAdminService service) { this.service=service; }
    @GetMapping public List<LabManualOrderAdminRow> list() { return service.list(); }
}
