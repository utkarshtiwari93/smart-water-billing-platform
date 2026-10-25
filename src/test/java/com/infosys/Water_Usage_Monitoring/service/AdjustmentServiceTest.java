package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.AdjustmentRequest;
import com.infosys.Water_Usage_Monitoring.dto.AdjustmentResponse;
import com.infosys.Water_Usage_Monitoring.entity.Adjustment;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.repository.AdjustmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdjustmentServiceTest {

    @Mock AdjustmentRepository repo;
    @Mock BillingCycleService cycleService;
    @Mock HouseholdRepository householdRepo;
    @InjectMocks AdjustmentService service;

    private Apartment apartment(long id) {
        Apartment a = new Apartment();
        a.setId(id);
        return a;
    }

    private BillingCycle cycle(CycleStatus status) {
        BillingCycle c = new BillingCycle();
        c.setId(1L);
        c.setApartment(apartment(1L));
        c.setStatus(status);
        return c;
    }

    private Household household(long apartmentId) {
        Household h = new Household();
        h.setId(5L);
        h.setFlatNumber("A-101");
        h.setApartment(apartment(apartmentId));
        return h;
    }

    private AdjustmentRequest req(String amount) {
        return new AdjustmentRequest(5L, new BigDecimal(amount), "  Late fee  ");
    }

    @Test
    void create_success_trimsReasonAndRecordsAdmin() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.OPEN));
        when(householdRepo.findById(5L)).thenReturn(Optional.of(household(1L)));
        when(repo.save(any(Adjustment.class))).thenAnswer(i -> i.getArgument(0));

        AdjustmentResponse r = service.create(1L, req("50.00"), "admin@water.local");

        assertEquals("Late fee", r.reason());
        assertEquals("admin@water.local", r.createdBy());
        assertEquals(0, r.amount().compareTo(new BigDecimal("50.00")));
    }

    @Test
    void create_negativeAmountIsAllowedAsCredit() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.OPEN));
        when(householdRepo.findById(5L)).thenReturn(Optional.of(household(1L)));
        when(repo.save(any(Adjustment.class))).thenAnswer(i -> i.getArgument(0));

        AdjustmentResponse r = service.create(1L, req("-30.00"), "admin@water.local");

        assertEquals(0, r.amount().compareTo(new BigDecimal("-30.00")));
    }

    @Test
    void create_zeroAmount_isRejected() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.OPEN));

        assertThrows(BadRequestException.class, () -> service.create(1L, req("0"), "admin@water.local"));
        verify(repo, never()).save(any(Adjustment.class));
    }

    @Test
    void create_inFinalizedCycle_isRejected() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.FINALIZED));

        assertThrows(ConflictException.class, () -> service.create(1L, req("50"), "admin@water.local"));
    }

    @Test
    void create_householdFromAnotherApartment_isRejected() {
        when(cycleService.find(1L)).thenReturn(cycle(CycleStatus.OPEN));
        when(householdRepo.findById(5L)).thenReturn(Optional.of(household(2L)));

        assertThrows(BadRequestException.class, () -> service.create(1L, req("50"), "admin@water.local"));
    }

    @Test
    void delete_inFinalizedCycle_isRejected() {
        Adjustment a = new Adjustment();
        a.setId(9L);
        a.setBillingCycle(cycle(CycleStatus.FINALIZED));
        when(repo.findById(9L)).thenReturn(Optional.of(a));

        assertThrows(ConflictException.class, () -> service.delete(9L));
        verify(repo, never()).delete(any(Adjustment.class));
    }
}