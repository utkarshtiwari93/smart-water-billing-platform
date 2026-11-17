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

    Optional<WaterUsageLog> findTopByHouseholdIdAndReadingDateBetweenOrderByReadingDateDesc(
            Long householdId, LocalDate from, LocalDate to);

    List<WaterUsageLog> findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(
            Long householdId, LocalDate from, LocalDate to);

    @Query("""
            select log from WaterUsageLog log
            where log.household.id = :householdId
              and (log.readingDate between :from and :to
                or log.readingDate = (
                    select max(previous.readingDate) from WaterUsageLog previous
                    where previous.household.id = :householdId and previous.readingDate < :from)
                or log.readingDate = (
                    select min(following.readingDate) from WaterUsageLog following
                    where following.household.id = :householdId and following.readingDate > :to))
            order by log.readingDate asc
            """)
    List<WaterUsageLog> findHouseholdReadingsWithBoundaries(@Param("householdId") Long householdId,
                                                            @Param("from") LocalDate from,
                                                            @Param("to") LocalDate to);

    @Query("""
            select log from WaterUsageLog log
            where log.household.apartment.id = :apartmentId
              and (log.readingDate between :from and :to
                or log.readingDate = (
                    select max(previous.readingDate) from WaterUsageLog previous
                    where previous.household.id = log.household.id and previous.readingDate < :from)
                or log.readingDate = (
                    select min(following.readingDate) from WaterUsageLog following
                    where following.household.id = log.household.id and following.readingDate > :to))
            order by log.household.id asc, log.readingDate asc
            """)
    List<WaterUsageLog> findApartmentReadingsWithBoundaries(@Param("apartmentId") Long apartmentId,
                                                            @Param("from") LocalDate from,
                                                            @Param("to") LocalDate to);

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

    @Query("""
            select log from WaterUsageLog log
            where log.alertChecked = false and log.household.apartment.id = :apartmentId
            order by log.readingDate asc, log.id asc
            """)
    List<WaterUsageLog> findTop500PendingForApartment(@Param("apartmentId") Long apartmentId,
                                                       org.springframework.data.domain.Pageable pageable);

    List<WaterUsageLog> findTop30ByHouseholdIdAndReadingDateLessThanAndConsumptionLitresGreaterThanOrderByReadingDateDesc(
            Long householdId, LocalDate date, BigDecimal minConsumption);
}