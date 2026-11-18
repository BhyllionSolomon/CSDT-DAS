package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;

/** Public-to-authenticated-students catalogue data; deliberately excludes stock and financial fields. */
public record StudentLabManualOption(
        Long id,
        String title,
        String courseCode,
        String courseTitle,
        BigDecimal price
) {}
