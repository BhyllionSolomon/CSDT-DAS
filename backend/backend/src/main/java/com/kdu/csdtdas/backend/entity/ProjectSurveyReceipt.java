package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;

/** Only records that a student has already submitted the form. It holds no answers and no time. */
@Entity
@Table(name = "project_survey_receipts", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academic_session_id"}))
public class ProjectSurveyReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    public ProjectSurveyReceipt() {}

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public AcademicSession getAcademicSession() { return academicSession; }
    public void setAcademicSession(AcademicSession academicSession) { this.academicSession = academicSession; }
}