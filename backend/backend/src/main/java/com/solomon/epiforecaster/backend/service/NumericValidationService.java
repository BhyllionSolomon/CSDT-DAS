package com.solomon.epiforecaster.backend.service;

import org.springframework.stereotype.Service;

@Service
public class NumericValidationService {

    public boolean isInteger(String value) {

        try {

            Integer.parseInt(value.trim());

            return true;

        } catch (Exception e) {

            return false;

        }

    }

}