package com.kdu.csdtdas.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class RegistrationFormDTOs {

    private RegistrationFormDTOs() {}

    public record CourseLine(String code, String title, Integer unit, String status, String semester) {}

    public record Form(
            Long studentId, String matricNumber, String fullName, String programme, String level,
            Long sessionId, String sessionName,
            List<CourseLine> courses, int totalUnits,
            String state, String registrationCode, String invoiceCode, String note,
            String decidedBy, LocalDateTime decidedAt
    ) {}
}