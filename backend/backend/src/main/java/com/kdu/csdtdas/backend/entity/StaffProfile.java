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

    /** Exact prefix printed before the staff name in the official Word allocation. */
    @Column(length = 20)
    private String displayTitle = "";

    /** Lower values appear earlier in the official supervisor allocation document. */
    @Column(nullable = false, columnDefinition = "integer default 9999")
    private int displayOrder = 9999;

    /** HOD-selected subset eligible to receive First Class students. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean firstClassEligible;

    @Column(nullable = false)
    private boolean projectCoordinator;

    @Column(nullable = false)
    private boolean assistantCoordinator;

    @Column(nullable = false, columnDefinition = "integer default 5")
    private int maxSupervisionLoad = 5;

    public StaffProfile() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getDisplayTitle() { return displayTitle; }
    public void setDisplayTitle(String displayTitle) { this.displayTitle = displayTitle == null ? "" : displayTitle.trim(); }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public boolean isSupervisionEligible() { return supervisionEligible; }
    public void setSupervisionEligible(boolean supervisionEligible) { this.supervisionEligible = supervisionEligible; }
    public boolean isFirstClassEligible() { return firstClassEligible; }
    public void setFirstClassEligible(boolean firstClassEligible) { this.firstClassEligible = firstClassEligible; }
    public boolean isProjectCoordinator() { return projectCoordinator; }
    public void setProjectCoordinator(boolean projectCoordinator) { this.projectCoordinator = projectCoordinator; }
    public boolean isAssistantCoordinator() { return assistantCoordinator; }
    public int getMaxSupervisionLoad() { return maxSupervisionLoad; }
    public void setMaxSupervisionLoad(int maxSupervisionLoad) { this.maxSupervisionLoad = maxSupervisionLoad; }
    public void setAssistantCoordinator(boolean assistantCoordinator) { this.assistantCoordinator = assistantCoordinator; }
}