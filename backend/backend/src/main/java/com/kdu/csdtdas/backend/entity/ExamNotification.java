package com.kdu.csdtdas.backend.entity;
import jakarta.persistence.*;import java.time.*;
@Entity @Table(name="exam_notifications",uniqueConstraints=@UniqueConstraint(columnNames={"recipient_id","task_id","notification_key"}))
public class ExamNotification{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="recipient_id") private User recipient;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="task_id") private ExamCourseTask task;
 @Column(nullable=false,length=80) private String notificationKey;
 @Column(nullable=false,length=120) private String title;
 @Column(nullable=false,length=2000) private String message;
 @Column(nullable=false) private LocalDateTime createdAt;
 private LocalDateTime readAt;
 public ExamNotification(){} @PrePersist void init(){createdAt=LocalDateTime.now();}
 public Long getId(){return id;} public User getRecipient(){return recipient;} public void setRecipient(User v){recipient=v;} public ExamCourseTask getTask(){return task;} public void setTask(ExamCourseTask v){task=v;}
 public String getNotificationKey(){return notificationKey;} public void setNotificationKey(String v){notificationKey=v;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public String getMessage(){return message;} public void setMessage(String v){message=v;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getReadAt(){return readAt;} public void setReadAt(LocalDateTime v){readAt=v;}
}