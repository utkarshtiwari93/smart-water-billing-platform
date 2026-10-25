package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.Adjustment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdjustmentRepository extends JpaRepository<Adjustment, Long> {

    List<Adjustment> findByBillingCycleIdOrderByIdAsc(Long billingCycleId);
}