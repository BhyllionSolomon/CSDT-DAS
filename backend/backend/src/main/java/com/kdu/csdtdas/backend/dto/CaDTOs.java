package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class CaDTOs {
    private CaDTOs() {}
    public record CourseInfo(Long courseId, String code, String title, int rosterSize, double allocated) {}
    public record ItemRow(Long id, String category, String title, double max, boolean released, int marked,
                          String instructions, LocalDateTime dueAt, int submissionCount) {}
    public record StudentRow(Long studentId, String matricNumber, String fullName, Map<Long, Double> scores,
                             double total, double rawTotal) {}
    public record Sheet(double cap, double allocated, List<ItemRow> items, List<StudentRow> students) {}
    public record ItemRequest(Long courseId, Long sessionId, String semester, String category, String title,
                              BigDecimal maxMark, String instructions, LocalDateTime dueAt) {}
    public record ItemUpdate(String title, BigDecimal maxMark, Boolean released, String instructions, LocalDateTime dueAt) {}
    public record ScoreEntry(Long studentId, BigDecimal score) {}
    public record SubmissionRow(Long id, Long studentId, String matricNumber, String fullName, String fileName,
                                LocalDateTime submittedAt, Double score, String feedback, LocalDateTime gradedAt) {}
    public record SubmissionReceipt(Long id, String fileName, LocalDateTime submittedAt, String status,
                                     Double score, String feedback) {}
    public record GradeRequest(BigDecimal score, String feedback) {}
    public record MyItem(String category, String title, double max, Double score, String instructions,
                         LocalDateTime dueAt, String submissionStatus, String submissionFileName,
                         LocalDateTime submittedAt, String feedback) {}
    public record MyCourse(Long courseId, String code, String title, List<MyItem> items, double earned, double releasedMax) {}
    public record Mine(double cap, List<MyCourse> courses) {}
}