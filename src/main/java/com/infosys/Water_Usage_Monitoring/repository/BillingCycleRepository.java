package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {
    List<BillingCycle> findByApartmentIdAndStatus(Long apartmentId, CycleStatus status);
}