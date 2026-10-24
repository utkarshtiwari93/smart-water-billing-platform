package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.BulkPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BulkPurchaseRepository extends JpaRepository<BulkPurchase, Long> {

    List<BulkPurchase> findByBillingCycleIdOrderByPurchaseDateAscIdAsc(Long billingCycleId);
}