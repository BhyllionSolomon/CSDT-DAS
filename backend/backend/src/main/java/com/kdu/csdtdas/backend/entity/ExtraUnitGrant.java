package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "extra_unit_grants",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "academic_session_id", "semester"})
)
public class ExtraUnitGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    @Column(nullable = false, length = 20)
    private String semester;

    @Column(nullable = false)
    private int units;

    @Column(length = 300)
    private String note;

    @Column(length = 100)
    private String grantedBy;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    public ExtraUnitGrant() {}

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public AcademicSession getAcademicSession() { return academicSession; }
    public void setAcademicSession(AcademicSession academicSession) { this.academicSession = academicSession; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public int getUnits() { return units; }
    public void setUnits(int units) { this.units = units; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }
    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }
}