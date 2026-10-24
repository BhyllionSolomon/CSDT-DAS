package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "timetable_entries", indexes = @Index(columnList = "window_id"))
public class TimetableEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "window_id", nullable = false)
    private TimetableWindow window;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id")
    private User lecturer;

    /** LECTURE or PRACTICAL. */
    @Column(nullable = false, length = 10)
    private String kind;

    /** MON to FRI. */
    @Column(nullable = false, length = 3)
    private String day;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private TimetableRoom room;

    @Column(length = 100)
    private String note;

    /** LECTURER, AUTO or HOD. */
    @Column(nullable = false, length = 10)
    private String source;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public TimetableEntry() {}

    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public TimetableWindow getWindow() { return window; }
    public void setWindow(TimetableWindow window) { this.window = window; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public User getLecturer() { return lecturer; }
    public void setLecturer(User lecturer) { this.lecturer = lecturer; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public TimetableRoom getRoom() { return room; }
    public void setRoom(TimetableRoom room) { this.room = room; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}