package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One anonymous rating. It deliberately has no link to a student and no time of day;
 * only the date is kept, and the id is random.
 */
@Entity
@Table(name = "feedback_responses")
public class FeedbackResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    @Column(nullable = false, length = 20)
    private String semester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private User lecturer;

    private int clarity;
    private int preparation;
    private int punctuality;
    private int helpfulness;
    private int fairness;

    @Column(length = 500)
    private String comment;

    @Column(nullable = false)
    private LocalDate submittedOn;

    public FeedbackResponse() {}

    public UUID getId() { return id; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public AcademicSession getAcademicSession() { return academicSession; }
    public void setAcademicSession(AcademicSession academicSession) { this.academicSession = academicSession; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public User getLecturer() { return lecturer; }
    public void setLecturer(User lecturer) { this.lecturer = lecturer; }
    public int getClarity() { return clarity; }
    public void setClarity(int clarity) { this.clarity = clarity; }
    public int getPreparation() { return preparation; }
    public void setPreparation(int preparation) { this.preparation = preparation; }
    public int getPunctuality() { return punctuality; }
    public void setPunctuality(int punctuality) { this.punctuality = punctuality; }
    public int getHelpfulness() { return helpfulness; }
    public void setHelpfulness(int helpfulness) { this.helpfulness = helpfulness; }
    public int getFairness() { return fairness; }
    public void setFairness(int fairness) { this.fairness = fairness; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDate getSubmittedOn() { return submittedOn; }
    public void setSubmittedOn(LocalDate submittedOn) { this.submittedOn = submittedOn; }
}