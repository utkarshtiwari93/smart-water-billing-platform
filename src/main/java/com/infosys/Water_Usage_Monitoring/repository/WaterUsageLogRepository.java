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

    List<WaterUsageLog> findTop51ByHouseholdIdOrderByReadingDateDescIdDesc(Long householdId);

    Optional<WaterUsageLog> findTopByHouseholdIdAndReadingDateLessThanOrderByReadingDateDesc(
            Long householdId, LocalDate date);

    List<WaterUsageLog> findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(
            Long householdId, LocalDate from, LocalDate to);

    @Query("""
            select l.household.id as householdId,
                   l.household.flatNumber as flatNumber,
                   sum(l.consumptionLitres) as consumptionLitres
            from WaterUsageLog l
            where l.household.apartment.id = :apartmentId
              and l.readingDate between :from and :to
            group by l.household.id, l.household.flatNumber
            """)
    List<ApartmentHouseholdConsumption> summarizeApartmentHouseholdConsumption(
            @Param("apartmentId") Long apartmentId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    @Query("select sum(l.consumptionLitres) from WaterUsageLog l "
            + "where l.household.id = :householdId and l.readingDate between :from and :to")
    BigDecimal sumConsumption(@Param("householdId") Long householdId,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    List<WaterUsageLog> findTop500ByAlertCheckedFalseOrderByReadingDateAscIdAsc();

    List<WaterUsageLog> findTop30ByHouseholdIdAndReadingDateLessThanAndConsumptionLitresGreaterThanOrderByReadingDateDesc(
            Long householdId, LocalDate date, BigDecimal minConsumption);
}