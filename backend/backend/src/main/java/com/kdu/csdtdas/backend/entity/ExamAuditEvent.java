package com.kdu.csdtdas.backend.entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="exam_audit_events")
public class ExamAuditEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="actor_id") private User actor;
 @Column(nullable=false,length=60) private String action;
 @Column(nullable=false,length=100) private String targetType;
 @Column(nullable=false) private Long targetId;
 @Column(length=2000) private String details;
 @Column(nullable=false) private LocalDateTime occurredAt;
 public ExamAuditEvent(){}
 @PrePersist public void prePersist(){if(occurredAt==null)occurredAt=LocalDateTime.now();}
 public void setActor(User v){actor=v;} public void setAction(String v){action=v;}
 public void setTargetType(String v){targetType=v;} public void setTargetId(Long v){targetId=v;}
 public void setDetails(String v){details=v;} public void setOccurredAt(LocalDateTime v){occurredAt=v;}
}