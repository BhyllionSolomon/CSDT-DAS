package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_presentations", uniqueConstraints = @UniqueConstraint(columnNames = {"supervision_id", "round_no"}))
public class ProjectPresentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervision_id", nullable = false)
    private ProjectSupervision supervision;

    /** 1 for the first presentation, 2 for the first re-presentation, and so on. */
    @Column(name = "round_no", nullable = false)
    private int roundNo;

    private LocalDateTime scheduledAt;

    @Column(length = 100)
    private String venue;

    /** PENDING, PASSED, CORRECTIONS_REQUIRED or REPRESENT. */
    @Column(nullable = false, length = 22)
    private String outcome = "PENDING";

    @Column(length = 500)
    private String outcomeNote;

    @Column(length = 100)
    private String scheduledBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ProjectPresentation() {}

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public ProjectSupervision getSupervision() { return supervision; }
    public void setSupervision(ProjectSupervision supervision) { this.supervision = supervision; }
    public int getRoundNo() { return roundNo; }
    public void setRoundNo(int roundNo) { this.roundNo = roundNo; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public String getOutcomeNote() { return outcomeNote; }
    public void setOutcomeNote(String outcomeNote) { this.outcomeNote = outcomeNote; }
    public String getScheduledBy() { return scheduledBy; }
    public void setScheduledBy(String scheduledBy) { this.scheduledBy = scheduledBy; }
}