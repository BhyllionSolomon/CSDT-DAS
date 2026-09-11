package com.solomon.epiforecaster.backend.util;

import java.math.BigDecimal;

public final class GradeUtil {

    private GradeUtil() {
    }

    public static String getLetterGrade(BigDecimal score) {

        validateScore(score);

        if (score.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "A";
        }

        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "B";
        }

        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return "C";
        }

        if (score.compareTo(BigDecimal.valueOf(45)) >= 0) {
            return "D";
        }

        if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return "E";
        }

        return "F";
    }

    public static int getGradePoint(BigDecimal score) {

        validateScore(score);

        if (score.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return 5;
        }

        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return 4;
        }

        if (score.compareTo(BigDecimal.valueOf(50)) >= 0) {
            return 3;
        }

        if (score.compareTo(BigDecimal.valueOf(45)) >= 0) {
            return 2;
        }

        if (score.compareTo(BigDecimal.valueOf(40)) >= 0) {
            return 1;
        }

        return 0;
    }

    public static BigDecimal getWeightedGradePoint(
            Integer creditUnit,
            BigDecimal score
    ) {

        int gp = getGradePoint(score);

        return BigDecimal
                .valueOf(creditUnit)
                .multiply(BigDecimal.valueOf(gp));
    }

    public static boolean isPassed(BigDecimal score) {
        validateScore(score);

        return score.compareTo(BigDecimal.valueOf(40)) >= 0;
    }

    private static void validateScore(BigDecimal score) {

        if (score == null) {
            throw new IllegalArgumentException("Score cannot be null");
        }

        if (score.compareTo(BigDecimal.ZERO) < 0 ||
                score.compareTo(BigDecimal.valueOf(100)) > 0) {

            throw new IllegalArgumentException(
                    "Score must be between 0 and 100"
            );
        }
    }
}