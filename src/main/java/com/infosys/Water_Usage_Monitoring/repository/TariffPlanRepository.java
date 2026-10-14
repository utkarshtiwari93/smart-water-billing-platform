package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TariffPlanRepository extends JpaRepository<TariffPlan, Long> {
    Optional<TariffPlan> findFirstByApartmentIdAndActiveTrueOrderByEffectiveFromDesc(Long apartmentId);
}