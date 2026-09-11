package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;

public record SemesterSummaryDTO(
        Long sessionId,
        String session,
        Integer semester,
        Integer tuo,
        Integer tup,
        Integer tuf,
        BigDecimal twgp,
        BigDecimal gpa
) {
}
