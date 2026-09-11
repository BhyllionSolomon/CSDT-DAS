package com.kdu.csdtdas.backend.util;

import java.math.BigDecimal;

public final class DegreeClassUtil {

    private DegreeClassUtil() {
    }

    public static String classify(BigDecimal cgpa) {

        if (cgpa == null) {
            return null;
        }

        if (cgpa.compareTo(BigDecimal.valueOf(4.50)) >= 0) {
            return "First Class";
        }

        if (cgpa.compareTo(BigDecimal.valueOf(3.50)) >= 0) {
            return "Second Class Upper Division";
        }

        if (cgpa.compareTo(BigDecimal.valueOf(2.40)) >= 0) {
            return "Second Class Lower Division";
        }

        if (cgpa.compareTo(BigDecimal.valueOf(1.50)) >= 0) {
            return "Third Class";
        }

        return "Fail";
    }
}
