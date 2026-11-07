package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.FeedbackReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FeedbackReceiptRepository extends JpaRepository<FeedbackReceipt, UUID> {

    boolean existsByStudentIdAndCourseIdAndAcademicSessionIdAndSemesterAndLecturerId(
            Long studentId, Long courseId, Long sessionId, String semester, Long lecturerId
    );
}