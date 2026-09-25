package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;

public class ParsedCgpaRow {
    private String matricNumber;
    private String fullName;
    private BigDecimal cgpa;

    public ParsedCgpaRow() {}
    public ParsedCgpaRow(String matricNumber, String fullName, BigDecimal cgpa) {
        this.matricNumber = matricNumber; this.fullName = fullName; this.cgpa = cgpa;
    }
    public String getMatricNumber() { return matricNumber; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }
}