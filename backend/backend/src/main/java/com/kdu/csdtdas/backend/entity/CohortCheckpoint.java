package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** The H.O.D's confirmation that a cohort's student list or results for one session are complete. */
@Entity
@Table(
        name = "cohort_checkpoints",
        uniqueConstraints = @UniqueConstraint(columnNames = {"intake_year", "session_year", "kind"})
)
public class CohortCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "intake_year", nullable = false)
    private int intakeYear;

    @Column(name = "session_year", nullable = false)
    private int sessionYear;

    /** STUDENTS or RESULTS. */
    @Column(nullable = false, length = 10)
    private String kind;

    @Column(length = 100)
    private String verifiedBy;

    @Column(nullable = false)
    private LocalDateTime verifiedAt;

    public CohortCheckpoint() {}

    @PrePersist
    public void prePersist() { verifiedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public int getIntakeYear() { return intakeYear; }
    public void setIntakeYear(int intakeYear) { this.intakeYear = intakeYear; }
    public int getSessionYear() { return sessionYear; }
    public void setSessionYear(int sessionYear) { this.sessionYear = sessionYear; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getVerifiedBy() { return verifiedBy; }
    public void setVerifiedBy(String verifiedBy) { this.verifiedBy = verifiedBy; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
}