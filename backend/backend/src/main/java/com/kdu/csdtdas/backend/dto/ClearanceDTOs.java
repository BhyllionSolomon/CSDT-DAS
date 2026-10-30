package com.kdu.csdtdas.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class ClearanceDTOs {

    private ClearanceDTOs() {}

    public record Status(
            boolean gateEnabled, Long sessionId, String sessionName, String state,
            String registrationCode, String invoiceCode, String note,
            boolean cleared, String message
    ) {}

    public record Submit(String invoiceCode) {}

    public record Decision(String note) {}

    public record Assisted(Long studentId, String invoiceCode) {}

    public record GateSetting(boolean enabled) {}

    public record Count(int count) {}

    public record Entry(
            Long id, Long studentId, String matricNumber, String fullName,
            String programme, String level,
            String invoiceCode, String registrationCode, String status, String note, String source,
            LocalDateTime submittedAt, LocalDateTime decidedAt, String decidedBy,
            List<String> courses, List<String> sharedWith
    ) {}
}