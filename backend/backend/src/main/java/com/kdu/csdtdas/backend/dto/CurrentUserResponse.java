package com.kdu.csdtdas.backend.dto;

public class CurrentUserResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private Long programmeId;
    private String programmeName;
    private Long studentId;
    private String matricNumber;
    private boolean assigned;

    public CurrentUserResponse() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getProgrammeId() { return programmeId; }
    public void setProgrammeId(Long programmeId) { this.programmeId = programmeId; }
    public String getProgrammeName() { return programmeName; }
    public void setProgrammeName(String programmeName) { this.programmeName = programmeName; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getMatricNumber() { return matricNumber; }
    public void setMatricNumber(String matricNumber) { this.matricNumber = matricNumber; }
    public boolean isAssigned() { return assigned; }
    public void setAssigned(boolean assigned) { this.assigned = assigned; }
}