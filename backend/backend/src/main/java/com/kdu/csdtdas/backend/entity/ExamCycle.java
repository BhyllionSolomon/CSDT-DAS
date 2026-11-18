package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_cycles", uniqueConstraints = @UniqueConstraint(columnNames = {"academic_session_id", "semester"}))
public class ExamCycle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "academic_session_id")
    private AcademicSession academicSession;
    @Column(nullable = false, length = 10) private String semester;
    @Column(nullable = false) private LocalDateTime createdAt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by_id")
    private User createdBy;
    @Column(nullable = false) private boolean active = true;
    public ExamCycle() {}
    @PrePersist public void prePersist() { createdAt = LocalDateTime.now(); }
    public Long getId(){return id;}
    public AcademicSession getAcademicSession(){return academicSession;}
    public void setAcademicSession(AcademicSession v){academicSession=v;}
    public String getSemester(){return semester;}
    public void setSemester(String v){semester=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public User getCreatedBy(){return createdBy;}
    public void setCreatedBy(User v){createdBy=v;}
    public boolean isActive(){return active;}
    public void setActive(boolean v){active=v;}
}