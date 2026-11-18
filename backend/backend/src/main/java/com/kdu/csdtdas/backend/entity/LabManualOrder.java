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
    @Column(name = "paystack_reference", unique = true, length = 100)
    private String paystackReference;
    @Column(name = "paystack_authorization_url", length = 600)
    private String paystackAuthorizationUrl;
    @Column(name = "gateway_transaction_id", length = 80)
    private String gatewayTransactionId;
    @Column(name = "payment_channel", length = 40)
    private String paymentChannel;
    @Column(name = "receipt_number", unique = true, length = 80)
    private String receiptNumber;
    private LocalDateTime paidAt;
    private LocalDateTime issuedAt;
    @Column(length = 100)
    private String issuedBy;
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
    public String getPaystackReference(){return paystackReference;} public void setPaystackReference(String v){paystackReference=v;}
    public String getPaystackAuthorizationUrl(){return paystackAuthorizationUrl;} public void setPaystackAuthorizationUrl(String v){paystackAuthorizationUrl=v;}
    public String getGatewayTransactionId(){return gatewayTransactionId;} public void setGatewayTransactionId(String v){gatewayTransactionId=v;}
    public String getPaymentChannel(){return paymentChannel;} public void setPaymentChannel(String v){paymentChannel=v;}
    public String getReceiptNumber(){return receiptNumber;} public void setReceiptNumber(String v){receiptNumber=v;}
    public LocalDateTime getPaidAt(){return paidAt;} public void setPaidAt(LocalDateTime v){paidAt=v;}
    public LocalDateTime getIssuedAt(){return issuedAt;} public void setIssuedAt(LocalDateTime v){issuedAt=v;}
    public String getIssuedBy(){return issuedBy;} public void setIssuedBy(String v){issuedBy=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public String getUpdatedBy(){return updatedBy;} public void setUpdatedBy(String v){updatedBy=v;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
}