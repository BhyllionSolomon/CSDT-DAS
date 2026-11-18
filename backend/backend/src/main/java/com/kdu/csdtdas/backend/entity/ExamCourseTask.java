package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_course_tasks", uniqueConstraints = @UniqueConstraint(columnNames = {"cycle_id", "course_id"}))
public class ExamCourseTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cycle_id")
    private ExamCycle cycle;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_id")
    private Course course;
    @Column(nullable = false) private LocalDate examDate;
    @Column(nullable = false) private LocalDateTime submissionDeadline;
    @Column(nullable = false) private LocalDateTime vettingDate;
    @Column(nullable = false) private LocalDateTime correctionDeadline;
    private LocalDateTime markingDeadline;
    @Column(nullable = false) private int noticeDays = 49;
    @Column(nullable = false) private LocalDateTime createdAt;
    public ExamCourseTask() {}
    @PrePersist public void prePersist(){createdAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public ExamCycle getCycle(){return cycle;}
    public void setCycle(ExamCycle v){cycle=v;}
    public Course getCourse(){return course;}
    public void setCourse(Course v){course=v;}
    public LocalDate getExamDate(){return examDate;}
    public void setExamDate(LocalDate v){examDate=v;}
    public LocalDateTime getSubmissionDeadline(){return submissionDeadline;}
    public void setSubmissionDeadline(LocalDateTime v){submissionDeadline=v;}
    public LocalDateTime getVettingDate(){return vettingDate;}
    public void setVettingDate(LocalDateTime v){vettingDate=v;}
    public LocalDateTime getCorrectionDeadline(){return correctionDeadline;}
    public void setCorrectionDeadline(LocalDateTime v){correctionDeadline=v;}
    public LocalDateTime getMarkingDeadline(){return markingDeadline;}
    public void setMarkingDeadline(LocalDateTime v){markingDeadline=v;}
    public int getNoticeDays(){return noticeDays;}
    public void setNoticeDays(int v){noticeDays=v;}
}