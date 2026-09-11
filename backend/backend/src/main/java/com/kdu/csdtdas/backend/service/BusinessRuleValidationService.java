package com.kdu.csdtdas.backend.service;

import org.springframework.stereotype.Service;

@Service
public class BusinessRuleValidationService {

    public boolean isValidEpiWeek(Integer epiWeek) {
        return epiWeek != null &&
                epiWeek >= 1 &&
                epiWeek <= 53;
    }

    public boolean isNonNegative(Integer value) {
        return value != null &&
                value >= 0;
    }

    public boolean confirmedNotGreaterThanSuspected(
            Integer suspected,
            Integer confirmed) {

        return confirmed <= suspected;
    }

    public boolean deathsNotGreaterThanConfirmed(
            Integer deaths,
            Integer confirmed) {

        return deaths <= confirmed;
    }

}
