package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.DistributionResponse;
import com.infosys.Water_Usage_Monitoring.dto.FinalizeResponse;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdShareResponse;
import com.infosys.Water_Usage_Monitoring.dto.InvoiceResponse;
import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
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
import com.infosys.Water_Usage_Monitoring.enums.ReadingSource;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.repository.AdjustmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.InvoiceRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
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
    @Mock WaterUsageLogRepository usageRepo;
    @Mock AlertSettingsService alertSettingsService;
    @Mock AlertMailer mailer;
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

    private WaterUsageLog reading(Household household, String date, String meter) {
        WaterUsageLog reading = new WaterUsageLog();
        reading.setHousehold(household);
        reading.setReadingDate(LocalDate.parse(date));
        reading.setMeterReading(new BigDecimal(meter));
        reading.setConsumptionLitres(BigDecimal.ZERO);
        reading.setSource(ReadingSource.MANUAL);
        return reading;
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
    void finalize_sendsBillEmailToResidentsWhenApartmentEmailIsEnabled() {
        BillingCycle c = cycle(CycleStatus.OPEN);
        Household a = household(1L, "A-101");
        Household b = household(2L, "B-202");
        User residentA = user(Role.RESIDENT, a);
        residentA.setEmail("resident-a@test.com");
        User residentB = user(Role.RESIDENT, b);
        residentB.setEmail("resident-b@test.com");
        com.infosys.Water_Usage_Monitoring.entity.AlertSettings settings =
                new com.infosys.Water_Usage_Monitoring.entity.AlertSettings();
        settings.setEmailEnabled(true);
        when(cycleService.find(1L)).thenReturn(c);
        when(distributionService.calculate(1L)).thenReturn(distribution());
        when(adjustmentRepo.findByBillingCycleIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(householdRepo.findById(1L)).thenReturn(Optional.of(a));
        when(householdRepo.findById(2L)).thenReturn(Optional.of(b));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
        when(alertSettingsService.getOrCreate(1L)).thenReturn(settings);
        when(userRepo.findByHouseholdId(1L)).thenReturn(List.of(residentA));
        when(userRepo.findByHouseholdId(2L)).thenReturn(List.of(residentB));

        service.finalizeCycle(1L);

        verify(mailer).sendHtml(org.mockito.ArgumentMatchers.eq("resident-a@test.com"),
                org.mockito.ArgumentMatchers.contains("INV-C1-H1"), org.mockito.ArgumentMatchers.contains("INR"));
        verify(mailer).sendHtml(org.mockito.ArgumentMatchers.eq("resident-a@test.com"),
                org.mockito.ArgumentMatchers.contains("Water-saving tips"),
                org.mockito.ArgumentMatchers.contains("full loads"));
        verify(mailer).sendHtml(org.mockito.ArgumentMatchers.eq("resident-b@test.com"),
                org.mockito.ArgumentMatchers.contains("INV-C1-H2"), org.mockito.ArgumentMatchers.contains("INR"));
        verify(mailer).sendHtml(org.mockito.ArgumentMatchers.eq("resident-b@test.com"),
                org.mockito.ArgumentMatchers.contains("Water-saving tips"), org.mockito.ArgumentMatchers.contains("full loads"));
    }

    @Test
    void finalize_smtpFailureDoesNotPreventInvoiceCreationOrCycleFinalization() {
        BillingCycle c = cycle(CycleStatus.OPEN);
        Household a = household(1L, "A-101");
        Household b = household(2L, "B-202");
        User resident = user(Role.RESIDENT, a);
        resident.setEmail("resident-a@test.com");
        com.infosys.Water_Usage_Monitoring.entity.AlertSettings settings =
                new com.infosys.Water_Usage_Monitoring.entity.AlertSettings();
        settings.setEmailEnabled(true);
        when(cycleService.find(1L)).thenReturn(c);
        when(distributionService.calculate(1L)).thenReturn(distribution());
        when(adjustmentRepo.findByBillingCycleIdOrderByIdAsc(1L)).thenReturn(List.of());
        when(householdRepo.findById(1L)).thenReturn(Optional.of(a));
        when(householdRepo.findById(2L)).thenReturn(Optional.of(b));
        when(invoiceRepo.save(any(Invoice.class))).thenAnswer(i -> i.getArgument(0));
        when(alertSettingsService.getOrCreate(1L)).thenReturn(settings);
        when(userRepo.findByHouseholdId(1L)).thenReturn(List.of(resident));
        when(userRepo.findByHouseholdId(2L)).thenReturn(List.of());
        when(mailer.sendHtml(any(), any(), any())).thenReturn(false);

        FinalizeResponse response = service.finalizeCycle(1L);

        assertEquals("FINALIZED", response.status());
        assertEquals(CycleStatus.FINALIZED, c.getStatus());
        verify(invoiceRepo, times(2)).save(any(Invoice.class));
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
    void resident_cannotDownloadAnotherHouseholdsInvoicePdfData() {
        when(invoiceRepo.findById(10L)).thenReturn(Optional.of(invoice(household(1L, "A-101"))));
        when(userRepo.findByEmail("r@test.com"))
                .thenReturn(Optional.of(user(Role.RESIDENT, household(2L, "B-202"))));

        assertThrows(AccessDeniedException.class, () -> service.getPdfData(10L, "r@test.com"));
        verify(usageRepo, never()).findTopByHouseholdIdAndReadingDateLessThanOrderByReadingDateDesc(
                anyLong(), any(LocalDate.class));
    }

    @Test
    void pdfDataUsesApartmentAndStoredBoundaryReadingsWithoutRecalculatingInvoice() {
        Household own = household(1L, "A-101");
        own.getApartment().setName("River View");
        own.getApartment().setAddress("12 Main Road");
        own.getApartment().setCity("Lucknow");
        own.setMeterNumber("MTR-101");
        Invoice storedInvoice = invoice(own);
        WaterUsageLog previous = reading(own, "2026-09-30", "100000");
        WaterUsageLog current = reading(own, "2026-10-31", "112000");
        when(invoiceRepo.findById(10L)).thenReturn(Optional.of(storedInvoice));
        when(userRepo.findByEmail("r@test.com")).thenReturn(Optional.of(user(Role.RESIDENT, own)));
        when(usageRepo.findTopByHouseholdIdAndReadingDateLessThanOrderByReadingDateDesc(
                1L, LocalDate.of(2026, 10, 1))).thenReturn(Optional.of(previous));
        when(usageRepo.findTopByHouseholdIdAndReadingDateBetweenOrderByReadingDateDesc(
                1L, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(Optional.of(current));

        var pdfData = service.getPdfData(10L, "r@test.com");

        assertEquals("River View", pdfData.apartmentName());
        assertEquals("12 Main Road", pdfData.apartmentAddress());
        assertEquals("MTR-101", pdfData.meterNumber());
        assertEquals(new BigDecimal("100000"), pdfData.previousMeterReading());
        assertEquals(new BigDecimal("112000"), pdfData.currentMeterReading());
        assertMoney("1272.00", pdfData.invoice().totalAmount());
        verify(calculator, never()).calculate(any(), any());
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