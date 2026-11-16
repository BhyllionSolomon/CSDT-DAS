package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_profiles", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** HEAD_OF_DEPARTMENT, PROFESSOR, ASSOCIATE_PROFESSOR, SENIOR_LECTURER, LECTURER_I, LECTURER_II, ASSISTANT_LECTURER or ADJUNCT. */
    @Column(nullable = false, length = 30)
    private String designation = "LECTURER_II";

    @Column(nullable = false)
    private boolean supervisionEligible;

    /** HOD-selected subset eligible to receive First Class students. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean firstClassEligible;

    @Column(nullable = false)
    private boolean projectCoordinator;

    @Column(nullable = false)
    private boolean assistantCoordinator;

    public StaffProfile() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public boolean isSupervisionEligible() { return supervisionEligible; }
    public void setSupervisionEligible(boolean supervisionEligible) { this.supervisionEligible = supervisionEligible; }
    public boolean isFirstClassEligible() { return firstClassEligible; }
    public void setFirstClassEligible(boolean firstClassEligible) { this.firstClassEligible = firstClassEligible; }
    public boolean isProjectCoordinator() { return projectCoordinator; }
    public void setProjectCoordinator(boolean projectCoordinator) { this.projectCoordinator = projectCoordinator; }
    public boolean isAssistantCoordinator() { return assistantCoordinator; }
    public void setAssistantCoordinator(boolean assistantCoordinator) { this.assistantCoordinator = assistantCoordinator; }
}