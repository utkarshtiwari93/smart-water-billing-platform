package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TariffPlanRepository extends JpaRepository<TariffPlan, Long> {

    List<TariffPlan> findByApartmentIdOrderByEffectiveFromDesc(Long apartmentId);

    Optional<TariffPlan> findFirstByApartmentIdAndActiveTrueOrderByEffectiveFromDesc(Long apartmentId);

    Optional<TariffPlan> findFirstByApartmentIdAndActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            Long apartmentId, LocalDate date);
}