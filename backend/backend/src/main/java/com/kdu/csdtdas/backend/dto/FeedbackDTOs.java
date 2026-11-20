package com.kdu.csdtdas.backend.dto;

import java.util.List;

public final class FeedbackDTOs {

    private FeedbackDTOs() {}

    public record WindowInfo(Long sessionId, String semester, boolean open) {}

    public record WindowRequest(Long sessionId, String semester, Boolean open) {}

    public record Item(Long courseId, String code, String title, Long lecturerId, String lecturerName, boolean rated) {}

    public record Mine(boolean open, String sessionName, String semester, List<Item> items) {}

    public record SubmitRequest(
            Long sessionId, String semester, Long courseId, Long lecturerId,
            Integer clarity, Integer preparation, Integer punctuality, Integer helpfulness, Integer fairness,
            String comment
    ) {}

    public record Result(
            Long courseId, String code, String title, Long lecturerId, String lecturerName,
            int responses, boolean visible, String note,
            Double clarity, Double preparation, Double punctuality, Double helpfulness, Double fairness, Double overall,
            List<String> comments
    ) {}

    public record Results(WindowInfo window, int minimum, List<Result> rows) {}

    public record Message(String message) {}
}