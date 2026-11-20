package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_allocation_events")
public class ProjectAllocationEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "draft_id", nullable = false)
    private ProjectAllocationDraft draft;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(length = 150)
    private String previousSupervisor;

    @Column(length = 150)
    private String newSupervisor;

    @Column(nullable = false, length = 100)
    private String changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @Column(length = 500)
    private String note;

    public ProjectAllocationEvent() {}
    @PrePersist public void prePersist(){if(changedAt==null) changedAt=LocalDateTime.now();}
    public void setDraft(ProjectAllocationDraft v){draft=v;}
    public void setStudent(Student v){student=v;}
    public void setAction(String v){action=v;}
    public void setPreviousSupervisor(String v){previousSupervisor=v;}
    public void setNewSupervisor(String v){newSupervisor=v;}
    public void setChangedBy(String v){changedBy=v;}
    public void setChangedAt(LocalDateTime v){changedAt=v;}
    public void setNote(String v){note=v;}
    public ProjectAllocationDraft getDraft(){return draft;}
    public Student getStudent(){return student;}
    public String getAction(){return action;}
    public String getPreviousSupervisor(){return previousSupervisor;}
    public String getNewSupervisor(){return newSupervisor;}
    public String getChangedBy(){return changedBy;}
    public LocalDateTime getChangedAt(){return changedAt;}
    public String getNote(){return note;}
}
