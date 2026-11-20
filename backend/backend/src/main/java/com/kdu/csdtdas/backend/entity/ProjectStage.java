package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_stages", uniqueConstraints = @UniqueConstraint(columnNames = {"supervision_id", "stage"}))
public class ProjectStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervision_id", nullable = false)
    private ProjectSupervision supervision;

    @Column(nullable = false, length = 12)
    private String stage;

    /** NOT_STARTED, IN_PROGRESS, SUBMITTED or APPROVED. */
    @Column(nullable = false, length = 12)
    private String status = "NOT_STARTED";

    @Column(length = 300)
    private String note;

    @Column(length = 100)
    private String updatedBy;

    private LocalDateTime updatedAt;

    public ProjectStage() {}

    public ProjectSupervision getSupervision() { return supervision; }
    public void setSupervision(ProjectSupervision supervision) { this.supervision = supervision; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}