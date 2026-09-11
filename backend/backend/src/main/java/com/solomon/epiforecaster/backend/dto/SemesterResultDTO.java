package com.solomon.epiforecaster.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public record SemesterResultDTO(

        Long studentId,

        String matricNumber,

        String studentName,

        Long sessionId,

        String session,

        Integer semester,

        List<CourseResultDTO> courses,

        SemesterSummaryDTO semesterSummary,

        CumulativeSummaryDTO previousCumulative,

        CumulativeSummaryDTO currentCumulative,

        String remark

) {
}