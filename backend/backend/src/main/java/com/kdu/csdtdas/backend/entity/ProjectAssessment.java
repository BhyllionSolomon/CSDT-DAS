package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** The supervisor's private assessment of one student's project. Students never see it. */
@Entity
@Table(name = "project_assessments", uniqueConstraints = @UniqueConstraint(columnNames = "supervision_id"))
public class ProjectAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervision_id", nullable = false)
    private ProjectSupervision supervision;

    /** YES, PARTLY or NO. */
    @Column(length = 8)
    private String problemStatement;

    @Column(length = 8)
    private String objectives;

    /** YES, NO or WITH_REVISION. */
    @Column(length = 14)
    private String publishable;

    @Column(precision = 5, scale = 2)
    private BigDecimal similarityPercent;

    @Column(precision = 5, scale = 2)
    private BigDecimal aiPercent;

    private LocalDate reportCheckedOn;

    private Integer diligence;
    private Integer communication;
    private Integer initiative;

    @Column(length = 600)
    private String comment;

    @Column(length = 100)
    private String updatedBy;

    private LocalDateTime updatedAt;

    public ProjectAssessment() {}

    public ProjectSupervision getSupervision() { return supervision; }
    public void setSupervision(ProjectSupervision supervision) { this.supervision = supervision; }
    public String getProblemStatement() { return problemStatement; }
    public void setProblemStatement(String problemStatement) { this.problemStatement = problemStatement; }
    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }
    public String getPublishable() { return publishable; }
    public void setPublishable(String publishable) { this.publishable = publishable; }
    public BigDecimal getSimilarityPercent() { return similarityPercent; }
    public void setSimilarityPercent(BigDecimal similarityPercent) { this.similarityPercent = similarityPercent; }
    public BigDecimal getAiPercent() { return aiPercent; }
    public void setAiPercent(BigDecimal aiPercent) { this.aiPercent = aiPercent; }
    public LocalDate getReportCheckedOn() { return reportCheckedOn; }
    public void setReportCheckedOn(LocalDate reportCheckedOn) { this.reportCheckedOn = reportCheckedOn; }
    public Integer getDiligence() { return diligence; }
    public void setDiligence(Integer diligence) { this.diligence = diligence; }
    public Integer getCommunication() { return communication; }
    public void setCommunication(Integer communication) { this.communication = communication; }
    public Integer getInitiative() { return initiative; }
    public void setInitiative(Integer initiative) { this.initiative = initiative; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}