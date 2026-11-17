package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_manual_orders", indexes = {
        @Index(name = "idx_lab_order_student_session", columnList = "student_id, academic_session_id"),
        @Index(name = "idx_lab_order_status", columnList = "status")
})
public class LabManualOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "manual_id", nullable = false)
    private LabManual manual;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 30)
    private String status = "AWAITING_PAYMENT";
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(length = 100)
    private String updatedBy;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    public LabManualOrder() {}
    @PrePersist public void beforeInsert() { LocalDateTime n=LocalDateTime.now(); createdAt=n; updatedAt=n; }
    @PreUpdate public void beforeUpdate() { updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;}
    public LabManual getManual(){return manual;} public void setManual(LabManual v){manual=v;}
    public Student getStudent(){return student;} public void setStudent(Student v){student=v;}
    public AcademicSession getAcademicSession(){return academicSession;} public void setAcademicSession(AcademicSession v){academicSession=v;}
    public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}