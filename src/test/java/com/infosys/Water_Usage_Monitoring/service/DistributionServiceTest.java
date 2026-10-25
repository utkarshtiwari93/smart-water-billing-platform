package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.DistributionResponse;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdShareResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.BulkPurchase;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.repository.BulkPurchaseRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributionServiceTest {

    @Mock BillingCycleService cycleService;
    @Mock BulkPurchaseRepository purchaseRepo;
    @Mock HouseholdRepository householdRepo;
    @Mock WaterUsageLogRepository logRepo;
    @Spy TariffCalculator calculator = new TariffCalculator();
    @Spy ChargeDistributor distributor = new ChargeDistributor();
    @InjectMocks DistributionService service;

    private final LocalDate start = LocalDate.of(2026, 10, 1);
    private final LocalDate end = LocalDate.of(2026, 10, 31);
    private BillingCycle cycle;

    @BeforeEach
    void setUp() {
        Apartment apartment = new Apartment();
        apartment.setId(1L);

        TariffPlan plan = new TariffPlan();
        plan.setTier1LimitKl(new BigDecimal("10"));
        plan.setBaseRate(new BigDecimal("10"));
        plan.setHigherRate(new BigDecimal("20"));

        cycle = new BillingCycle();
        cycle.setId(1L);
        cycle.setApartment(apartment);
        cycle.setTariffPlan(plan);
        cycle.setStartDate(start);
        cycle.setEndDate(end);
        cycle.setStatus(CycleStatus.OPEN);
    }

    private Household household(long id, String flat, String meter, String area) {
        Household h = new Household();
        h.setId(id);
        h.setFlatNumber(flat);
        h.setMeterNumber(meter);
        h.setAreaSqft(new BigDecimal(area));
        return h;
    }

    private BulkPurchase purchase(String total) {
        BulkPurchase p = new BulkPurchase();
        p.setTotalCost(new BigDecimal(total));
        return p;
    }

    private HouseholdShareResponse row(DistributionResponse r, String flat) {
        return r.households().stream().filter(h -> h.flatNumber().equals(flat)).findFirst().orElseThrow();
    }

    private void stubTwoMeteredHouseholds() {
        when(cycleService.find(1L)).thenReturn(cycle);
        when(householdRepo.findByApartmentId(1L)).thenReturn(List.of(
                household(2L, "B-202", "M2", "950"), household(1L, "A-101", "M1", "1200")));
        when(logRepo.sumConsumption(1L, start, end)).thenReturn(new BigDecimal("14000"));   // water 180.00
        when(logRepo.sumConsumption(2L, start, end)).thenReturn(new BigDecimal("6000"));    // water 60.00
    }

    @Test
    void sharedCost_isPoolMinusWaterCharges_splitByConsumption() {
        stubTwoMeteredHouseholds();
        when(purchaseRepo.findByBillingCycleIdOrderByPurchaseDateAscIdAsc(1L))
                .thenReturn(List.of(purchase("1000.00"), purchase("800.00")));       // pool 1800

        DistributionResponse r = service.calculate(1L);

        assertEquals(0, r.totalWaterCharges().compareTo(new BigDecimal("240.00")));
        assertEquals(0, r.sharedCost().compareTo(new BigDecimal("1560.00")));
        assertEquals(0, row(r, "A-101").sharedAllocation().compareTo(new BigDecimal("1092.00")));
        assertEquals(0, row(r, "B-202").sharedAllocation().compareTo(new BigDecimal("468.00")));
        assertEquals(0, row(r, "A-101").totalCharge().compareTo(new BigDecimal("1272.00")));
        assertEquals(0, r.totalBilled().compareTo(new BigDecimal("1800.00")));       // pool poora recover
        assertEquals("A-101", r.households().get(0).flatNumber());                   // flat ke hisaab se sorted
    }

    @Test
    void waterChargesAbovePool_meansNoSharedCostAndASurplus() {
        stubTwoMeteredHouseholds();
        when(purchaseRepo.findByBillingCycleIdOrderByPurchaseDateAscIdAsc(1L))
                .thenReturn(List.of(purchase("100.00")));

        DistributionResponse r = service.calculate(1L);

        assertEquals(0, r.sharedCost().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.surplus().compareTo(new BigDecimal("140.00")));
        assertNotNull(r.note());
        assertEquals(0, row(r, "A-101").totalCharge().compareTo(new BigDecimal("180.00")));
    }

    @Test
    void noPurchases_meansNothingToDistribute() {
        stubTwoMeteredHouseholds();
        when(purchaseRepo.findByBillingCycleIdOrderByPurchaseDateAscIdAsc(1L)).thenReturn(List.of());

        DistributionResponse r = service.calculate(1L);

        assertEquals(0, r.sharedCost().compareTo(BigDecimal.ZERO));
        assertEquals(0, r.surplus().compareTo(BigDecimal.ZERO));
        assertNotNull(r.note());
    }

    @Test
    void householdWithoutMeter_getsAreaEstimatedShare_andNoWaterCharge() {
        when(cycleService.find(1L)).thenReturn(cycle);
        when(householdRepo.findByApartmentId(1L)).thenReturn(List.of(
                household(1L, "A-101", "M1", "1200"),
                household(3L, "C-303", null, "500")));
        when(logRepo.sumConsumption(1L, start, end)).thenReturn(new BigDecimal("14000"));
        when(purchaseRepo.findByBillingCycleIdOrderByPurchaseDateAscIdAsc(1L))
                .thenReturn(List.of(purchase("1000.00")));

        DistributionResponse r = service.calculate(1L);

        HouseholdShareResponse unmetered = row(r, "C-303");
        assertEquals(false, unmetered.hasMeter());
        assertEquals(0, unmetered.waterCharge().compareTo(BigDecimal.ZERO));
        assertEquals("AREA_ESTIMATE", unmetered.allocationBasis());
        assertTrue(unmetered.sharedAllocation().signum() > 0);

        BigDecimal distributed = r.households().stream()
                .map(HouseholdShareResponse::sharedAllocation).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, distributed.compareTo(r.sharedCost()));                      // jod exact
    }
}