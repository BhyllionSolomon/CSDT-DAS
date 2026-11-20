package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class LabDTOs {

    private LabDTOs() {}

    public record CourseHit(Long id, String code, String title) {}

    public record ManualRow(
            Long id, String title, Long courseId, String courseCode, String courseTitle,
            BigDecimal unitPrice, boolean active, int purchased, int sold, int stock, BigDecimal averageCost
    ) {}

    public record ManualRequest(Long id, String title, Long courseId, BigDecimal unitPrice, Boolean active) {}

    public record PurchaseRequest(Integer quantity, BigDecimal unitCost, String date, String note) {}

    public record StudentHit(Long id, String matricNumber, String fullName, String programme, String level) {}

    public record SaleRequest(Long manualId, Long studentId) {}

    public record SaleRow(
            Long id, Long manualId, String manualTitle, String matricNumber, String studentName,
            BigDecimal unitPrice, int quantity, LocalDateTime soldAt, String recordedBy, boolean voided, String sessionName
    ) {}

    public record ManualFinance(
            Long manualId, String title, int purchased, int sold, int stock,
            BigDecimal revenue, BigDecimal spent, BigDecimal grossProfit, BigDecimal stockValue
    ) {}

    public record MonthPoint(String month, BigDecimal revenue, BigDecimal spent) {}

    public record Finance(
            BigDecimal revenue, BigDecimal spent, BigDecimal net, BigDecimal grossProfit, BigDecimal stockValue,
            int copiesSold, int copiesInStock, List<ManualFinance> manuals, List<MonthPoint> months
    ) {}

    public record Group(int students, Double average, Double passRate) {}

    public record Impact(
            String manualTitle, String courseCode, String sessionName,
            Group buyers, Group nonBuyers, Double averageDifference, boolean enoughData, String note
    ) {}

    public record Message(String message) {}
}