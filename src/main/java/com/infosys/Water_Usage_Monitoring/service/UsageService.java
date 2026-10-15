package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.UsageRequest;
import com.infosys.Water_Usage_Monitoring.dto.UsageResponse;
import com.infosys.Water_Usage_Monitoring.entity.Household;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final WaterUsageLogRepository logRepo;
    private final HouseholdRepository householdRepo;
    private final UserRepository userRepo;

    @Transactional
    public UsageResponse createManual(UsageRequest r) {
        Household household = householdRepo.findById(r.householdId())
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + r.householdId()));
        return saveReading(household, r.readingDate(), r.meterReading(), ReadingSource.MANUAL);
    }

    @Transactional
    public UsageResponse saveReading(Household household, LocalDate date,
                                     BigDecimal meterReading, ReadingSource source) {
        if (logRepo.existsByHouseholdIdAndReadingDate(household.getId(), date))
            throw new DuplicateResourceException("Reading already exists for " + date);

        BigDecimal consumption = BigDecimal.ZERO;   // pehli reading = baseline
        WaterUsageLog latest = logRepo.findTopByHouseholdIdOrderByReadingDateDesc(household.getId()).orElse(null);

        if (latest != null) {
            if (!date.isAfter(latest.getReadingDate()))
                throw new BadRequestException("Reading date must be after the latest reading ("
                        + latest.getReadingDate() + ")");
            if (meterReading.compareTo(latest.getMeterReading()) < 0)
                throw new BadRequestException("Meter reading cannot be less than the previous reading ("
                        + latest.getMeterReading() + ")");
            consumption = meterReading.subtract(latest.getMeterReading());
        }

        WaterUsageLog log = new WaterUsageLog();
        log.setHousehold(household);
        log.setReadingDate(date);
        log.setMeterReading(meterReading);
        log.setConsumptionLitres(consumption);
        log.setSource(source);
        return toDto(logRepo.save(log));
    }

    @Transactional(readOnly = true)
    public List<UsageResponse> list(Long householdId, LocalDate from, LocalDate to, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (user.getRole() == Role.RESIDENT
                && (user.getHousehold() == null || !user.getHousehold().getId().equals(householdId)))
            throw new AccessDeniedException("You can only view your own household usage");

        if (!householdRepo.existsById(householdId))
            throw new ResourceNotFoundException("Household not found: " + householdId);

        LocalDate end = (to != null) ? to : LocalDate.now();
        LocalDate start = (from != null) ? from : end.minusDays(30);
        if (start.isAfter(end)) throw new BadRequestException("'from' must not be after 'to'");

        return logRepo.findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(householdId, start, end)
                .stream().map(this::toDto).toList();
    }

    private UsageResponse toDto(WaterUsageLog l) {
        return new UsageResponse(l.getId(), l.getHousehold().getId(), l.getReadingDate(),
                l.getMeterReading(), l.getConsumptionLitres(), l.getSource().name());
    }
}