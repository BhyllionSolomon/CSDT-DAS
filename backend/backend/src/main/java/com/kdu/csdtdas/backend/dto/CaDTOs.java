package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class CaDTOs {

    private CaDTOs() {}

    public record CourseInfo(Long courseId, String code, String title, int rosterSize, double allocated) {}

    public record ItemRow(Long id, String category, String title, double max, boolean released, int marked) {}

    public record StudentRow(Long studentId, String matricNumber, String fullName, Map<Long, Double> scores, double total) {}

    public record Sheet(double cap, double allocated, List<ItemRow> items, List<StudentRow> students) {}

    public record ItemRequest(Long courseId, Long sessionId, String semester, String category, String title, BigDecimal maxMark) {}

    public record ItemUpdate(String title, BigDecimal maxMark, Boolean released) {}

    public record ScoreEntry(Long studentId, BigDecimal score) {}

    public record MyItem(String category, String title, double max, Double score) {}

    public record MyCourse(Long courseId, String code, String title, List<MyItem> items, double earned, double releasedMax) {}

    public record Mine(double cap, List<MyCourse> courses) {}
}