package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.Household;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HouseholdRepository extends JpaRepository<Household, Long> {
    List<Household> findByApartmentId(Long apartmentId);
    boolean existsByApartmentIdAndFlatNumber(Long apartmentId, String flatNumber);
    Optional<Household> findByMeterNumber(String meterNumber);
}