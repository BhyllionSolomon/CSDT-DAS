package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** What one assessor says the student must correct before the next presentation. */
@Entity
@Table(name = "presentation_feedback", uniqueConstraints = @UniqueConstraint(columnNames = {"presentation_id", "assessor_id"}))
public class PresentationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "presentation_id", nullable = false)
    private ProjectPresentation presentation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessor_id", nullable = false)
    private User assessor;

    @Column(nullable = false, length = 1500)
    private String comments;

    private LocalDateTime updatedAt;

    public PresentationFeedback() {}

    public ProjectPresentation getPresentation() { return presentation; }
    public void setPresentation(ProjectPresentation presentation) { this.presentation = presentation; }
    public User getAssessor() { return assessor; }
    public void setAssessor(User assessor) { this.assessor = assessor; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}