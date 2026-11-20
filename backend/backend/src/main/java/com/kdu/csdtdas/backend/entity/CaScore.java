package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ca_scores",
        uniqueConstraints = @UniqueConstraint(columnNames = {"item_id", "student_id"})
)
public class CaScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private CaItem item;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(length = 100)
    private String recordedBy;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    public CaScore() {}

    public Long getId() { return id; }
    public CaItem getItem() { return item; }
    public void setItem(CaItem item) { this.item = item; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}