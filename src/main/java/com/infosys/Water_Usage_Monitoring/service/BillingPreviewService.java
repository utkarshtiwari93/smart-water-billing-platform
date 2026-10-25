package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.BillPreviewResponse;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingPreviewService {

    private final BillingCycleService cycleService;
    private final HouseholdRepository householdRepo;
    private final WaterUsageLogRepository logRepo;
    private final UserRepository userRepo;
    private final TariffCalculator calculator;

    @Transactional(readOnly = true)
    public BillPreviewResponse preview(Long cycleId, Long householdId, String email) {
        BillingCycle cycle = cycleService.find(cycleId);
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        boolean resident = user.getRole() == Role.RESIDENT;
        Long targetId = householdId;

        if (targetId == null) {
            if (resident && user.getHousehold() != null) targetId = user.getHousehold().getId();
            else throw new BadRequestException("householdId is required");
        }

        if (resident && (user.getHousehold() == null || !user.getHousehold().getId().equals(targetId)))
            throw new AccessDeniedException("You can only view your own household bill");

        final Long id = targetId;
        Household household = householdRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + id));

        if (!household.getApartment().getId().equals(cycle.getApartment().getId()))
            throw new BadRequestException("Household does not belong to this billing cycle's apartment");

        return build(cycle, household);
    }

    @Transactional(readOnly = true)
    public List<BillPreviewResponse> previewAll(Long cycleId) {
        BillingCycle cycle = cycleService.find(cycleId);
        return householdRepo.findByApartmentId(cycle.getApartment().getId()).stream()
                .sorted(Comparator.comparing(Household::getFlatNumber))
                .map(h -> build(cycle, h))
                .toList();
    }

    private BillPreviewResponse build(BillingCycle cycle, Household h) {
        BigDecimal litres = logRepo.sumConsumption(h.getId(), cycle.getStartDate(), cycle.getEndDate());
        if (litres == null) litres = BigDecimal.ZERO;

        TariffPlan plan = cycle.getTariffPlan();
        TariffCalculator.Charge c = calculator.calculate(litres, plan);

        return new BillPreviewResponse(cycle.getId(), h.getId(), h.getFlatNumber(),
                cycle.getStartDate(), cycle.getEndDate(),
                litres, c.consumptionKl(),
                plan.getTier1LimitKl(), plan.getBaseRate(), plan.getHigherRate(),
                c.tier1Kl(), c.tier2Kl(), c.tier1Charge(), c.tier2Charge(), c.total());
    }
}