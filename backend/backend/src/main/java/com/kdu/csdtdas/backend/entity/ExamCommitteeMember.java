package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_committee_members", uniqueConstraints = @UniqueConstraint(columnNames = {"cycle_id", "user_id"}))
public class ExamCommitteeMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "cycle_id")
    private ExamCycle cycle;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "appointed_by_id")
    private User appointedBy;
    @Column(nullable = false) private LocalDateTime appointedAt;
    public ExamCommitteeMember(){}
    @PrePersist public void prePersist(){appointedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public ExamCycle getCycle(){return cycle;}
    public void setCycle(ExamCycle v){cycle=v;}
    public User getUser(){return user;}
    public void setUser(User v){user=v;}
    public User getAppointedBy(){return appointedBy;}
    public void setAppointedBy(User v){appointedBy=v;}
    public LocalDateTime getAppointedAt(){return appointedAt;}
}