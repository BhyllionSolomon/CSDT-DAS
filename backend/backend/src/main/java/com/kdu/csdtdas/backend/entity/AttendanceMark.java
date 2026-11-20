package com.kdu.csdtdas.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "attendance_marks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"class_id", "student_id"})
)
public class AttendanceMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private AttendanceClass attendanceClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** PRESENT, ABSENT, LATE or EXCUSED. */
    @Column(nullable = false, length = 10)
    private String status;

    public AttendanceMark() {}

    public Long getId() { return id; }
    public AttendanceClass getAttendanceClass() { return attendanceClass; }
    public void setAttendanceClass(AttendanceClass attendanceClass) { this.attendanceClass = attendanceClass; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}