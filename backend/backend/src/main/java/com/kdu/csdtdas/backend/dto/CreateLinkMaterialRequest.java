package com.kdu.csdtdas.backend.dto;

public record CreateLinkMaterialRequest(
        Long courseId,
        Long academicSessionId,
        String semester,
        String title,
        String description,
        String linkUrl
) {}