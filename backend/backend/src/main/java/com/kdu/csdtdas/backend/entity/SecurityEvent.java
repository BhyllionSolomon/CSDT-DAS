package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "security_events", indexes = @Index(columnList = "occurredAt"))
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(length = 100)
    private String username;

    @Column(length = 30)
    private String role;

    @Column(length = 60)
    private String ip;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(length = 300)
    private String detail;

    public SecurityEvent() {}

    @PrePersist
    public void prePersist() { if (occurredAt == null) occurredAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}