package com.kdu.csdtdas.backend.dto;

import java.time.LocalDateTime;

public final class AdviserDTOs {

    private AdviserDTOs() {}

    public record Assign(Long programmeId, Long levelId) {}

    public record Remove(String reason) {}

    public record Message(String message) {}

    public record Entry(
            Long id, Long userId, String fullName, String email, String phoneNumber,
            String programmeName, String levelName,
            LocalDateTime startedAt, LocalDateTime endedAt,
            String assignedBy, String endedBy, String endReason
    ) {}

    public record Contact(String fullName, String email, String phoneNumber, String programmeName, String levelName) {}
}