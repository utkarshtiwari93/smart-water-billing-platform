package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.DistributionResponse;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdShareResponse;
import com.infosys.Water_Usage_Monitoring.dto.InvoiceResponse;
import com.infosys.Water_Usage_Monitoring.dto.PurchaseSummaryResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.ApartmentHouseholdConsumption;
import com.infosys.Water_Usage_Monitoring.repository.BillingCycleRepository;
import com.infosys.Water_Usage_Monitoring.repository.NotificationRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AquaBotAdminContextServiceTest {

    @Mock UserRepository userRepository;
    @Mock BillingCycleRepository billingCycleRepository;
    @Mock WaterUsageLogRepository usageRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock DistributionService distributionService;
    @Mock BulkPurchaseService bulkPurchaseService;
    @Mock InvoiceService invoiceService;
    @InjectMocks AquaBotAdminContextService contextService;

    @Test
    void forAdmin_usesPersistedApartmentAndProducesCycleScopedReadOnlyContext() {
        User admin = user(11L, Role.ADMIN, 10L);
        BillingCycle openCycle = cycle(100L, 10L, CycleStatus.OPEN);
        when(userRepository.findByEmail("admin-a@example.com")).thenReturn(Optional.of(admin));
        when(billingCycleRepository.findTop13ByApartmentIdOrderByStartDateDescIdDesc(10L))
                .thenReturn(List.of(openCycle));
        when(distributionService.calculate(100L)).thenReturn(new DistributionResponse(
                100L, bd("20"), bd("12"), bd("8"), BigDecimal.ZERO, bd("20"), null,
                List.of(new HouseholdShareResponse(
                        200L, "A-1", true, bd("900"), bd("12"), "CONSUMPTION", bd("8"), bd("20")))));
        when(bulkPurchaseService.summary(100L))
                .thenReturn(new PurchaseSummaryResponse(100L, 1, bd("10"), bd("20"), bd("2")));
        when(usageRepository.summarizeApartmentHouseholdConsumption(
                10L, openCycle.getStartDate(), openCycle.getEndDate()))
                .thenReturn(List.of(consumption(200L, "A-1", "900")));
        when(notificationRepository.findLatestApartmentAlerts(
                eq(10L), any(), any(Pageable.class))).thenReturn(List.of());
        when(billingCycleRepository.countByApartmentIdAndStatus(10L, CycleStatus.OPEN)).thenReturn(1L);

        AquaBotAdminContext context = contextService.forAdmin("admin-a@example.com");

        assertEquals(1, context.periods().size());
        AquaBotAdminContext.PeriodSummary period = context.periods().get(0);
        assertEquals("OPEN", period.cycleStatus());
        assertFalse(period.finalized());
        assertTrue(period.allocationIsEstimate());
        assertEquals(1, context.openCycleCount());
        assertFalse(context.paymentStatusAvailable());
        assertEquals(1, period.householdsWithReadings());
        assertEquals(0, bd("900").compareTo(period.averageConsumptionLitres()));
        assertEquals(0, period.invoiceCount());
        assertEquals(BigDecimal.ZERO, period.invoiceTotal());
        assertEquals("A-1", period.highestConsumers().get(0).flatNumber());
        assertEquals("A-1", period.sharedCostAllocation().get(0).flatNumber());

        verify(billingCycleRepository).findTop13ByApartmentIdOrderByStartDateDescIdDesc(10L);
        verify(usageRepository).summarizeApartmentHouseholdConsumption(
                10L, openCycle.getStartDate(), openCycle.getEndDate());
        verify(notificationRepository).findLatestApartmentAlerts(
                eq(10L), any(), any(Pageable.class));
        verify(distributionService).calculate(100L);
        verify(bulkPurchaseService).summary(100L);
        verify(invoiceService, never()).summarizeForCycle(100L);

        verify(billingCycleRepository, never()).save(any());
        verify(billingCycleRepository, never()).delete(any(BillingCycle.class));
        verify(usageRepository, never()).save(any());
        verify(usageRepository, never()).delete(any());
        verify(notificationRepository, never()).save(any());
        verify(notificationRepository, never()).markAllRead(any());
        verify(bulkPurchaseService, never()).delete(any());
        verify(invoiceService, never()).finalizeCycle(any());
        verify(invoiceService, never()).archive(any());
    }

    @Test
    void forAdmin_neverQueriesAnotherApartment() {
        User admin = user(11L, Role.ADMIN, 10L);
        BillingCycle cycle = cycle(100L, 10L, CycleStatus.FINALIZED);
        when(userRepository.findByEmail("admin-a@example.com")).thenReturn(Optional.of(admin));
        when(billingCycleRepository.findTop13ByApartmentIdOrderByStartDateDescIdDesc(10L))
                .thenReturn(List.of(cycle));
        when(distributionService.calculate(100L)).thenReturn(new DistributionResponse(
                100L, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, null, List.of()));
        when(bulkPurchaseService.summary(100L))
                .thenReturn(new PurchaseSummaryResponse(100L, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));
        when(usageRepository.summarizeApartmentHouseholdConsumption(
                10L, cycle.getStartDate(), cycle.getEndDate())).thenReturn(List.of());
        when(invoiceService.summarizeForCycle(100L))
                .thenReturn(new InvoiceService.CycleSummary(1, bd("25")));
        when(notificationRepository.findLatestApartmentAlerts(
                eq(10L), any(), any(Pageable.class))).thenReturn(List.of());
        when(billingCycleRepository.countByApartmentIdAndStatus(10L, CycleStatus.OPEN)).thenReturn(0L);

        contextService.forAdmin("admin-a@example.com");

        verify(billingCycleRepository).findTop13ByApartmentIdOrderByStartDateDescIdDesc(10L);
        verify(billingCycleRepository, never()).findTop13ByApartmentIdOrderByStartDateDescIdDesc(20L);
        verify(usageRepository).summarizeApartmentHouseholdConsumption(
                10L, cycle.getStartDate(), cycle.getEndDate());
        verify(usageRepository, never()).summarizeApartmentHouseholdConsumption(
                eq(20L), any(), any());
        verify(notificationRepository).findLatestApartmentAlerts(
                eq(10L), any(), any(Pageable.class));
        verify(notificationRepository, never()).findLatestApartmentAlerts(
                eq(20L), any(), any(Pageable.class));
    }

    @Test
    void forAdmin_rejectsPersistedResidentBeforeAnalyticsQueries() {
        when(userRepository.findByEmail("resident@example.com"))
                .thenReturn(Optional.of(user(12L, Role.RESIDENT, 10L)));

        assertThrows(AccessDeniedException.class, () -> contextService.forAdmin("resident@example.com"));

        verifyNoInteractions(billingCycleRepository, usageRepository, notificationRepository,
                distributionService, bulkPurchaseService, invoiceService);
    }

    @Test
    void forAdmin_unknownIdentityFailsBeforeAnalyticsQueries() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contextService.forAdmin("missing@example.com"));

        verifyNoInteractions(billingCycleRepository, usageRepository, notificationRepository,
                distributionService, bulkPurchaseService, invoiceService);
    }

    private User user(Long id, Role role, Long apartmentId) {
        User user = new User();
        user.setId(id);
        user.setEmail("admin-a@example.com");
        user.setRole(role);
        if (apartmentId != null) {
            Apartment apartment = new Apartment();
            apartment.setId(apartmentId);
            user.setApartment(apartment);
        }
        return user;
    }

    private BillingCycle cycle(Long id, Long apartmentId, CycleStatus status) {
        Apartment apartment = new Apartment();
        apartment.setId(apartmentId);
        BillingCycle cycle = new BillingCycle();
        cycle.setId(id);
        cycle.setApartment(apartment);
        cycle.setStartDate(LocalDate.of(2026, 10, 1));
        cycle.setEndDate(LocalDate.of(2026, 10, 31));
        cycle.setStatus(status);
        return cycle;
    }

    private ApartmentHouseholdConsumption consumption(Long householdId, String flatNumber, String litres) {
        return new ApartmentHouseholdConsumption() {
            @Override
            public Long getHouseholdId() {
                return householdId;
            }

            @Override
            public String getFlatNumber() {
                return flatNumber;
            }

            @Override
            public BigDecimal getConsumptionLitres() {
                return bd(litres);
            }
        };
    }

    private BigDecimal bd(String value) {
        return new BigDecimal(value);
    }
}
