package com.kdu.csdtdas.backend.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public final class LabManualWorkflowDTO {
 private LabManualWorkflowDTO(){}
 public record Row(Long id,String title,String courseCode,String courseTitle,Integer pageCount,BigDecimal suggestedLevy,BigDecimal approvedLevy,String status,String targetLab,String uploadedBy,String hodReviewedBy,String hodComment,String receivedBy,LocalDateTime submittedAt,LocalDateTime approvedAt,LocalDateTime receivedAt){}
 public record Review(BigDecimal approvedLevy,String comment){}
 public record Dispatch(String targetLab){}
 public record Message(String message){}
 public record Buyer(Long orderId,String matricNumber,String studentName,String programme,String level,BigDecimal amount,String status,LocalDateTime paidAt){}
}