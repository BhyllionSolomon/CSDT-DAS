package com.kdu.csdtdas.backend.dto;

import com.kdu.csdtdas.backend.entity.OnlineClass;

import java.time.LocalDateTime;

public record OnlineClassResponse(
        Long id,
        Long courseId,
        String courseCode,
        String courseTitle,
        String academicSessionName,
        String semester,
        String title,
        String status,
        LocalDateTime scheduledAt,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String lecturerName,
        long participantCount
) {
    public static OnlineClassResponse from(OnlineClass oc, long participantCount) {
        return new OnlineClassResponse(
                oc.getId(),
                oc.getCourse().getId(),
                oc.getCourse().getCode(),
                oc.getCourse().getTitle(),
                oc.getAcademicSession().getName(),
                oc.getSemester(),
                oc.getTitle(),
                oc.getStatus(),
                oc.getScheduledAt(),
                oc.getStartedAt(),
                oc.getEndedAt(),
                oc.getLecturer().getFullName(),
                participantCount
        );
    }

    public record Participant(String matricNumber, String fullName, LocalDateTime joinedAt) {}

    public record JoinLink(String joinUrl) {}
}