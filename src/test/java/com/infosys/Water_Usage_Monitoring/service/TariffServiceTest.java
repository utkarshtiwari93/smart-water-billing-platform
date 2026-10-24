package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.TariffPlanRequest;
import com.infosys.Water_Usage_Monitoring.dto.TariffPlanResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.repository.BillingCycleRepository;
import com.infosys.Water_Usage_Monitoring.repository.TariffPlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TariffServiceTest {

    @Mock TariffPlanRepository repo;
    @Mock BillingCycleRepository cycleRepo;
    @Mock ApartmentService apartmentService;
    @InjectMocks TariffService service;

    private Apartment apartment;

    @BeforeEach
    void setUp() {
        apartment = new Apartment();
        apartment.setId(1L);
    }

    private TariffPlanRequest req(String name, String tier1, String base, String higher) {
        return new TariffPlanRequest(name, new BigDecimal(tier1), new BigDecimal(base),
                new BigDecimal(higher), LocalDate.of(2026, 10, 1), true);
    }

    private TariffPlan existingPlan() {
        TariffPlan p = new TariffPlan();
        p.setId(7L);
        p.setApartment(apartment);
        p.setName("Standard");
        p.setTier1LimitKl(new BigDecimal("10.00"));
        p.setBaseRate(new BigDecimal("10.00"));
        p.setHigherRate(new BigDecimal("20.00"));
        p.setEffectiveFrom(LocalDate.of(2026, 10, 1));
        p.setActive(true);
        return p;
    }

    @Test
    void create_success() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(repo.save(any(TariffPlan.class))).thenAnswer(i -> i.getArgument(0));

        TariffPlanResponse r = service.create(1L, req("Standard", "10", "10", "20"));

        assertEquals("Standard", r.name());
        assertEquals(1L, r.apartmentId());
        assertTrue(r.active());
    }

    @Test
    void create_higherRateBelowBaseRate_isRejected() {
        when(apartmentService.find(1L)).thenReturn(apartment);

        assertThrows(BadRequestException.class, () -> service.create(1L, req("Bad", "10", "20", "10")));
        verify(repo, never()).save(any(TariffPlan.class));
    }

    @Test
    void update_ratesChanged_onFinalizedPlan_isRejected() {
        when(repo.findById(7L)).thenReturn(Optional.of(existingPlan()));
        when(cycleRepo.existsByTariffPlanIdAndStatusIn(eq(7L), anyCollection())).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(7L, req("Standard", "10", "12", "25")));
    }

    @Test
    void update_onlyName_onFinalizedPlan_isAllowed() {
        when(repo.findById(7L)).thenReturn(Optional.of(existingPlan()));

        TariffPlanResponse r = service.update(7L, req("Renamed", "10", "10", "20"));

        assertEquals("Renamed", r.name());
        verify(cycleRepo, never()).existsByTariffPlanIdAndStatusIn(eq(7L), anyCollection());
    }

    @Test
    void delete_planUsedByCycle_isRejected() {
        TariffPlan plan = existingPlan();
        when(repo.findById(7L)).thenReturn(Optional.of(plan));
        when(cycleRepo.existsByTariffPlanId(7L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.delete(7L));
        verify(repo, never()).delete(any(TariffPlan.class));
    }

    @Test
    void delete_unusedPlan_succeeds() {
        TariffPlan plan = existingPlan();
        when(repo.findById(7L)).thenReturn(Optional.of(plan));
        when(cycleRepo.existsByTariffPlanId(7L)).thenReturn(false);

        service.delete(7L);

        verify(repo).delete(plan);
    }
}