package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ca_submissions", uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "student_id"}))
public class CaSubmission {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "item_id", nullable = false)
    private CaItem item;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @Column(nullable = false, length = 255) private String fileName;
    @Column(nullable = false, length = 100) private String contentType;
    @Column(nullable = false, columnDefinition = "bytea") private byte[] fileData;
    @Column(nullable = false) private LocalDateTime submittedAt;
    @Column(length = 2000) private String feedback;
    @Column(length = 100) private String gradedBy;
    @Column private LocalDateTime gradedAt;
    public CaSubmission() {}
    @PrePersist public void prePersist() { if (submittedAt == null) submittedAt = LocalDateTime.now(); }
    public Long getId(){return id;}
    public CaItem getItem(){return item;} public void setItem(CaItem v){item=v;}
    public Student getStudent(){return student;} public void setStudent(Student v){student=v;}
    public String getFileName(){return fileName;} public void setFileName(String v){fileName=v;}
    public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
    public byte[] getFileData(){return fileData;} public void setFileData(byte[] v){fileData=v;}
    public LocalDateTime getSubmittedAt(){return submittedAt;} public void setSubmittedAt(LocalDateTime v){submittedAt=v;}
    public String getFeedback(){return feedback;} public void setFeedback(String v){feedback=v;}
    public String getGradedBy(){return gradedBy;} public void setGradedBy(String v){gradedBy=v;}
    public LocalDateTime getGradedAt(){return gradedAt;} public void setGradedAt(LocalDateTime v){gradedAt=v;}
}