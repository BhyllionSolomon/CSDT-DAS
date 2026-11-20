package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "home_slides")
public class HomeSlide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int sortOrder;

    @Column(length = 60)
    private String eyebrow;

    @Column(nullable = false, length = 150)
    private String heading;

    @Column(length = 400)
    private String body;

    @Column(length = 100)
    private String sideHeading;

    @Column(length = 300)
    private String sideText;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean hasImage;

    /** Changes whenever the picture changes, so browsers fetch the new one. */
    @Column(nullable = false)
    private long imageVersion;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public HomeSlide() {}

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getEyebrow() { return eyebrow; }
    public void setEyebrow(String eyebrow) { this.eyebrow = eyebrow; }
    public String getHeading() { return heading; }
    public void setHeading(String heading) { this.heading = heading; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getSideHeading() { return sideHeading; }
    public void setSideHeading(String sideHeading) { this.sideHeading = sideHeading; }
    public String getSideText() { return sideText; }
    public void setSideText(String sideText) { this.sideText = sideText; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isHasImage() { return hasImage; }
    public void setHasImage(boolean hasImage) { this.hasImage = hasImage; }
    public long getImageVersion() { return imageVersion; }
    public void setImageVersion(long imageVersion) { this.imageVersion = imageVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}