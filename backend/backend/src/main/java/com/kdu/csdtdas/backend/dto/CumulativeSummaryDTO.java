package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;

public record CumulativeSummaryDTO(

        Integer ctu,

        BigDecimal cumulativeTwgp,

        BigDecimal cgpa,

        String degreeClass

) {
}
