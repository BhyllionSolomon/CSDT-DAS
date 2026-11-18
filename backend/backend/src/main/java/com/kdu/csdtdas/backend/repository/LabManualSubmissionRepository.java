package com.kdu.csdtdas.backend.repository;
import com.kdu.csdtdas.backend.entity.LabManualSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface LabManualSubmissionRepository extends JpaRepository<LabManualSubmission,Long>{
 List<LabManualSubmission> findAllByUploadedByOrderBySubmittedAtDesc(String uploadedBy);
 List<LabManualSubmission> findAllByStatusOrderBySubmittedAtAsc(String status);
 List<LabManualSubmission> findAllByTargetLabAndStatusOrderBySubmittedAtAsc(String targetLab,String status);
}