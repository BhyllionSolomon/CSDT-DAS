package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "online_class_joins",
        uniqueConstraints = { @UniqueConstraint(columnNames = {"online_class_id", "user_id"}) }
)
public class OnlineClassJoin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "online_class_id", nullable = false)
    private OnlineClass onlineClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    public OnlineClassJoin() {}

    @PrePersist
    public void prePersist() { joinedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public OnlineClass getOnlineClass() { return onlineClass; }
    public void setOnlineClass(OnlineClass onlineClass) { this.onlineClass = onlineClass; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}