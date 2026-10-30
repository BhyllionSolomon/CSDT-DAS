package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "registration_clearances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academic_session_id"})
)
public class RegistrationClearance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    /** As the student typed it. */
    @Column(nullable = false, length = 40)
    private String invoiceCode;

    /** Upper-case letters and digits only, used to spot the same invoice used twice. */
    @Column(nullable = false, length = 40)
    private String invoiceKey;

    @Column(nullable = false, unique = true, length = 30)
    private String registrationCode;

    /** PENDING, APPROVED, REJECTED or FLAGGED. */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(length = 300)
    private String note;

    /** STUDENT, HOD or ROLLOUT. */
    @Column(nullable = false, length = 20)
    private String source;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    private LocalDateTime decidedAt;

    @Column(length = 100)
    private String decidedBy;

    public RegistrationClearance() {}

    @PrePersist
    public void prePersist() {
        if (submittedAt == null) submittedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public AcademicSession getAcademicSession() { return academicSession; }
    public void setAcademicSession(AcademicSession academicSession) { this.academicSession = academicSession; }
    public String getInvoiceCode() { return invoiceCode; }
    public void setInvoiceCode(String invoiceCode) { this.invoiceCode = invoiceCode; }
    public String getInvoiceKey() { return invoiceKey; }
    public void setInvoiceKey(String invoiceKey) { this.invoiceKey = invoiceKey; }
    public String getRegistrationCode() { return registrationCode; }
    public void setRegistrationCode(String registrationCode) { this.registrationCode = registrationCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime decidedAt) { this.decidedAt = decidedAt; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String decidedBy) { this.decidedBy = decidedBy; }
}