package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "timetable_windows",
        uniqueConstraints = @UniqueConstraint(columnNames = {"academic_session_id", "semester"})
)
public class TimetableWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    @Column(nullable = false, length = 20)
    private String semester;

    /** OPEN (lecturers choose), CLOSED (waiting for the H.O.D), PUBLISHED. */
    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(nullable = false)
    private LocalDateTime deadline;

    private LocalDateTime openedAt;
    private LocalDateTime autoFilledAt;
    private LocalDateTime publishedAt;

    @Column(length = 100)
    private String openedBy;

    @Column(length = 100)
    private String publishedBy;

    public TimetableWindow() {}

    public Long getId() { return id; }
    public AcademicSession getAcademicSession() { return academicSession; }
    public void setAcademicSession(AcademicSession academicSession) { this.academicSession = academicSession; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public void setOpenedAt(LocalDateTime openedAt) { this.openedAt = openedAt; }
    public LocalDateTime getAutoFilledAt() { return autoFilledAt; }
    public void setAutoFilledAt(LocalDateTime autoFilledAt) { this.autoFilledAt = autoFilledAt; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public String getOpenedBy() { return openedBy; }
    public void setOpenedBy(String openedBy) { this.openedBy = openedBy; }
    public String getPublishedBy() { return publishedBy; }
    public void setPublishedBy(String publishedBy) { this.publishedBy = publishedBy; }
}