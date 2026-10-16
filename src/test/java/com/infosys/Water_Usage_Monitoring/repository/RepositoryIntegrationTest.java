package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import com.infosys.Water_Usage_Monitoring.enums.ReadingSource;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryIntegrationTest {

    @Autowired ApartmentRepository apartmentRepo;
    @Autowired HouseholdRepository householdRepo;
    @Autowired UserRepository userRepo;
    @Autowired WaterUsageLogRepository logRepo;

    private Household savedHousehold(String flat, String meter) {
        Apartment a = new Apartment();
        a.setName("Test Apt");
        apartmentRepo.save(a);

        Household h = new Household();
        h.setApartment(a);
        h.setFlatNumber(flat);
        h.setMeterNumber(meter);
        return householdRepo.save(h);
    }

    private WaterUsageLog log(Household h, LocalDate d, String reading) {
        WaterUsageLog l = new WaterUsageLog();
        l.setHousehold(h);
        l.setReadingDate(d);
        l.setMeterReading(new BigDecimal(reading));
        l.setConsumptionLitres(BigDecimal.ZERO);
        l.setSource(ReadingSource.MANUAL);
        return l;
    }

    @Test
    void savesApartmentHouseholdAndLog_andFindersWork() {
        Household h = savedHousehold("A-101", "TST-M1");
        logRepo.save(log(h, LocalDate.of(2026, 10, 8), "1250"));

        assertTrue(logRepo.existsByHouseholdIdAndReadingDate(h.getId(), LocalDate.of(2026, 10, 8)));
        assertEquals(1, householdRepo.findByApartmentId(h.getApartment().getId()).size());
        assertTrue(householdRepo.findByMeterNumber("TST-M1").isPresent());
        assertTrue(householdRepo.existsByApartmentIdAndFlatNumber(h.getApartment().getId(), "A-101"));
    }

    @Test
    void duplicateReadingSameDay_isRejectedByUniqueConstraint() {
        Household h = savedHousehold("A-102", "TST-M2");
        LocalDate d = LocalDate.of(2026, 10, 8);
        logRepo.saveAndFlush(log(h, d, "1250"));

        assertThrows(DataIntegrityViolationException.class,
                () -> logRepo.saveAndFlush(log(h, d, "1262")));
    }

    @Test
    void latestReading_returnsMostRecentByDate() {
        Household h = savedHousehold("A-103", "TST-M3");
        logRepo.save(log(h, LocalDate.of(2026, 10, 1), "1000"));
        logRepo.save(log(h, LocalDate.of(2026, 10, 3), "1030"));
        logRepo.save(log(h, LocalDate.of(2026, 10, 2), "1015"));

        WaterUsageLog latest = logRepo.findTopByHouseholdIdOrderByReadingDateDesc(h.getId()).orElseThrow();

        assertEquals(LocalDate.of(2026, 10, 3), latest.getReadingDate());
    }

    @Test
    void userFinders_workByEmailAndRole() {
        Household h = savedHousehold("A-104", "TST-M4");
        User u = new User();
        u.setApartment(h.getApartment());
        u.setName("Repo Test");
        u.setEmail("repo-test@test.com");
        u.setPasswordHash("HASH");
        u.setRole(Role.RESIDENT);
        userRepo.save(u);

        assertTrue(userRepo.findByEmail("repo-test@test.com").isPresent());
        assertTrue(userRepo.existsByEmail("repo-test@test.com"));
        assertFalse(userRepo.existsByEmail("nobody@test.com"));
        assertEquals(1, userRepo.findByApartmentId(h.getApartment().getId()).size());
    }
}