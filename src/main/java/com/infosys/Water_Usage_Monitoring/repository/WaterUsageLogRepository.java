package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaterUsageLogRepository extends JpaRepository<WaterUsageLog, Long> {

    boolean existsByHouseholdIdAndReadingDate(Long householdId, LocalDate date);

    Optional<WaterUsageLog> findTopByHouseholdIdOrderByReadingDateDesc(Long householdId);

    Optional<WaterUsageLog> findTopByHouseholdIdAndReadingDateLessThanOrderByReadingDateDesc(
            Long householdId, LocalDate date);

    List<WaterUsageLog> findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(
            Long householdId, LocalDate from, LocalDate to);

    @Query("select sum(l.consumptionLitres) from WaterUsageLog l "
            + "where l.household.id = :householdId and l.readingDate between :from and :to")
    BigDecimal sumConsumption(@Param("householdId") Long householdId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);
}