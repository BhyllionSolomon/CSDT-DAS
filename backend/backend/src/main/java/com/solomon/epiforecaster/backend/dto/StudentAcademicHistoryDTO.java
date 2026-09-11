package com.solomon.epiforecaster.backend.dto;

import java.util.List;

public record StudentAcademicHistoryDTO(

        Long studentId,

        String matricNumber,

        String studentName,

        String programme,

        List<SemesterResultDTO> semesters,

        CumulativeSummaryDTO finalCumulative

) {
}