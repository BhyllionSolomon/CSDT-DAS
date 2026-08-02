package com.solomon.epiforecaster.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "disease_relationship")
public class DiseaseRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_disease_type_id", nullable = false)
    private DiseaseType sourceDisease;

    @ManyToOne
    @JoinColumn(name = "target_disease_type_id", nullable = false)
    private DiseaseType targetDisease;

    @Column(nullable = false)
    private Integer lagWeeks;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private Double strengthValue;

    private Double pValue;

    private LocalDateTime createdAt;

    public DiseaseRelationship() {
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public DiseaseType getSourceDisease() {
        return sourceDisease;
    }

    public DiseaseType getTargetDisease() {
        return targetDisease;
    }

    public Integer getLagWeeks() {
        return lagWeeks;
    }

    public String getMethod() {
        return method;
    }

    public Double getStrengthValue() {
        return strengthValue;
    }

    public Double getPValue() {
        return pValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setSourceDisease(DiseaseType sourceDisease) {
        this.sourceDisease = sourceDisease;
    }

    public void setTargetDisease(DiseaseType targetDisease) {
        this.targetDisease = targetDisease;
    }

    public void setLagWeeks(Integer lagWeeks) {
        this.lagWeeks = lagWeeks;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setStrengthValue(Double strengthValue) {
        this.strengthValue = strengthValue;
    }

    public void setPValue(Double pValue) {
        this.pValue = pValue;
    }
}