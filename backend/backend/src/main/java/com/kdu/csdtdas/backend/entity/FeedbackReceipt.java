package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

/** Only records that a student has already rated; it holds no scores and no time. */
@Entity
@Table(
        name = "feedback_receipts",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"student_id", "course_id", "academic_session_id", "semester", "lecturer_id"}
        )
)
public class FeedbackReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    @Column(nullable = false, length = 20)
    private String semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    public FeedbackReceipt() {}

    public UUID getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public AcademicSession getAcademicSession() { return academicSession; }
    public void setAcademicSession(AcademicSession academicSession) { this.academicSession = academicSession; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public User getLecturer() { return lecturer; }
    public void setLecturer(User lecturer) { this.lecturer = lecturer; }
}