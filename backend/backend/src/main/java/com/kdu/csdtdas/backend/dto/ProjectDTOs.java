package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ProjectDTOs {

    private ProjectDTOs() {}

    public record StaffRow(
            Long userId, String username, String fullName, String role, String designation, String displayTitle, int displayOrder,
            boolean eligible, boolean firstClassEligible, boolean coordinator, boolean assistantCoordinator, int currentLoad
    ) {}

    public record StaffUpdate(
            Long userId, String designation, String displayTitle, Integer displayOrder, Boolean eligible, Boolean firstClassEligible, Boolean coordinator, Boolean assistantCoordinator
    ) {}

    public record StudentLine(
            Long studentId, String matricNumber, String fullName, String programme,
            String classification, BigDecimal cgpa
    ) {}

    public record SupervisorGroup(
            Long supervisorId, String fullName, String designation,
            boolean coordinator, boolean assistantCoordinator,
            int existing, int proposed, int firstClass, int upperSecond, int others,
            List<StudentLine> students
    ) {}

    public record Proposal(
            Long sessionId, String sessionName, int eligibleStudents, int supervisors,
            int firstClass, int upperSecond, int others,
            List<SupervisorGroup> groups, List<String> notes
    ) {}

    public record Pair(Long studentId, Long supervisorId) {}

    public record ConfirmRequest(Long sessionId, List<Pair> assignments) {}

    public record Message(String message) {}

    public record AssignmentRow(
            Long id, Long studentId, String matricNumber, String fullName, String programme,
            String classification, BigDecimal cgpa,
            Long supervisorId, String supervisorName, String designation,
            String status, String sessionName, LocalDateTime assignedAt
    ) {}

    public record MyAssignment(
            boolean assigned, String supervisorName, String designation, String status,
            String sessionName, LocalDateTime assignedAt, String supervisorEmail, String supervisorPhone
    ) {}

    public record MyStudent(
            Long studentId, String matricNumber, String fullName, String programme,
            String sessionName, LocalDateTime assignedAt
    ) {}

    public record DraftItemRow(
            Long itemId, Long studentId, String matricNumber, String fullName, String programme,
            String classification, BigDecimal cgpa, Long supervisorId, String supervisorName
    ) {}

    public record AllocationEventRow(
            String action, String matricNumber, String previousSupervisor, String newSupervisor,
            String changedBy, LocalDateTime changedAt, String note
    ) {}

    public record DraftView(
            Long draftId, Long sessionId, String sessionName, String status,
            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime publishedAt,
            List<DraftItemRow> items, List<AllocationEventRow> history
    ) {}

    public record MoveRequest(Long supervisorId, String note) {}

    /** Deliberately excludes CGPA and degree classification. */
    public record PublicScheduleRow(
            String sessionName, String matricNumber, String fullName, String programme,
            String supervisorName, String designation, String displayTitle, int displayOrder
    ) {}

    public record AllocationExportRow(String supervisor, String student, String matric, String department) {}
    public record AllocationExportPreview(String session, int count, List<AllocationExportRow> rows) {}
}