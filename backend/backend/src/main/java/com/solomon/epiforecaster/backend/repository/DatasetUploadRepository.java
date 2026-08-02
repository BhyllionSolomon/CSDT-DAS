package com.solomon.epiforecaster.backend.repository;

import com.solomon.epiforecaster.backend.entity.DatasetUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatasetUploadRepository extends JpaRepository<DatasetUpload, Long> {
}