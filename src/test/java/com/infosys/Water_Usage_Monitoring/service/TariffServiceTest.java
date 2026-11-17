package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.TariffPlanRequest;
import com.infosys.Water_Usage_Monitoring.dto.TariffPlanResponse;
import com.infosys.Water_Usage_Monitoring.dto.TariffSlabRequest;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
        assertEquals(2, r.slabs().size());
    }

    @Test
    void create_multipleSlabs_persistsProgressiveRanges() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(repo.save(any(TariffPlan.class))).thenAnswer(i -> i.getArgument(0));
        TariffPlanRequest request = new TariffPlanRequest("Progressive", null, null, null,
                LocalDate.of(2026, 10, 1), true,
                List.of(new TariffSlabRequest(new BigDecimal("5"), new BigDecimal("8")),
                        new TariffSlabRequest(new BigDecimal("15"), new BigDecimal("12")),
                        new TariffSlabRequest(null, new BigDecimal("20"))));

        TariffPlanResponse response = service.create(1L, request);

        assertEquals(3, response.slabs().size());
        assertEquals(new BigDecimal("15"), response.slabs().get(2).fromKl());
        assertEquals(new BigDecimal("20"), response.slabs().get(2).rate());
    }

    @Test
    void create_nonIncreasingOrNonProgressiveSlabs_areRejected() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        TariffPlanRequest decreasingLimit = new TariffPlanRequest("Invalid", null, null, null,
                LocalDate.of(2026, 10, 1), true,
                List.of(new TariffSlabRequest(new BigDecimal("10"), new BigDecimal("10")),
                        new TariffSlabRequest(new BigDecimal("8"), new BigDecimal("20")),
                        new TariffSlabRequest(null, new BigDecimal("30"))));
        TariffPlanRequest decreasingRate = new TariffPlanRequest("Invalid", null, null, null,
                LocalDate.of(2026, 10, 1), true,
                List.of(new TariffSlabRequest(new BigDecimal("10"), new BigDecimal("20")),
                        new TariffSlabRequest(null, new BigDecimal("10"))));

        assertThrows(BadRequestException.class, () -> service.create(1L, decreasingLimit));
        assertThrows(BadRequestException.class, () -> service.create(1L, decreasingRate));
        verify(repo, never()).save(any(TariffPlan.class));
    }

    @Test
    void create_singleUnboundedSlab_isAccepted() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(repo.save(any(TariffPlan.class))).thenAnswer(i -> i.getArgument(0));
        TariffPlanRequest request = new TariffPlanRequest("Flat rate", BigDecimal.ZERO,
                new BigDecimal("12"), new BigDecimal("12"), LocalDate.of(2026, 10, 1), true,
                List.of(new TariffSlabRequest(null, new BigDecimal("12"))));

        TariffPlanResponse response = service.create(1L, request);

        assertEquals(1, response.slabs().size());
        assertNull(response.slabs().get(0).upToKl());
    }

    @Test
    void create_higherRateBelowBaseRate_isRejected() {
        when(apartmentService.find(1L)).thenReturn(apartment);

        assertThrows(BadRequestException.class, () -> service.create(1L, req("Bad", "10", "20", "10")));
        verify(repo, never()).save(any(TariffPlan.class));
    }

    @Test
    void create_duplicateActiveEffectiveDate_isRejected() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(repo.existsByApartmentIdAndEffectiveFromAndActiveTrue(1L, LocalDate.of(2026, 10, 1)))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(1L, req("Duplicate", "10", "10", "20")));
        verify(repo, never()).save(any(TariffPlan.class));
    }

    @Test
    void create_negativeRate_isRejected() {
        when(apartmentService.find(1L)).thenReturn(apartment);

        assertThrows(BadRequestException.class, () -> service.create(1L, req("Bad", "10", "-1", "2")));
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
    void update_duplicateActiveEffectiveDate_isRejected() {
        when(repo.findById(7L)).thenReturn(Optional.of(existingPlan()));
        when(repo.existsByApartmentIdAndIdNotAndEffectiveFromAndActiveTrue(
                1L, 7L, LocalDate.of(2026, 10, 1))).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.update(7L, req("Standard", "10", "10", "20")));
    }

    @Test
    void activation_deactivatesPlanAndRejectsAmbiguousReactivation() {
        TariffPlan plan = existingPlan();
        when(repo.findById(7L)).thenReturn(Optional.of(plan));

        assertEquals(false, service.setActive(7L, false).active());
        when(repo.existsByApartmentIdAndIdNotAndEffectiveFromAndActiveTrue(
                1L, 7L, plan.getEffectiveFrom())).thenReturn(true);
        assertThrows(ConflictException.class, () -> service.setActive(7L, true));
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