package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.BillPreviewResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingPreviewServiceTest {

    @Mock BillingCycleService cycleService;
    @Mock HouseholdRepository householdRepo;
    @Mock WaterUsageLogRepository logRepo;
    @Mock UserRepository userRepo;
    @Spy TariffCalculator calculator = new TariffCalculator();
    @InjectMocks BillingPreviewService service;

    private final LocalDate start = LocalDate.of(2026, 10, 1);
    private final LocalDate end = LocalDate.of(2026, 10, 31);

    private Apartment apartment(long id) {
        Apartment a = new Apartment();
        a.setId(id);
        return a;
    }

    private BillingCycle cycle() {
        TariffPlan p = new TariffPlan();
        p.setId(1L);
        p.setTier1LimitKl(new BigDecimal("10"));
        p.setBaseRate(new BigDecimal("10"));
        p.setHigherRate(new BigDecimal("20"));

        BillingCycle c = new BillingCycle();
        c.setId(1L);
        c.setApartment(apartment(1L));
        c.setTariffPlan(p);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setStatus(CycleStatus.OPEN);
        return c;
    }

    private Household household(long id, String flat, long apartmentId) {
        Household h = new Household();
        h.setId(id);
        h.setFlatNumber(flat);
        h.setApartment(apartment(apartmentId));
        return h;
    }

    private User user(Role role, Household household) {
        User u = new User();
        u.setRole(role);
        u.setHousehold(household);
        return u;
    }

    @Test
    void admin_preview_appliesTiersToCycleConsumption() {
        Household h = household(1L, "A-101", 1L);
        when(cycleService.find(1L)).thenReturn(cycle());
        when(userRepo.findByEmail("a@test.com")).thenReturn(Optional.of(user(Role.ADMIN, null)));
        when(householdRepo.findById(1L)).thenReturn(Optional.of(h));
        when(logRepo.sumConsumption(1L, start, end)).thenReturn(new BigDecimal("14000"));

        BillPreviewResponse r = service.preview(1L, 1L, "a@test.com");

        assertEquals(0, r.tier1Charge().compareTo(new BigDecimal("100.00")));
        assertEquals(0, r.tier2Charge().compareTo(new BigDecimal("80.00")));
        assertEquals(0, r.waterCharge().compareTo(new BigDecimal("180.00")));
        assertEquals("A-101", r.flatNumber());
    }

    @Test
    void household_withNoReadings_hasZeroCharge() {
        Household h = household(1L, "A-101", 1L);
        when(cycleService.find(1L)).thenReturn(cycle());
        when(userRepo.findByEmail("a@test.com")).thenReturn(Optional.of(user(Role.ADMIN, null)));
        when(householdRepo.findById(1L)).thenReturn(Optional.of(h));
        when(logRepo.sumConsumption(1L, start, end)).thenReturn(null);

        BillPreviewResponse r = service.preview(1L, 1L, "a@test.com");

        assertEquals(0, r.waterCharge().compareTo(BigDecimal.ZERO));
    }

    @Test
    void resident_withoutHouseholdId_getsOwnBill() {
        Household own = household(1L, "A-101", 1L);
        when(cycleService.find(1L)).thenReturn(cycle());
        when(userRepo.findByEmail("r@test.com")).thenReturn(Optional.of(user(Role.RESIDENT, own)));
        when(householdRepo.findById(1L)).thenReturn(Optional.of(own));
        when(logRepo.sumConsumption(1L, start, end)).thenReturn(new BigDecimal("6000"));

        BillPreviewResponse r = service.preview(1L, null, "r@test.com");

        assertEquals(1L, r.householdId());
        assertEquals(0, r.waterCharge().compareTo(new BigDecimal("60.00")));
    }

    @Test
    void resident_cannotViewAnotherHouseholdsBill() {
        when(cycleService.find(1L)).thenReturn(cycle());
        when(userRepo.findByEmail("r@test.com"))
                .thenReturn(Optional.of(user(Role.RESIDENT, household(2L, "A-102", 1L))));

        assertThrows(AccessDeniedException.class, () -> service.preview(1L, 1L, "r@test.com"));
    }

    @Test
    void admin_withoutHouseholdId_isRejected() {
        when(cycleService.find(1L)).thenReturn(cycle());
        when(userRepo.findByEmail("a@test.com")).thenReturn(Optional.of(user(Role.ADMIN, null)));

        assertThrows(BadRequestException.class, () -> service.preview(1L, null, "a@test.com"));
    }

    @Test
    void household_fromAnotherApartment_isRejected() {
        when(cycleService.find(1L)).thenReturn(cycle());
        when(userRepo.findByEmail("a@test.com")).thenReturn(Optional.of(user(Role.ADMIN, null)));
        when(householdRepo.findById(5L)).thenReturn(Optional.of(household(5L, "Z-1", 2L)));

        assertThrows(BadRequestException.class, () -> service.preview(1L, 5L, "a@test.com"));
    }

    @Test
    void previewAll_returnsEveryHouseholdSortedByFlat() {
        Household b = household(2L, "B-202", 1L);
        Household a = household(1L, "A-101", 1L);
        when(cycleService.find(1L)).thenReturn(cycle());
        when(householdRepo.findByApartmentId(1L)).thenReturn(List.of(b, a));
        when(logRepo.sumConsumption(1L, start, end)).thenReturn(new BigDecimal("14000"));
        when(logRepo.sumConsumption(2L, start, end)).thenReturn(new BigDecimal("6000"));

        List<BillPreviewResponse> result = service.previewAll(1L);

        assertEquals(2, result.size());
        assertEquals("A-101", result.get(0).flatNumber());
        assertEquals(0, result.get(0).waterCharge().compareTo(new BigDecimal("180.00")));
        assertEquals("B-202", result.get(1).flatNumber());
        assertEquals(0, result.get(1).waterCharge().compareTo(new BigDecimal("60.00")));
    }
}