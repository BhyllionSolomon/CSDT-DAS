package com.kdu.csdtdas.backend.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ExamQuestionValidationServiceTest {
    private final ExamQuestionValidationService service =
        new ExamQuestionValidationService(null, null, null, null, null, null);

    private List<BigDecimal> marks(String csv) {
        return java.util.Arrays.stream(csv.split(",")).map(String::trim).map(BigDecimal::new).toList();
    }

    @Test void acceptsUnrestrictedEqualMarkChoices() {
        assertTrue(service.allCombinationsTotal(marks("17.5,17.5,17.5,17.5,17.5,17.5"), 4, Set.of(), "", 70));
    }

    @Test void rejectsUnrestrictedChoicesWithDifferentPossibleTotals() {
        assertFalse(service.allCombinationsTotal(marks("10,20,30"), 2, Set.of(), "", 40));
    }

    @Test void acceptsCompulsoryQuestionAndEqualOptionalChoices() {
        assertTrue(service.allCombinationsTotal(marks("20,25,25,25"), 3, Set.of(1), "", 70));
    }

    @Test void acceptsOneChoiceFromEachRestrictedGroup() {
        assertTrue(service.allCombinationsTotal(marks("20,20,30,30"), 2, Set.of(), "1,2|3,4", 50));
    }

    @Test void rejectsOverlappingRestrictedGroups() {
        assertFalse(service.allCombinationsTotal(marks("20,20,30,30"), 2, Set.of(), "1,2|2,3", 50));
    }

    @Test void supportsFreeChoiceQuestionsAlongsideRestrictedGroups() {
        assertTrue(service.allCombinationsTotal(marks("20,20,10,10"), 2, Set.of(), "1,2", 30));
    }

    @Test void rejectsCompulsoryQuestionInsideChoiceGroup() {
        assertFalse(service.allCombinationsTotal(marks("20,20,30,30"), 2, Set.of(1), "1,2", 50));
    }
}