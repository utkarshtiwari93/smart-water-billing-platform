package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByBillingCycleIdOrderByHouseholdFlatNumberAsc(Long billingCycleId);

    List<Invoice> findByHouseholdIdOrderByIssuedAtDesc(Long householdId);

    List<Invoice> findTop13ByHouseholdIdOrderByIssuedAtDescIdDesc(Long householdId);

    @Query("""
            select count(i) as invoiceCount, coalesce(sum(i.totalAmount), 0) as invoiceTotal
            from Invoice i
            where i.billingCycle.id = :cycleId
            """)
    InvoiceCycleTotals summarizeByBillingCycleId(@Param("cycleId") Long cycleId);
}