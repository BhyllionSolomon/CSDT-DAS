package com.solomon.epiforecaster.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "validation_result")
public class ValidationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_dataset_id", nullable = false)
    private RawDataset rawDataset;

    @Column(nullable = false)
    private Integer rowNumber;

    @Column(nullable = false)
    private String fieldName;

    @Column(length = 2000)
    private String fieldValue;

    @Column(nullable = false)
    private String validationType;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ValidationResult() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public RawDataset getRawDataset() {
        return rawDataset;
    }

    public void setRawDataset(RawDataset rawDataset) {
        this.rawDataset = rawDataset;
    }

    public Integer getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(Integer rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(String fieldValue) {
        this.fieldValue = fieldValue;
    }

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String errorMessage) {
        this.message = errorMessage;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}