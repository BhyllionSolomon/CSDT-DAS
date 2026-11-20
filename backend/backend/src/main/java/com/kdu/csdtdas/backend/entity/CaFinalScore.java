package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="ca_final_scores", uniqueConstraints=@UniqueConstraint(columnNames={"course_id","academic_session_id","student_id","semester"}))
public class CaFinalScore {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="course_id",nullable=false) private Course course;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="academic_session_id",nullable=false) private AcademicSession academicSession;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="student_id",nullable=false) private Student student;
    @Column(nullable=false,length=20) private String semester;
    @Column(nullable=false,precision=9,scale=2) private BigDecimal rawTotal;
    @Column(nullable=false,precision=5,scale=2) private BigDecimal finalScore;
    @Column(nullable=false,length=100) private String aggregatedBy;
    @Column(nullable=false) private LocalDateTime aggregatedAt;
    public CaFinalScore() {}
    public Long getId(){return id;}
    public Course getCourse(){return course;} public void setCourse(Course v){course=v;}
    public AcademicSession getAcademicSession(){return academicSession;} public void setAcademicSession(AcademicSession v){academicSession=v;}
    public Student getStudent(){return student;} public void setStudent(Student v){student=v;}
    public String getSemester(){return semester;} public void setSemester(String v){semester=v;}
    public BigDecimal getRawTotal(){return rawTotal;} public void setRawTotal(BigDecimal v){rawTotal=v;}
    public BigDecimal getFinalScore(){return finalScore;} public void setFinalScore(BigDecimal v){finalScore=v;}
    public String getAggregatedBy(){return aggregatedBy;} public void setAggregatedBy(String v){aggregatedBy=v;}
    public LocalDateTime getAggregatedAt(){return aggregatedAt;} public void setAggregatedAt(LocalDateTime v){aggregatedAt=v;}
}