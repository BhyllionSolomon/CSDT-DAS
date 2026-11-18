package com.kdu.csdtdas.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class LabStudentDTOs {
    private LabStudentDTOs() {}
    public record ManualOption(Long id, String title, String courseCode, String courseTitle, BigDecimal price, String status, Long orderId) {}
    public record OrderRequest(Long manualId) {}
    public record OrderRow(Long id, Long manualId, String manualTitle, String courseCode, BigDecimal amount, String status, String sessionName, LocalDateTime createdAt) {}
    public record Message(String message) {}
    public record PaymentStart(Long orderId) {}
    public record PaymentInit(Long orderId, String reference, String authorizationUrl) {}
    public record Receipt(Long orderId, String receiptNumber, String matricNumber, String studentName,
            String courseCode, String manualTitle, BigDecimal amount, String currency, String paymentReference,
            String paymentChannel, LocalDateTime paidAt, String status, LocalDateTime issuedAt, String issuedBy) {}
}