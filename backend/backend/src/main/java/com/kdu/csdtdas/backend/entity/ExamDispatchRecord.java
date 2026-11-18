package com.kdu.csdtdas.backend.entity;
import jakarta.persistence.*;import java.time.*;
@Entity @Table(name="exam_dispatch_records")
public class ExamDispatchRecord{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="task_id") private ExamCourseTask task;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="dispatch_user_id") private User dispatchUser;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assigned_by_id") private User assignedBy;
 private LocalDateTime assignedAt,collectedAt,deliveredAt,scriptsReceivedAt;
 @Column(length=120) private String venue;
 private Integer scriptsCount;
 @Column(length=150) private String returnedByName;
 @Column(length=120) private String returnedByUnit;
 @Column(length=120) private String returnedByRole;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="received_by_id") private User receivedBy;
 @Column(length=2000) private String notes;
 public ExamDispatchRecord(){} @PrePersist void init(){if(assignedAt==null)assignedAt=LocalDateTime.now();}
 public Long getId(){return id;} public ExamCourseTask getTask(){return task;} public void setTask(ExamCourseTask v){task=v;}
 public User getDispatchUser(){return dispatchUser;} public void setDispatchUser(User v){dispatchUser=v;} public User getAssignedBy(){return assignedBy;} public void setAssignedBy(User v){assignedBy=v;}
 public LocalDateTime getAssignedAt(){return assignedAt;} public void setAssignedAt(LocalDateTime v){assignedAt=v;} public LocalDateTime getCollectedAt(){return collectedAt;} public void setCollectedAt(LocalDateTime v){collectedAt=v;}
 public LocalDateTime getDeliveredAt(){return deliveredAt;} public void setDeliveredAt(LocalDateTime v){deliveredAt=v;} public LocalDateTime getScriptsReceivedAt(){return scriptsReceivedAt;} public void setScriptsReceivedAt(LocalDateTime v){scriptsReceivedAt=v;}
 public String getVenue(){return venue;} public void setVenue(String v){venue=v;} public Integer getScriptsCount(){return scriptsCount;} public void setScriptsCount(Integer v){scriptsCount=v;}
 public String getReturnedByName(){return returnedByName;} public void setReturnedByName(String v){returnedByName=v;} public String getReturnedByUnit(){return returnedByUnit;} public void setReturnedByUnit(String v){returnedByUnit=v;}
 public String getReturnedByRole(){return returnedByRole;} public void setReturnedByRole(String v){returnedByRole=v;} public User getReceivedBy(){return receivedBy;} public void setReceivedBy(User v){receivedBy=v;}
 public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}