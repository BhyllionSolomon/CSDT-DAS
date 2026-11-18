package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.LabAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LabAssetRepository extends JpaRepository<LabAsset, Long> {
    boolean existsByAssetTagIgnoreCase(String assetTag);
    boolean existsByAssetTagIgnoreCaseAndIdNot(String assetTag, Long id);
    List<LabAsset> findAllByOrderByLaboratoryAscCategoryAscNameAsc();
}