package com.kdu.csdtdas.backend.entity;
import jakarta.persistence.*;import java.time.*;
@Entity @Table(name="exam_sittings")
public class ExamSitting{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="task_id",nullable=false) private ExamCourseTask task;
 @Column(length=120) private String venue;
 private LocalTime startTime; private LocalTime endTime;
 @Column(nullable=false) private boolean confirmed=false;
 @Column(nullable=false) private boolean published=false;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="confirmed_by_id") private User confirmedBy;
 private LocalDateTime confirmedAt;
 public ExamSitting(){} public Long getId(){return id;} public ExamCourseTask getTask(){return task;} public void setTask(ExamCourseTask v){task=v;}
 public String getVenue(){return venue;} public void setVenue(String v){venue=v;} public LocalTime getStartTime(){return startTime;} public void setStartTime(LocalTime v){startTime=v;}
 public LocalTime getEndTime(){return endTime;} public void setEndTime(LocalTime v){endTime=v;} public boolean isConfirmed(){return confirmed;} public void setConfirmed(boolean v){confirmed=v;}
 public boolean isPublished(){return published;} public void setPublished(boolean v){published=v;} public User getConfirmedBy(){return confirmedBy;} public void setConfirmedBy(User v){confirmedBy=v;}
 public LocalDateTime getConfirmedAt(){return confirmedAt;} public void setConfirmedAt(LocalDateTime v){confirmedAt=v;}
}