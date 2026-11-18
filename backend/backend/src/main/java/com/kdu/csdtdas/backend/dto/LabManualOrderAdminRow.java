package com.kdu.csdtdas.backend.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record LabManualOrderAdminRow(Long id, String matricNumber, String studentName, String programme,
        String level, String courseCode, String manualTitle, BigDecimal amount, String status,
        String sessionName, LocalDateTime requestedAt) {}
