package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_papers")
public class ExamPaper {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "task_id")
    private ExamCourseTask task;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "lecturer_id")
    private User lecturer;
    @Column(nullable = false, length = 255) private String fileName;
    @Column(nullable = false, length = 100) private String contentType;
    @Column(nullable = false, columnDefinition = "bytea") private byte[] encryptedFile;
    @Column(nullable = false, length = 2000) private String wrappedDataKey;
    @Column(length = 64) private String contentSha256;
    @Column(nullable = false, length = 30) private String status = "SUBMITTED";
    @Column(nullable = false) private int version = 1;
    @Column(nullable = false) private LocalDateTime submittedAt;
    @Column(length = 4000) private String reviewNotes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;
    private LocalDateTime reviewedAt;
    public ExamPaper(){}
    @PrePersist public void prePersist(){if(submittedAt==null)submittedAt=LocalDateTime.now();}
    public Long getId(){return id;}
    public ExamCourseTask getTask(){return task;}
    public void setTask(ExamCourseTask v){task=v;}
    public User getLecturer(){return lecturer;}
    public void setLecturer(User v){lecturer=v;}
    public String getFileName(){return fileName;}
    public void setFileName(String v){fileName=v;}
    public String getContentType(){return contentType;}
    public void setContentType(String v){contentType=v;}
    public byte[] getEncryptedFile(){return encryptedFile;}
    public void setEncryptedFile(byte[] v){encryptedFile=v;}
    public String getContentSha256(){return contentSha256;}
    public void setContentSha256(String v){contentSha256=v;}
    public String getWrappedDataKey(){return wrappedDataKey;}
    public void setWrappedDataKey(String v){wrappedDataKey=v;}
    public String getStatus(){return status;}
    public void setStatus(String v){status=v;}
    public int getVersion(){return version;}
    public void setVersion(int v){version=v;}
    public LocalDateTime getSubmittedAt(){return submittedAt;}
    public void setSubmittedAt(LocalDateTime v){submittedAt=v;}
    public String getReviewNotes(){return reviewNotes;}
    public void setReviewNotes(String v){reviewNotes=v;}
    public User getReviewedBy(){return reviewedBy;}
    public void setReviewedBy(User v){reviewedBy=v;}
    public LocalDateTime getReviewedAt(){return reviewedAt;}
    public void setReviewedAt(LocalDateTime v){reviewedAt=v;}
}