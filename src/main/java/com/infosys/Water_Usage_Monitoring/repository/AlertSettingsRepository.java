package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.AlertSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertSettingsRepository extends JpaRepository<AlertSettings, Long> {

    Optional<AlertSettings> findByApartmentId(Long apartmentId);
}