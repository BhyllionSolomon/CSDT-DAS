package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.CaScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CaScoreRepository extends JpaRepository<CaScore, Long> {

    List<CaScore> findByItemId(Long itemId);

    void deleteByItemId(Long itemId);

    @Query("""
        SELECT s FROM CaScore s
        JOIN FETCH s.item i
        JOIN FETCH s.student
        WHERE i.course.id = :courseId AND i.academicSession.id = :sessionId AND i.semester = :semester
    """)
    List<CaScore> findForCourse(@Param("courseId") Long courseId, @Param("sessionId") Long sessionId, @Param("semester") String semester);

    @Query("""
        SELECT s FROM CaScore s
        JOIN FETCH s.item i
        WHERE s.student.id = :studentId AND i.academicSession.id = :sessionId AND i.semester = :semester
    """)
    List<CaScore> findForStudent(@Param("studentId") Long studentId, @Param("sessionId") Long sessionId, @Param("semester") String semester);
}