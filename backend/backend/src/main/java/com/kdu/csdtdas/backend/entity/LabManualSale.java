package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_manual_sales")
public class LabManualSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manual_id", nullable = false)
    private LabManual manual;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    /** The price on the day of the sale, so later price changes never rewrite history. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity = 1;

    @Column(nullable = false)
    private LocalDateTime soldAt;

    @Column(length = 100)
    private String recordedBy;

    @Column(nullable = false)
    private boolean voided;

    @Column(length = 100)
    private String voidedBy;

    private LocalDateTime voidedAt;

    public LabManualSale() {}

    public Long getId() { return id; }
    public LabManual getManual() { return manual; }
    public void setManual(LabManual manual) { this.manual = manual; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public AcademicSession getAcademicSession() { return academicSession; }
    public void setAcademicSession(AcademicSession academicSession) { this.academicSession = academicSession; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public LocalDateTime getSoldAt() { return soldAt; }
    public void setSoldAt(LocalDateTime soldAt) { this.soldAt = soldAt; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public boolean isVoided() { return voided; }
    public void setVoided(boolean voided) { this.voided = voided; }
    public String getVoidedBy() { return voidedBy; }
    public void setVoidedBy(String voidedBy) { this.voidedBy = voidedBy; }
    public LocalDateTime getVoidedAt() { return voidedAt; }
    public void setVoidedAt(LocalDateTime voidedAt) { this.voidedAt = voidedAt; }
}