package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByBillingCycleIdOrderByHouseholdFlatNumberAsc(Long billingCycleId);

    List<Invoice> findByHouseholdIdOrderByIssuedAtDesc(Long householdId);
}