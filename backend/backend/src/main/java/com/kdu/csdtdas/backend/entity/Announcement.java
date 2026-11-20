package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 4000)
    private String body;

    /** ALL, STAFF, STUDENTS, PROGRAMME_LEVEL or COURSE. */
    @Column(nullable = false, length = 20)
    private String audience;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programme_id")
    private Programme programme;

    private Integer levelNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    /** NORMAL or URGENT. */
    @Column(nullable = false, length = 10)
    private String priority = "NORMAL";

    @Column(nullable = false)
    private boolean pinned;

    private LocalDate expiresOn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "posted_by_id", nullable = false)
    private User postedBy;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Announcement() {}

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
    public Programme getProgramme() { return programme; }
    public void setProgramme(Programme programme) { this.programme = programme; }
    public Integer getLevelNumber() { return levelNumber; }
    public void setLevelNumber(Integer levelNumber) { this.levelNumber = levelNumber; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }
    public User getPostedBy() { return postedBy; }
    public void setPostedBy(User postedBy) { this.postedBy = postedBy; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}