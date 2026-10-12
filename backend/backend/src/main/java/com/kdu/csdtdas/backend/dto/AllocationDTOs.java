package com.kdu.csdtdas.backend.dto;

import com.kdu.csdtdas.backend.entity.Course;
import com.kdu.csdtdas.backend.entity.CourseAllocation;
import com.kdu.csdtdas.backend.entity.User;

import java.util.List;

public final class AllocationDTOs {

    private AllocationDTOs() {}

    public record Entry(
            Long id, Long lecturerId, String lecturerName, String phoneNumber,
            Long courseId, String courseCode, String courseTitle, Integer creditUnit,
            String courseStatus, String levelName,
            Long academicSessionId, String academicSessionName, String semester,
            boolean claimed, String source
    ) {
        public static Entry from(CourseAllocation a) {
            Course c = a.getCourse();
            User l = a.getLecturer();

            String name = l != null ? l.getFullName() : a.getLecturerName();
            String phone = l != null && l.getPhoneNumber() != null ? l.getPhoneNumber() : a.getPhoneNumber();

            return new Entry(
                    a.getId(),
                    l != null ? l.getId() : null,
                    name,
                    phone,
                    c.getId(),
                    c.getCode(),
                    c.getTitle(),
                    c.getCreditUnit(),
                    c.getStatus(),
                    c.getLevel() != null ? c.getLevel().getName() : null,
                    a.getAcademicSession().getId(),
                    a.getAcademicSession().getName(),
                    a.getSemester(),
                    l != null,
                    a.getSource()
            );
        }
    }

    public record MyOptions(List<Entry> confirm, List<Entry> mine, List<Entry> open) {}

    public record Claim(Long courseId, Long academicSessionId, String semester) {}

    public record Offer(Long courseId, Long academicSessionId, String semester) {}

    public record AddLecturer(
            Long courseId, Long academicSessionId, String semester, Long lecturerId, String lecturerName
    ) {}

    // ---- document import ----

    public record MatchedCourse(Long id, String code, String title) {}

    public record NewCourse(
            String code, String title, Integer unit, String status,
            Long levelId, String levelName, List<Long> programmeIds, List<String> programmeNames
    ) {}

    public record PreviewRow(
            int rowNumber, String codeText, String titleText, Integer unit, String status,
            List<String> lecturers, List<MatchedCourse> matches, List<NewCourse> newCourses,
            List<String> warnings
    ) {}

    public record CommitRow(List<Long> courseIds, List<NewCourse> newCourses, List<String> lecturers) {}

    public record CommitRequest(Long academicSessionId, String semester, List<CommitRow> rows) {}

    public record CommitResult(
            int coursesCreated, int courseSlotsOpened, int lecturersListed, int alreadyListed,
            List<String> notes
    ) {}
}