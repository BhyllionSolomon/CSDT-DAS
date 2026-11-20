package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_allocation_draft_items",
       uniqueConstraints = @UniqueConstraint(columnNames = {"draft_id", "student_id"}))
public class ProjectAllocationDraftItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private ProjectAllocationDraft draft;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;

    @Column(nullable = false, length = 20)
    private String classification;

    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public ProjectAllocationDraftItem() {}
    @PrePersist @PreUpdate public void touch(){updatedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public ProjectAllocationDraft getDraft(){return draft;}
    public void setDraft(ProjectAllocationDraft v){draft=v;}
    public Student getStudent(){return student;}
    public void setStudent(Student v){student=v;}
    public User getSupervisor(){return supervisor;}
    public void setSupervisor(User v){supervisor=v;}
    public String getClassification(){return classification;}
    public void setClassification(String v){classification=v;}
    public BigDecimal getCgpa(){return cgpa;}
    public void setCgpa(BigDecimal v){cgpa=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}
