package com.kdu.csdtdas.backend.entity;import jakarta.persistence.*;import java.time.*;
@Entity @Table(name="exam_script_returns")
public class ExamScriptReturn{@Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
@ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="batch_id") private ExamScriptBatch batch;
@ManyToOne(optional=false,fetch=FetchType.LAZY) @JoinColumn(name="returned_by_id") private User returnedBy;
@Column(nullable=false) private Integer quantity;@Column(nullable=false) private LocalDateTime returnedAt;@Column(length=2000) private String notes;
public ExamScriptReturn(){}@PrePersist void init(){returnedAt=LocalDateTime.now();}
public Long getId(){return id;}public ExamScriptBatch getBatch(){return batch;}public void setBatch(ExamScriptBatch v){batch=v;}public User getReturnedBy(){return returnedBy;}public void setReturnedBy(User v){returnedBy=v;}public Integer getQuantity(){return quantity;}public void setQuantity(Integer v){quantity=v;}public LocalDateTime getReturnedAt(){return returnedAt;}public String getNotes(){return notes;}public void setNotes(String v){notes=v;}}