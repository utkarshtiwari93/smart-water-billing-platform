package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.DistributionResponse;
import com.infosys.Water_Usage_Monitoring.dto.FinalizeResponse;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdShareResponse;
import com.infosys.Water_Usage_Monitoring.dto.InvoiceResponse;
import com.infosys.Water_Usage_Monitoring.entity.Adjustment;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.Invoice;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.enums.InvoiceItemType;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.repository.AdjustmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.InvoiceRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock BillingCycleService cycleService;
    @Mock DistributionService distributionService;
    @Spy TariffCalculator calculator = new TariffCalculator();
    @Mock HouseholdRepository householdRepo;
    @Mock AdjustmentRepository adjustmentRepo;
    @Mock InvoiceRepository invoiceRepo;
    @Mock UserRepository userRepo;
    @InjectMocks InvoiceService service;

    private Apartment apartment;
    private TariffPlan plan;

    @BeforeEach
    void setUp() {
        apartment = new Apartment();
        apartment.setId(1L);

        plan = new TariffPlan();
        plan.setId(1L);
        plan.setTier1LimitKl(new BigDecimal("10"));
        plan.setBaseRate(new BigDecimal("10"));
        plan.setHigherRate(new BigDecimal("20"));
    }

    private BillingCycle cycle(CycleStatus status) {
        BillingCycle c = new BillingCycle();
        c.setId(1L);
        c.setApartment(apartment);
        c.setTariffPlan(plan);
        c.setStartDate(LocalDate.of(2026, 10, 1));
        c.setEndDate(LocalDate.of(2026, 10, 31));
        c.setStatus(status);
        return c;
    }

    private Household household(long id, String flat) {
        Household h = new Household();
        h.setId(id);
        h.setFlatNumber(flat);
        h.setApartment(apartment);
        return h;
    }

    private Adjustment adjustment(Household h, String amount, String reason) {
        Adjustment a = new Adjustment();
        a.setHousehold(h);
        a.setAmount(new BigDecimal(amount));
        a.setReason(reason);
        return a;
    }

    private HouseholdShareResponse share(long id, String flat, String litres, String water, String shared, String total) {
        return new HouseholdShareResponse(id, flat, true, new BigDecimal(litres), new BigDecimal(water),
                "CONSUMPTION", new BigDecimal(shared), new BigDecimal(total));
    }

    private DistributionResponse distribution() {
        return new DistributionResponse(1L, new BigDecimal("1800.00"), new BigDecimal("240.00"),
                new BigDecimal("1560.00"), BigDecimal.ZERO, new BigDecimal("1800.00"), null,
                List.of(share(1L, "A-101", "14000", "180.00", "1092.00", "1272.00"),
                        share(2L, "B-202", "6000", "60.00", "468.00", "528.00")));
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private Invoice invoice(Household h) {
        Invoice inv = new Invoice();
        inv.setId(10L);
        inv.setInvoiceNumber("INV-C1-H" + h.getId());
        inv.setBillingCycle(cycle(CycleStatus.FINALIZED));
        inv.setHousehold(h);
        inv.setConsumptionLitres(new BigDecimal("14000.00"));
        inv.setTotalAmount(new BigDecimal("1272.00"));
        inv.setIssuedAt(LocalDateTime.now());
        return inv;
    }

    private User user(Role role, Household h) {
        User u = new User();
        u.setRole(role);
        u.setHousehold(h);
        return u;
    }

    @Test
    void finalize_createsItemizedInvoicesAndFreezesCycle() {
        BillingCycle c = cycle(CycleStatus.OPEN);
        Household a = household(1L, "A-101");
        Household b = household(2L, "B-202");
        when(cycleService.find(1L)).thenReturn(c);
        when(distributionService.calculate(1L)).thenReturn(distribution());
        when(adjustmentRepo.findByBillingCycleIdOrderByIdAsc(1L))
                .thenReturn(List.of(adjustment(a, "50.00", "Late fee")));
        when(householdRepo.findById(1L)).thenReturn(Optional.of(a));
        when(householdRepo.findById(2L)).thenReturn(Optional.of(b));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));

        FinalizeResponse r = service.finalizeCycle(1L);

        assertEquals("FINALIZED", r.status());
        assertEquals(CycleStatus.FINALIZED, c.getStatus());
        assertEquals(2, r.invoicesCreated());
        assertMoney("1850.00", r.totalInvoiced());          // pool 1800 + adjustment 50

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepo, times(2)).save(captor.capture());

        Invoice invA = captor.getAllValues().get(0);
        assertEquals("INV-C1-H1", invA.getInvoiceNumber());
        assertEquals(4, invA.getItems().size());
        assertEquals(InvoiceItemType.WATER_TIER1, invA.getItems().get(0).getItemType());
        assertMoney("100.00", invA.getItems().get(0).getAmount());
        assertEquals(InvoiceItemType.WATER_TIER2, invA.getItems().get(1).getItemType());
        assertMoney("80.00", invA.getItems().get(1).getAmount());
        assertEquals(InvoiceItemType.SHARED_AREA, invA.getItems().get(2).getItemType());
        assertEquals(InvoiceItemType.ADJUSTMENT, invA.getItems().get(3).getItemType());
        assertMoney("1322.00", invA.getTotalAmount());

        Invoice invB = captor.getAllValues().get(1);
        assertEquals(2, invB.getItems().size());              // adjustment nahi, tier 2 nahi
        assertMoney("528.00", invB.getTotalAmount());
    }

    @Test
    void finalize_nonOpenCycle_isRejected() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.FINALIZED));

        assertThrows(ConflictException.class, () -> service.finalizeCycle(1L));
        verify(distributionService, never()).calculate(anyLong());
        verify(invoiceRepo, never()).save(any(Invoice.class));
    }

    @Test
    void finalize_withNoHouseholds_isRejected() {
        BillingCycle c = cycle(CycleStatus.OPEN);
        when(cycleService.find(1L)).thenReturn(c);
        when(distributionService.calculate(1L)).thenReturn(new DistributionResponse(1L, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, new ArrayList<>()));

        assertThrows(BadRequestException.class, () -> service.finalizeCycle(1L));
        assertEquals(CycleStatus.OPEN, c.getStatus());
    }

    @Test
    void archive_finalizedCycle_becomesArchived() {
        BillingCycle c = cycle(CycleStatus.FINALIZED);
        when(cycleService.find(1L)).thenReturn(c);

        FinalizeResponse r = service.archive(1L);

        assertEquals("ARCHIVED", r.status());
        assertEquals(CycleStatus.ARCHIVED, c.getStatus());
    }

    @Test
    void archive_openCycle_isRejected() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.OPEN));

        assertThrows(ConflictException.class, () -> service.archive(1L));
    }

    @Test
    void resident_canViewOwnInvoice() {
        Household own = household(1L, "A-101");
        when(invoiceRepo.findById(10L)).thenReturn(Optional.of(invoice(own)));
        when(userRepo.findByEmail("r@test.com")).thenReturn(Optional.of(user(Role.RESIDENT, own)));

        InvoiceResponse r = service.get(10L, "r@test.com");

        assertEquals("INV-C1-H1", r.invoiceNumber());
        assertMoney("1272.00", r.totalAmount());
    }

    @Test
    void resident_cannotViewAnotherHouseholdsInvoice() {
        when(invoiceRepo.findById(10L)).thenReturn(Optional.of(invoice(household(1L, "A-101"))));
        when(userRepo.findByEmail("r@test.com"))
                .thenReturn(Optional.of(user(Role.RESIDENT, household(2L, "B-202"))));

        assertThrows(AccessDeniedException.class, () -> service.get(10L, "r@test.com"));
    }

    @Test
    void admin_canViewAnyInvoice() {
        when(invoiceRepo.findById(10L)).thenReturn(Optional.of(invoice(household(1L, "A-101"))));
        when(userRepo.findByEmail("a@test.com")).thenReturn(Optional.of(user(Role.ADMIN, null)));

        assertEquals(10L, service.get(10L, "a@test.com").id());
    }

    @Test
    void myInvoices_withoutHousehold_isRejected() {
        when(userRepo.findByEmail("r@test.com")).thenReturn(Optional.of(user(Role.RESIDENT, null)));

        assertThrows(BadRequestException.class, () -> service.myInvoices("r@test.com"));
    }
}