package com.kdu.csdtdas.backend.dto;

import java.time.LocalDateTime;

public record CreateOnlineClassRequest(
        Long courseId,
        Long academicSessionId,
        String semester,
        String title,
        LocalDateTime scheduledAt
) {}