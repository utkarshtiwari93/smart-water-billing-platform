package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApartmentRepository extends JpaRepository<Apartment, Long> {}