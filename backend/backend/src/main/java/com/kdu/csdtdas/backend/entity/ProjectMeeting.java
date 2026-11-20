package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_meetings")
public class ProjectMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervision_id", nullable = false)
    private ProjectSupervision supervision;

    @Column(nullable = false)
    private LocalDate meetingDate;

    /** IN_PERSON, ONLINE or PHONE. */
    @Column(nullable = false, length = 10)
    private String mode = "IN_PERSON";

    /** False when the student did not show up. */
    @Column(nullable = false)
    private boolean attended = true;

    @Column(length = 600)
    private String summary;

    @Column(length = 400)
    private String nextSteps;

    @Column(length = 100)
    private String loggedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public ProjectMeeting() {}

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public ProjectSupervision getSupervision() { return supervision; }
    public void setSupervision(ProjectSupervision supervision) { this.supervision = supervision; }
    public LocalDate getMeetingDate() { return meetingDate; }
    public void setMeetingDate(LocalDate meetingDate) { this.meetingDate = meetingDate; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public boolean isAttended() { return attended; }
    public void setAttended(boolean attended) { this.attended = attended; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getNextSteps() { return nextSteps; }
    public void setNextSteps(String nextSteps) { this.nextSteps = nextSteps; }
    public String getLoggedBy() { return loggedBy; }
    public void setLoggedBy(String loggedBy) { this.loggedBy = loggedBy; }
}