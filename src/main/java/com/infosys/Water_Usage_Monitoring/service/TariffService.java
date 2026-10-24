package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.TariffPlanRequest;
import com.infosys.Water_Usage_Monitoring.dto.TariffPlanResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.BillingCycleRepository;
import com.infosys.Water_Usage_Monitoring.repository.TariffPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffPlanRepository repo;
    private final BillingCycleRepository cycleRepo;
    private final ApartmentService apartmentService;

    @Transactional
    public TariffPlanResponse create(Long apartmentId, TariffPlanRequest r) {
        Apartment apartment = apartmentService.find(apartmentId);
        validate(r);

        TariffPlan plan = new TariffPlan();
        plan.setApartment(apartment);
        apply(plan, r);
        return toDto(repo.save(plan));
    }

    @Transactional(readOnly = true)
    public List<TariffPlanResponse> list(Long apartmentId) {
        apartmentService.find(apartmentId);
        return repo.findByApartmentIdOrderByEffectiveFromDesc(apartmentId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TariffPlanResponse get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public TariffPlanResponse update(Long id, TariffPlanRequest r) {
        TariffPlan plan = find(id);
        validate(r);

        boolean ratesChanged = plan.getTier1LimitKl().compareTo(r.tier1LimitKl()) != 0
                || plan.getBaseRate().compareTo(r.baseRate()) != 0
                || plan.getHigherRate().compareTo(r.higherRate()) != 0;

        if (ratesChanged && cycleRepo.existsByTariffPlanIdAndStatusIn(
                id, List.of(CycleStatus.FINALIZED, CycleStatus.ARCHIVED)))
            throw new ConflictException(
                    "Rates cannot be changed: this plan is used by a finalized or archived billing cycle");

        apply(plan, r);
        return toDto(plan);
    }

    @Transactional
    public void delete(Long id) {
        TariffPlan plan = find(id);
        if (cycleRepo.existsByTariffPlanId(id))
            throw new ConflictException("Tariff plan is used by a billing cycle and cannot be deleted");
        repo.delete(plan);
    }

    @Transactional(readOnly = true)
    public TariffPlan find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff plan not found: " + id));
    }

    private void validate(TariffPlanRequest r) {
        if (r.higherRate().compareTo(r.baseRate()) < 0)
            throw new BadRequestException("higherRate must be greater than or equal to baseRate");
    }

    private void apply(TariffPlan plan, TariffPlanRequest r) {
        plan.setName(r.name().trim());
        plan.setTier1LimitKl(r.tier1LimitKl());
        plan.setBaseRate(r.baseRate());
        plan.setHigherRate(r.higherRate());
        plan.setEffectiveFrom(r.effectiveFrom());
        plan.setActive(r.active() == null || r.active());
    }

    private TariffPlanResponse toDto(TariffPlan t) {
        return new TariffPlanResponse(t.getId(), t.getApartment().getId(), t.getName(),
                t.getTier1LimitKl(), t.getBaseRate(), t.getHigherRate(),
                t.getEffectiveFrom(), t.isActive());
    }
}