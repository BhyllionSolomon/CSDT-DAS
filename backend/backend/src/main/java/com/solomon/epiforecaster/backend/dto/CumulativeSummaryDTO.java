package com.solomon.epiforecaster.backend.dto;

import java.math.BigDecimal;

public record CumulativeSummaryDTO(

        Integer ctu,

        BigDecimal cumulativeTwgp,

        BigDecimal cgpa,

        String degreeClass

) {
}