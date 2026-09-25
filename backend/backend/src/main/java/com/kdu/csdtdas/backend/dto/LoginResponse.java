package com.kdu.csdtdas.backend.dto;

public class LoginResponse {

    private String token;
    private String username;
    private String fullName;
    private String role;
    private Long programmeId;
    private String programmeName;

    public LoginResponse() {}

    public LoginResponse(String token, String username, String fullName, String role) {
        this.token = token; this.username = username;
        this.fullName = fullName; this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Long getProgrammeId() { return programmeId; }
    public void setProgrammeId(Long programmeId) { this.programmeId = programmeId; }
    public String getProgrammeName() { return programmeName; }
    public void setProgrammeName(String programmeName) { this.programmeName = programmeName; }
}