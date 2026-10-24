package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.BulkPurchaseRequest;
import com.infosys.Water_Usage_Monitoring.dto.BulkPurchaseResponse;
import com.infosys.Water_Usage_Monitoring.dto.PurchaseSummaryResponse;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.BulkPurchase;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.enums.PurchaseSource;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.repository.BulkPurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkPurchaseServiceTest {

    @Mock BulkPurchaseRepository repo;
    @Mock BillingCycleService cycleService;
    @InjectMocks BulkPurchaseService service;

    private BillingCycle cycle(CycleStatus status) {
        BillingCycle c = new BillingCycle();
        c.setId(1L);
        c.setStartDate(LocalDate.of(2026, 10, 1));
        c.setEndDate(LocalDate.of(2026, 10, 31));
        c.setStatus(status);
        return c;
    }

    private BulkPurchaseRequest req(String date, String volume, String unitCost) {
        return new BulkPurchaseRequest(LocalDate.parse(date), PurchaseSource.TANKER, "ABC Tankers",
                new BigDecimal(volume), new BigDecimal(unitCost), null);
    }

    private BulkPurchase purchase(long id, BillingCycle c, String volume, String unitCost, String total) {
        BulkPurchase p = new BulkPurchase();
        p.setId(id);
        p.setBillingCycle(c);
        p.setPurchaseDate(LocalDate.of(2026, 10, 5));
        p.setSource(PurchaseSource.TANKER);
        p.setVolumeKl(new BigDecimal(volume));
        p.setUnitCost(new BigDecimal(unitCost));
        p.setTotalCost(new BigDecimal(total));
        return p;
    }

    @Test
    void create_computesTotalCostFromVolumeAndUnitCost() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.OPEN));
        when(repo.save(any(BulkPurchase.class))).thenAnswer(i -> i.getArgument(0));

        BulkPurchaseResponse r = service.create(1L, req("2026-10-10", "10.50", "12.50"));

        assertEquals(0, r.totalCost().compareTo(new BigDecimal("131.25")));
        assertEquals("TANKER", r.source());
    }

    @Test
    void create_dateOutsideCycle_isRejected() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.OPEN));

        assertThrows(BadRequestException.class, () -> service.create(1L, req("2026-11-05", "10", "10")));
        verify(repo, never()).save(any(BulkPurchase.class));
    }

    @Test
    void create_inFinalizedCycle_isRejected() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.FINALIZED));

        assertThrows(ConflictException.class, () -> service.create(1L, req("2026-10-10", "10", "10")));
        verify(repo, never()).save(any(BulkPurchase.class));
    }

    @Test
    void update_inFinalizedCycle_isRejected() {
        BillingCycle c = cycle(CycleStatus.FINALIZED);
        when(repo.findById(5L)).thenReturn(Optional.of(purchase(5L, c, "10", "10", "100.00")));

        assertThrows(ConflictException.class, () -> service.update(5L, req("2026-10-10", "20", "10")));
    }

    @Test
    void delete_inOpenCycle_succeeds() {
        BulkPurchase p = purchase(5L, cycle(CycleStatus.OPEN), "10", "10", "100.00");
        when(repo.findById(5L)).thenReturn(Optional.of(p));

        service.delete(5L);

        verify(repo).delete(p);
    }

    @Test
    void summary_usesWeightedAverageUnitCost() {
        BillingCycle c = cycle(CycleStatus.OPEN);
        when(cycleService.find(1L)).thenReturn(c);
        when(repo.findByBillingCycleIdOrderByPurchaseDateAscIdAsc(1L)).thenReturn(List.of(
                purchase(1L, c, "100.00", "10.00", "1000.00"),
                purchase(2L, c, "50.00", "16.00", "800.00")));

        PurchaseSummaryResponse s = service.summary(1L);

        assertEquals(2, s.purchaseCount());
        assertEquals(0, s.totalVolumeKl().compareTo(new BigDecimal("150.00")));
        assertEquals(0, s.totalCost().compareTo(new BigDecimal("1800.00")));
        assertEquals(0, s.averageUnitCost().compareTo(new BigDecimal("12.00")));
    }

    @Test
    void summary_withNoPurchases_returnsZeros() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.OPEN));
        when(repo.findByBillingCycleIdOrderByPurchaseDateAscIdAsc(1L)).thenReturn(List.of());

        PurchaseSummaryResponse s = service.summary(1L);

        assertEquals(0, s.purchaseCount());
        assertEquals(0, s.averageUnitCost().compareTo(BigDecimal.ZERO));
    }
}