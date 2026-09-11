package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;

public record CourseResultDTO(

        Long resultId,

        Long courseId,

        String courseCode,

        String courseTitle,

        Integer creditUnit,

        BigDecimal score,

        String letterGrade,

        Integer gradePoint,

        BigDecimal weightedGradePoint,

        boolean passed

) {
}
