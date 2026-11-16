package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_allocation_drafts")
public class ProjectAllocationDraft {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(nullable = false, length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    @Column(length = 100)
    private String publishedBy;

    public ProjectAllocationDraft() {}
    @PrePersist public void prePersist() { LocalDateTime n=LocalDateTime.now(); createdAt=n; updatedAt=n; }
    @PreUpdate public void preUpdate() { updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;}
    public AcademicSession getAcademicSession(){return academicSession;}
    public void setAcademicSession(AcademicSession v){academicSession=v;}
    public String getStatus(){return status;}
    public void setStatus(String v){status=v;}
    public String getCreatedBy(){return createdBy;}
    public void setCreatedBy(String v){createdBy=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public LocalDateTime getPublishedAt(){return publishedAt;}
    public void setPublishedAt(LocalDateTime v){publishedAt=v;}
    public String getPublishedBy(){return publishedBy;}
    public void setPublishedBy(String v){publishedBy=v;}
}
