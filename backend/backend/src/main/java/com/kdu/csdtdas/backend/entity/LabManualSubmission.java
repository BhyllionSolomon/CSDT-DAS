package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="lab_manual_submissions", indexes={
 @Index(name="idx_manual_submission_status", columnList="status"),
 @Index(name="idx_manual_submission_owner", columnList="uploaded_by")
})
public class LabManualSubmission {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=150) private String title;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="course_id",nullable=false) private Course course;
 @Column(nullable=false) private Integer pageCount;
 @Column(precision=10,scale=2) private BigDecimal suggestedLevy;
 @Column(precision=10,scale=2) private BigDecimal approvedLevy;
 @Column(nullable=false,length=30) private String status="SUBMITTED_TO_HOD";
 @Column(length=40) private String targetLab;
 @Column(nullable=false,length=100) private String uploadedBy;
 @Column(length=100) private String hodReviewedBy;
 @Column(length=500) private String hodComment;
 @Column(length=100) private String sentToLabBy;
 @Column(length=100) private String receivedBy;
 @Column(length=255) private String fileName;
 @Column(nullable=false,length=100) private String contentType="application/pdf";
 @Basic(fetch=FetchType.LAZY) @Column(nullable=false,columnDefinition="bytea") private byte[] fileData;
 @Column(nullable=false) private LocalDateTime submittedAt;
 private LocalDateTime approvedAt;
 private LocalDateTime sentToLabAt;
 private LocalDateTime receivedAt;
 @PrePersist public void prePersist(){submittedAt=LocalDateTime.now();}
 public Long getId(){return id;} public String getTitle(){return title;} public void setTitle(String v){title=v;}
 public Course getCourse(){return course;} public void setCourse(Course v){course=v;}
 public Integer getPageCount(){return pageCount;} public void setPageCount(Integer v){pageCount=v;}
 public BigDecimal getSuggestedLevy(){return suggestedLevy;} public void setSuggestedLevy(BigDecimal v){suggestedLevy=v;}
 public BigDecimal getApprovedLevy(){return approvedLevy;} public void setApprovedLevy(BigDecimal v){approvedLevy=v;}
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 public String getTargetLab(){return targetLab;} public void setTargetLab(String v){targetLab=v;}
 public String getUploadedBy(){return uploadedBy;} public void setUploadedBy(String v){uploadedBy=v;}
 public String getHodReviewedBy(){return hodReviewedBy;} public void setHodReviewedBy(String v){hodReviewedBy=v;}
 public String getHodComment(){return hodComment;} public void setHodComment(String v){hodComment=v;}
 public String getSentToLabBy(){return sentToLabBy;} public void setSentToLabBy(String v){sentToLabBy=v;}
 public String getReceivedBy(){return receivedBy;} public void setReceivedBy(String v){receivedBy=v;}
 public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
 public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
 public byte[] getFileData(){return fileData;} public void setFileData(byte[] v){fileData=v;}
 public LocalDateTime getSubmittedAt(){return submittedAt;} public LocalDateTime getApprovedAt(){return approvedAt;} public void setApprovedAt(LocalDateTime v){approvedAt=v;}
 public LocalDateTime getSentToLabAt(){return sentToLabAt;} public void setSentToLabAt(LocalDateTime v){sentToLabAt=v;}
 public LocalDateTime getReceivedAt(){return receivedAt;} public void setReceivedAt(LocalDateTime v){receivedAt=v;}
}