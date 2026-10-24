package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface BillingCycleRepository extends JpaRepository<BillingCycle, Long> {

    List<BillingCycle> findByApartmentIdAndStatus(Long apartmentId, CycleStatus status);

    List<BillingCycle> findByApartmentIdOrderByStartDateDesc(Long apartmentId);

    List<BillingCycle> findByApartmentIdAndStatusOrderByStartDateDesc(Long apartmentId, CycleStatus status);

    boolean existsByTariffPlanId(Long tariffPlanId);

    boolean existsByTariffPlanIdAndStatusIn(Long tariffPlanId, Collection<CycleStatus> statuses);

    boolean existsByApartmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long apartmentId, LocalDate newEnd, LocalDate newStart);

    boolean existsByApartmentIdAndIdNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long apartmentId, Long excludeId, LocalDate newEnd, LocalDate newStart);
}