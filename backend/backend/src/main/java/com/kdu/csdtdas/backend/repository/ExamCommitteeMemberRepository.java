package com.kdu.csdtdas.backend.repository;
import com.kdu.csdtdas.backend.entity.ExamCommitteeMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExamCommitteeMemberRepository extends JpaRepository<ExamCommitteeMember, Long> {
    boolean existsByCycleIdAndUserId(Long cycleId, Long userId);
    List<ExamCommitteeMember> findByCycleId(Long cycleId);
    long deleteByCycleIdAndUserId(Long cycleId, Long userId);
}