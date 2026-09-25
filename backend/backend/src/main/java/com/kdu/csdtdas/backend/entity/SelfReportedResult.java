package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "self_reported_results")
public class SelfReportedResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, length = 20)
    private String courseCode;

    @Column(length = 200)
    private String courseTitle;

    @Column(nullable = false)
    private Double totalScore;

    @Column(length = 5)
    private String grade;

    @Column(length = 20)
    private String semester;

    @Column(length = 20)
    private String sessionLabel;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    public SelfReportedResult() {}

    @PrePersist
    public void prePersist() { uploadedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public Double getTotalScore() { return totalScore; }
    public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getSessionLabel() { return sessionLabel; }
    public void setSessionLabel(String sessionLabel) { this.sessionLabel = sessionLabel; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}