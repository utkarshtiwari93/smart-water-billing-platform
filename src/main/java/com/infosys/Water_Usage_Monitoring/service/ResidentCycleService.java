package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.ResidentCycleSummary;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.BillingCycleRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResidentCycleService {

    private final UserRepository userRepository;
    private final BillingCycleRepository cycleRepository;
    private final ConsumptionAggregationService aggregationService;
    private final TariffCalculator tariffCalculator;

    @Transactional(readOnly = true)
    public ResidentCycleSummary current(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        if (user.getRole() != Role.RESIDENT || user.getHousehold() == null)
            throw new BadRequestException("You are not assigned to a household");

        LocalDate today = LocalDate.now();
        List<BillingCycle> cycles = cycleRepository
                .findByApartmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        user.getApartment().getId(), today, today);
        if (cycles.isEmpty()) return null;

        BillingCycle cycle = cycles.get(0);
        ConsumptionAggregationService.Aggregate aggregate = aggregationService.household(
                user.getHousehold().getId(), cycle.getStartDate(), today,
                ConsumptionAggregationService.Granularity.DAY);
        BigDecimal consumption = aggregate.totalLitres();
        BigDecimal estimatedWaterCharge = null;
        if (aggregate.coveredDays() >= 7 && cycle.getTariffPlan() != null) {
            long cycleDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate()) + 1;
            BigDecimal projectedConsumption = consumption
                    .divide(BigDecimal.valueOf(aggregate.coveredDays()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(cycleDays))
                    .setScale(2, RoundingMode.HALF_UP);
            estimatedWaterCharge = tariffCalculator.calculate(projectedConsumption, cycle.getTariffPlan()).total();
        }
        return new ResidentCycleSummary(cycle.getId(), cycle.getStartDate(), cycle.getEndDate(),
                cycle.getStatus().name(), consumption, aggregate.coveredDays(),
                aggregate.latestReadingDate(), estimatedWaterCharge);
    }
}
