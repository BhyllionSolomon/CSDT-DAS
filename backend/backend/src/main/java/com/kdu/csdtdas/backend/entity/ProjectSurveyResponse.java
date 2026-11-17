package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * One anonymous integrity form. It holds no student, no id of a student and no time of day. It is linked to
 * the supervisor so the H.O.D can act on what is reported.
 */
@Entity
@Table(name = "project_survey_responses")
public class ProjectSurveyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_session_id", nullable = false)
    private AcademicSession academicSession;

    @Column(length = 3) private String moneyCollected;
    @Column(length = 500) private String moneyDetails;
    @Column(length = 3) private String inappropriate;
    @Column(length = 500) private String inappropriateDetails;
    @Column(length = 3) private String ghostwritten;
    @Column(length = 500) private String ghostwrittenDetails;

    private int professional;
    private int helpful;
    private int available;
    private int overall;

    @Column(length = 1000) private String impact;
    @Column(length = 1000) private String problemSolved;
    @Column(length = 3) private String acknowledged;
    @Column(length = 500) private String acknowledgedDetails;
    @Column(length = 1000) private String comment;

    public ProjectSurveyResponse() {}

    public User getSupervisor() { return supervisor; }
    public void setSupervisor(User supervisor) { this.supervisor = supervisor; }
    public AcademicSession getAcademicSession() { return academicSession; }
    public void setAcademicSession(AcademicSession academicSession) { this.academicSession = academicSession; }
    public String getMoneyCollected() { return moneyCollected; }
    public void setMoneyCollected(String v) { this.moneyCollected = v; }
    public String getMoneyDetails() { return moneyDetails; }
    public void setMoneyDetails(String v) { this.moneyDetails = v; }
    public String getInappropriate() { return inappropriate; }
    public void setInappropriate(String v) { this.inappropriate = v; }
    public String getInappropriateDetails() { return inappropriateDetails; }
    public void setInappropriateDetails(String v) { this.inappropriateDetails = v; }
    public String getGhostwritten() { return ghostwritten; }
    public void setGhostwritten(String v) { this.ghostwritten = v; }
    public String getGhostwrittenDetails() { return ghostwrittenDetails; }
    public void setGhostwrittenDetails(String v) { this.ghostwrittenDetails = v; }
    public int getProfessional() { return professional; }
    public void setProfessional(int v) { this.professional = v; }
    public int getHelpful() { return helpful; }
    public void setHelpful(int v) { this.helpful = v; }
    public int getAvailable() { return available; }
    public void setAvailable(int v) { this.available = v; }
    public int getOverall() { return overall; }
    public void setOverall(int v) { this.overall = v; }
    public String getImpact() { return impact; }
    public void setImpact(String v) { this.impact = v; }
    public String getProblemSolved() { return problemSolved; }
    public void setProblemSolved(String v) { this.problemSolved = v; }
    public String getAcknowledged() { return acknowledged; }
    public void setAcknowledged(String v) { this.acknowledged = v; }
    public String getAcknowledgedDetails() { return acknowledgedDetails; }
    public void setAcknowledgedDetails(String v) { this.acknowledgedDetails = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { this.comment = v; }
}