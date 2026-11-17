package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.UsageRequest;
import com.infosys.Water_Usage_Monitoring.dto.UsageResponse;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import com.infosys.Water_Usage_Monitoring.enums.ReadingSource;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.DuplicateResourceException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock WaterUsageLogRepository logRepo;
    @Mock HouseholdRepository householdRepo;
    @Mock UserRepository userRepo;
    @Mock ConsumptionAggregationService aggregationService;
    @InjectMocks UsageService service;

    private Household household;

    @BeforeEach
    void setUp() {
        household = new Household();
        household.setId(1L);
    }

    private WaterUsageLog log(LocalDate date, String reading) {
        WaterUsageLog l = new WaterUsageLog();
        l.setHousehold(household);
        l.setReadingDate(date);
        l.setMeterReading(new BigDecimal(reading));
        l.setConsumptionLitres(BigDecimal.ZERO);
        l.setSource(ReadingSource.MANUAL);
        return l;
    }

    private void stubHousehold(LocalDate date, boolean exists) {
        when(householdRepo.findById(1L)).thenReturn(Optional.of(household));
        when(logRepo.existsByHouseholdIdAndReadingDate(1L, date)).thenReturn(exists);
    }

    @Test
    void firstReading_isBaselineWithZeroConsumption() {
        LocalDate d = LocalDate.of(2026, 10, 1);
        stubHousehold(d, false);
        when(logRepo.findTopByHouseholdIdOrderByReadingDateDesc(1L)).thenReturn(Optional.empty());
        when(logRepo.save(any(WaterUsageLog.class))).thenAnswer(i -> i.getArgument(0));

        UsageResponse r = service.createManual(new UsageRequest(1L, d, new BigDecimal("1250")));

        assertEquals(0, r.consumptionLitres().compareTo(BigDecimal.ZERO));
        assertEquals("MANUAL", r.source());
    }

    @Test
    void nextReading_consumptionIsDifferenceFromPrevious() {
        LocalDate d = LocalDate.of(2026, 10, 2);
        stubHousehold(d, false);
        when(logRepo.findTopByHouseholdIdOrderByReadingDateDesc(1L))
                .thenReturn(Optional.of(log(LocalDate.of(2026, 10, 1), "1250")));
        when(logRepo.save(any(WaterUsageLog.class))).thenAnswer(i -> i.getArgument(0));

        UsageResponse r = service.createManual(new UsageRequest(1L, d, new BigDecimal("1262")));

        assertEquals(0, r.consumptionLitres().compareTo(new BigDecimal("12")));
    }

    @Test
    void duplicateDate_isRejectedAndNothingSaved() {
        LocalDate d = LocalDate.of(2026, 10, 2);
        stubHousehold(d, true);

        assertThrows(DuplicateResourceException.class,
                () -> service.createManual(new UsageRequest(1L, d, new BigDecimal("1262"))));
        verify(logRepo, never()).save(any(WaterUsageLog.class));
    }

    @Test
    void readingLowerThanPrevious_isRejected() {
        LocalDate d = LocalDate.of(2026, 10, 2);
        stubHousehold(d, false);
        when(logRepo.findTopByHouseholdIdOrderByReadingDateDesc(1L))
                .thenReturn(Optional.of(log(LocalDate.of(2026, 10, 1), "1250")));

        assertThrows(BadRequestException.class,
                () -> service.createManual(new UsageRequest(1L, d, new BigDecimal("1200"))));
    }

    @Test
    void dateNotAfterLatestReading_isRejected() {
        LocalDate d = LocalDate.of(2026, 9, 30);
        stubHousehold(d, false);
        when(logRepo.findTopByHouseholdIdOrderByReadingDateDesc(1L))
                .thenReturn(Optional.of(log(LocalDate.of(2026, 10, 1), "1250")));

        assertThrows(BadRequestException.class,
                () -> service.createManual(new UsageRequest(1L, d, new BigDecimal("1300"))));
    }

    @Test
    void unknownHousehold_throwsNotFound() {
        when(householdRepo.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.createManual(new UsageRequest(9L, LocalDate.of(2026, 10, 1), BigDecimal.TEN)));
    }

    private User user(Role role, Long householdId) {
        User u = new User();
        u.setRole(role);
        if (householdId != null) {
            Household h = new Household();
            h.setId(householdId);
            u.setHousehold(h);
        }
        return u;
    }

    @Test
    void resident_cannotViewAnotherHouseholdsUsage() {
        when(userRepo.findByEmail("r@test.com")).thenReturn(Optional.of(user(Role.RESIDENT, 2L)));

        assertThrows(AccessDeniedException.class, () -> service.list(1L, null, null, "r@test.com"));
    }

    @Test
    void resident_canViewOwnHouseholdUsage() {
        when(userRepo.findByEmail("r@test.com")).thenReturn(Optional.of(user(Role.RESIDENT, 1L)));
        when(householdRepo.existsById(1L)).thenReturn(true);
        when(logRepo.findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(
                eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(log(LocalDate.of(2026, 10, 1), "1250")));

        assertEquals(1, service.list(1L, null, null, "r@test.com").size());
    }

    @Test
    void admin_listingUnknownHousehold_throwsNotFound() {
        when(userRepo.findByEmail("a@test.com")).thenReturn(Optional.of(user(Role.ADMIN, null)));
        when(householdRepo.existsById(7L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.list(7L, null, null, "a@test.com"));
    }

    @Test
    void fromAfterTo_isRejected() {
        when(userRepo.findByEmail("a@test.com")).thenReturn(Optional.of(user(Role.ADMIN, null)));
        when(householdRepo.existsById(1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.list(1L,
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 1), "a@test.com"));
    }

    @Test
    void benchmark_comparesResidentWithApartmentAndSimilarSizedHomes() {
        Apartment apartment = new Apartment();
        apartment.setId(7L);
        Household own = new Household();
        own.setId(1L);
        own.setApartment(apartment);
        own.setAreaSqft(new BigDecimal("1000"));
        Household peer = new Household();
        peer.setId(2L);
        peer.setApartment(apartment);
        peer.setAreaSqft(new BigDecimal("1150"));
        User resident = user(Role.RESIDENT, 1L);
        resident.setHousehold(own);

        when(aggregationService.apartment(eq(7L), any(LocalDate.class), any(LocalDate.class),
                eq(ConsumptionAggregationService.Granularity.DAY)))
                .thenReturn(Map.of(
                        1L, new ConsumptionAggregationService.Aggregate(
                                new BigDecimal("100"), 30, LocalDate.of(2026, 10, 31), List.of()),
                        2L, new ConsumptionAggregationService.Aggregate(
                                new BigDecimal("300"), 30, LocalDate.of(2026, 10, 31), List.of())));
        when(userRepo.findByEmail("r@test.com")).thenReturn(Optional.of(resident));
        when(householdRepo.findByApartmentId(7L)).thenReturn(List.of(own, peer));

        var result = service.benchmark(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "r@test.com");

        assertEquals(0, result.householdConsumptionLitres().compareTo(new BigDecimal("100")));
        assertEquals(0, result.apartmentAverageLitres().compareTo(new BigDecimal("200.00")));
        assertEquals(0, result.similarHouseholdAverageLitres().compareTo(new BigDecimal("300")));
        assertEquals(1, result.similarHouseholdCount());
        assertEquals(-50.0, result.householdVsApartmentPercent().doubleValue());
        assertEquals(true, result.similarGroupAvailable());
    }

    @Test
    void benchmarkExcludesHouseholdsWithoutValidReadingIntervals() {
        Apartment apartment = new Apartment();
        apartment.setId(7L);
        Household own = new Household();
        own.setId(1L);
        own.setApartment(apartment);
        own.setAreaSqft(new BigDecimal("1000"));
        Household peer = new Household();
        peer.setId(2L);
        peer.setApartment(apartment);
        peer.setAreaSqft(new BigDecimal("1000"));
        User resident = user(Role.RESIDENT, 1L);
        resident.setHousehold(own);

        when(userRepo.findByEmail("r@test.com")).thenReturn(Optional.of(resident));
        when(householdRepo.findByApartmentId(7L)).thenReturn(List.of(own, peer));
        when(aggregationService.apartment(eq(7L), any(LocalDate.class), any(LocalDate.class),
                eq(ConsumptionAggregationService.Granularity.DAY)))
                .thenReturn(Map.of(2L, new ConsumptionAggregationService.Aggregate(
                        new BigDecimal("20"), 10, LocalDate.of(2026, 10, 31), List.of())));

        var result = service.benchmark(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), "r@test.com");

        assertEquals(null, result.householdConsumptionLitres());
        assertEquals(1, result.apartmentHouseholdCount());
        assertEquals(0, result.apartmentAverageLitres().compareTo(new BigDecimal("20.00")));
        assertEquals(false, result.similarGroupAvailable());
    }

    @Test
    void trendRejectsAdminAccessToAnotherApartment() {
        Apartment assignedApartment = new Apartment();
        assignedApartment.setId(8L);
        Apartment targetApartment = new Apartment();
        targetApartment.setId(7L);
        Household target = new Household();
        target.setId(2L);
        target.setApartment(targetApartment);
        User admin = user(Role.ADMIN, null);
        admin.setApartment(assignedApartment);
        when(userRepo.findByEmail("a@test.com")).thenReturn(Optional.of(admin));
        when(householdRepo.findById(2L)).thenReturn(Optional.of(target));

        assertThrows(AccessDeniedException.class, () -> service.trend(2L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                ConsumptionAggregationService.Granularity.DAY, "a@test.com"));
        verify(aggregationService, never()).household(any(), any(), any(), any());
    }

    @Test
    void comparisonUsesEqualLengthPreviousPeriodAndReportsPercentageChange() {
        Apartment apartment = new Apartment();
        apartment.setId(7L);
        Household own = new Household();
        own.setId(1L);
        own.setApartment(apartment);
        User resident = user(Role.RESIDENT, 1L);
        resident.setHousehold(own);
        when(userRepo.findByEmail("r@test.com")).thenReturn(Optional.of(resident));
        when(householdRepo.findById(1L)).thenReturn(Optional.of(own));
        when(aggregationService.household(eq(1L), any(LocalDate.class), any(LocalDate.class),
                eq(ConsumptionAggregationService.Granularity.DAY)))
                .thenReturn(new ConsumptionAggregationService.Aggregate(
                                new BigDecimal("120"), 30, LocalDate.of(2026, 11, 30), List.of()),
                        new ConsumptionAggregationService.Aggregate(
                                new BigDecimal("100"), 30, LocalDate.of(2026, 10, 31), List.of()));

        var result = service.compare(1L, LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30), "r@test.com");

        assertEquals(LocalDate.of(2026, 10, 2), result.previousFrom());
        assertEquals(LocalDate.of(2026, 10, 31), result.previousTo());
        assertEquals(0, result.percentageDifference().compareTo(new BigDecimal("20.00")));
    }
}