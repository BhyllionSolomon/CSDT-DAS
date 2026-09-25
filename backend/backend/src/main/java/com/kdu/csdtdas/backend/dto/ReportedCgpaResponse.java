package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;

public class ReportedCgpaResponse {
    private Long id;
    private String matricNumber;
    private String fullName;
    private BigDecimal cgpa;
    private String uploadedBy;

    public ReportedCgpaResponse() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMatricNumber() { return matricNumber; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public BigDecimal getCgpa() { return cgpa; }
    public void setCgpa(BigDecimal cgpa) { this.cgpa = cgpa; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}