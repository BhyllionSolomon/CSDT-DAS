package com.kdu.csdtdas.backend.dto;

import java.util.List;

public final class AttendanceDTOs {

    private AttendanceDTOs() {}

    public record CourseInfo(Long courseId, String code, String title, String level, int classesHeld, int rosterSize) {}

    public record ClassRow(Long id, String date, String kind, String topic, int present, int total) {}

    public record RosterStudent(Long studentId, String matricNumber, String fullName, String status) {}

    public record Roster(Long classId, String topic, boolean existing, List<RosterStudent> students) {}

    public record Mark(Long studentId, String status) {}

    public record SaveRequest(Long courseId, Long sessionId, String semester, String date, String kind, String topic, List<Mark> marks) {}

    public record SummaryRow(
            Long studentId, String matricNumber, String fullName,
            int classes, int present, int late, int absent, int excused, Integer percent, boolean eligible
    ) {}

    public record Summary(int required, int classesHeld, List<SummaryRow> students) {}

    public record HistoryItem(String date, String kind, String status) {}

    public record MyCourse(
            Long courseId, String code, String title,
            int classes, int attended, int late, int absent, int excused, Integer percent, boolean eligible,
            List<HistoryItem> history
    ) {}

    public record Mine(int required, List<MyCourse> courses) {}
}