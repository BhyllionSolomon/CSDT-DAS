package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "direct_entry_profiles", uniqueConstraints = @UniqueConstraint(columnNames = "student_id"))
public class DirectEntryProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** 200 or 300. */
    @Column(nullable = false)
    private int entryLevelNumber;

    @Column(length = 100)
    private String markedBy;

    @Column(nullable = false)
    private LocalDateTime markedAt;

    public DirectEntryProfile() {}

    public Long getId() { return id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public int getEntryLevelNumber() { return entryLevelNumber; }
    public void setEntryLevelNumber(int entryLevelNumber) { this.entryLevelNumber = entryLevelNumber; }
    public String getMarkedBy() { return markedBy; }
    public void setMarkedBy(String markedBy) { this.markedBy = markedBy; }
    public LocalDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }
}