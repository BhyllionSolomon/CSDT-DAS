package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "project_survey_windows", uniqueConstraints = @UniqueConstraint(columnNames = "academic_session_id"))
public class ProjectSurveyWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    public ProjectSurveyWindow() {}

    public AcademicSession getAcademicSession() { return academicSession; }
    public void setAcademicSession(AcademicSession academicSession) { this.academicSession = academicSession; }
    public boolean isOpen() { return open; }
    public void setOpen(boolean open) { this.open = open; }
}