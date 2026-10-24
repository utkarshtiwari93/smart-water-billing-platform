package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.BillingCycleRequest;
import com.infosys.Water_Usage_Monitoring.dto.BillingCycleResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingCycleServiceTest {

    @Mock BillingCycleRepository cycleRepo;
    @Mock TariffPlanRepository planRepo;
    @Mock TariffService tariffService;
    @Mock ApartmentService apartmentService;
    @InjectMocks BillingCycleService service;

    private final LocalDate start = LocalDate.of(2026, 10, 1);
    private final LocalDate end = LocalDate.of(2026, 10, 31);
    private Apartment apartment;

    @BeforeEach
    void setUp() {
        apartment = new Apartment();
        apartment.setId(1L);
    }

    private TariffPlan plan(long id, long apartmentId, LocalDate effectiveFrom, boolean active) {
        Apartment a = new Apartment();
        a.setId(apartmentId);
        TariffPlan p = new TariffPlan();
        p.setId(id);
        p.setApartment(a);
        p.setName("Standard");
        p.setEffectiveFrom(effectiveFrom);
        p.setActive(active);
        return p;
    }

    private BillingCycle cycle(long id, CycleStatus status) {
        BillingCycle c = new BillingCycle();
        c.setId(id);
        c.setApartment(apartment);
        c.setTariffPlan(plan(5L, 1L, start, true));
        c.setStartDate(start);
        c.setEndDate(end);
        c.setStatus(status);
        return c;
    }

    private void stubNoOverlap() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(cycleRepo.existsByApartmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(1L, end, start))
                .thenReturn(false);
    }

    @Test
    void create_withExplicitPlan_createsOpenCycle() {
        stubNoOverlap();
        when(tariffService.find(5L)).thenReturn(plan(5L, 1L, start, true));
        when(cycleRepo.save(any(BillingCycle.class))).thenAnswer(i -> i.getArgument(0));

        BillingCycleResponse r = service.create(1L, new BillingCycleRequest(start, end, 5L));

        assertEquals("OPEN", r.status());
        assertEquals(5L, r.tariffPlanId());
    }

    @Test
    void create_withoutPlan_usesLatestEffectivePlan() {
        stubNoOverlap();
        when(planRepo.findFirstByApartmentIdAndActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(1L, start))
                .thenReturn(Optional.of(plan(9L, 1L, start, true)));
        when(cycleRepo.save(any(BillingCycle.class))).thenAnswer(i -> i.getArgument(0));

        BillingCycleResponse r = service.create(1L, new BillingCycleRequest(start, end, null));

        assertEquals(9L, r.tariffPlanId());
    }

    @Test
    void create_noPlanAvailable_isRejected() {
        stubNoOverlap();
        when(planRepo.findFirstByApartmentIdAndActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(1L, start))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.create(1L, new BillingCycleRequest(start, end, null)));
    }

    @Test
    void create_endBeforeStart_isRejected() {
        assertThrows(BadRequestException.class,
                () -> service.create(1L, new BillingCycleRequest(end, start, null)));
    }

    @Test
    void create_overlappingCycle_isRejected() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(cycleRepo.existsByApartmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(1L, end, start))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(1L, new BillingCycleRequest(start, end, 5L)));
        verify(cycleRepo, never()).save(any(BillingCycle.class));
    }

    @Test
    void create_planFromAnotherApartment_isRejected() {
        stubNoOverlap();
        when(tariffService.find(5L)).thenReturn(plan(5L, 2L, start, true));

        assertThrows(BadRequestException.class, () -> service.create(1L, new BillingCycleRequest(start, end, 5L)));
    }

    @Test
    void create_planNotYetEffective_isRejected() {
        stubNoOverlap();
        when(tariffService.find(5L)).thenReturn(plan(5L, 1L, LocalDate.of(2026, 11, 1), true));

        assertThrows(BadRequestException.class, () -> service.create(1L, new BillingCycleRequest(start, end, 5L)));
    }

    @Test
    void update_nonOpenCycle_isRejected() {
        when(cycleRepo.findById(9L)).thenReturn(Optional.of(cycle(9L, CycleStatus.FINALIZED)));

        assertThrows(ConflictException.class, () -> service.update(9L, new BillingCycleRequest(start, end, null)));
    }

    @Test
    void delete_openCycle_succeeds() {
        BillingCycle c = cycle(9L, CycleStatus.OPEN);
        when(cycleRepo.findById(9L)).thenReturn(Optional.of(c));

        service.delete(9L);

        verify(cycleRepo).delete(c);
    }

    @Test
    void delete_finalizedCycle_isRejected() {
        when(cycleRepo.findById(9L)).thenReturn(Optional.of(cycle(9L, CycleStatus.FINALIZED)));

        assertThrows(ConflictException.class, () -> service.delete(9L));
        verify(cycleRepo, never()).delete(any(BillingCycle.class));
    }
}