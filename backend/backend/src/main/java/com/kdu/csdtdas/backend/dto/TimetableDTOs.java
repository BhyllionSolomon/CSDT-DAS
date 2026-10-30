package com.kdu.csdtdas.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class TimetableDTOs {

    private TimetableDTOs() {}

    public record RoomDto(Long id, String name, String type, boolean active) {}

    public record WindowInfo(
            Long id, Long sessionId, String sessionName, String semester,
            String status, LocalDateTime deadline, LocalDateTime publishedAt
    ) {}

    public record EntryDto(
            Long id, Long courseId, String courseCode, String courseTitle, String level,
            String kind, String day, String startTime, String endTime,
            Long roomId, String room, String roomType,
            Long lecturerId, String lecturer, String note, String source
    ) {}

    public record CourseState(
            Long courseId, String code, String title, int unit, String level,
            List<String> lecturers, int sessions, boolean responded
    ) {}

    public record AdminStatus(
            WindowInfo window, int totalCourses, int respondedCourses,
            List<CourseState> courses, List<EntryDto> entries
    ) {}

    public record MineCourse(
            Long courseId, String code, String title, int unit, String level, List<EntryDto> entries
    ) {}

    public record MineView(WindowInfo window, boolean canEdit, List<MineCourse> courses) {}

    public record StudentView(WindowInfo window, boolean published, List<EntryDto> entries) {}

    public record AutoFillResult(int coursesPlaced, int sessionsAdded, List<String> unplaced) {}

    public record Message(String message) {}

    // ---- requests

    public record OpenWindow(Long sessionId, String semester, String deadline) {}

    public record DeadlineRequest(String deadline) {}

    public record PublishRequest(Boolean force) {}

    public record RoomRequest(String name, String type) {}

    public record ActiveRequest(Boolean active) {}

    public record EntryRequest(
            Long sessionId, String semester, Long courseId, String kind, String day,
            String startTime, String endTime, Long roomId, String note, Long lecturerId
    ) {}
}