package com.kdu.csdtdas.backend.repository;

import com.kdu.csdtdas.backend.entity.LabManualPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LabManualPurchaseRepository extends JpaRepository<LabManualPurchase, Long> {

    @Query("SELECT p FROM LabManualPurchase p JOIN FETCH p.manual ORDER BY p.purchasedOn DESC, p.id DESC")
    List<LabManualPurchase> findAllWithManual();
}