package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaterUsageLogRepository extends JpaRepository<WaterUsageLog, Long> {
    boolean existsByHouseholdIdAndReadingDate(Long householdId, LocalDate date);


    Optional<WaterUsageLog> findTopByHouseholdIdAndReadingDateLessThanOrderByReadingDateDesc(
            Long householdId, LocalDate date);

    List<WaterUsageLog> findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(
            Long householdId, LocalDate from, LocalDate to);
}