package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "timetable_rooms")
public class TimetableRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    /** LECTURE or LAB. */
    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false)
    private boolean active = true;

    public TimetableRoom() {}

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}