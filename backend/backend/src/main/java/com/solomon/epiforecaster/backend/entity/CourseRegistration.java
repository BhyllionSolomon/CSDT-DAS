package com.solomon.epiforecaster.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "course_registrations",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "student_id",
                                "course_id",
                                "academic_session_id",
                                "semester"
                        }
                )
        }
)
public class CourseRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false, length = 30)
    private String status = "REGISTERED";

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public CourseRegistration() {
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        registeredAt = now;
        updatedAt = now;

        if (status == null) {
            status = "REGISTERED";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public AcademicSession getAcademicSession() {
        return academicSession;
    }

    public void setAcademicSession(AcademicSession academicSession) {
        this.academicSession = academicSession;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}