package com.kdu.csdtdas.backend.entity;
import jakarta.persistence.*;import java.time.*;
@Entity @Table(name="exam_script_batches")
public class ExamScriptBatch{
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="task_id") private ExamCourseTask task;
 @ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="lecturer_id") private User lecturer;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="collected_by_id") private User collectedBy;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="returned_to_id") private User returnedTo;
 @Column(nullable=false) private Integer quantityCollected;
 private Integer quantityReturned;
 @Column(nullable=false) private LocalDateTime collectedAt;
 private LocalDateTime returnedAt;
 @Column(length=2000) private String notes;
 public ExamScriptBatch(){} @PrePersist void init(){if(collectedAt==null)collectedAt=LocalDateTime.now();}
 public Long getId(){return id;} public ExamCourseTask getTask(){return task;} public void setTask(ExamCourseTask v){task=v;} public User getLecturer(){return lecturer;} public void setLecturer(User v){lecturer=v;}
 public User getCollectedBy(){return collectedBy;} public void setCollectedBy(User v){collectedBy=v;} public User getReturnedTo(){return returnedTo;} public void setReturnedTo(User v){returnedTo=v;}
 public Integer getQuantityCollected(){return quantityCollected;} public void setQuantityCollected(Integer v){quantityCollected=v;} public Integer getQuantityReturned(){return quantityReturned;} public void setQuantityReturned(Integer v){quantityReturned=v;}
 public LocalDateTime getCollectedAt(){return collectedAt;} public void setCollectedAt(LocalDateTime v){collectedAt=v;} public LocalDateTime getReturnedAt(){return returnedAt;} public void setReturnedAt(LocalDateTime v){returnedAt=v;}
 public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}