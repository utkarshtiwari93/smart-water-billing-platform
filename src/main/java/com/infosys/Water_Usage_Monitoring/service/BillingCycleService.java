package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.BillingCycleRequest;
import com.infosys.Water_Usage_Monitoring.dto.BillingCycleResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingCycleService {

    private final BillingCycleRepository cycleRepo;
    private final TariffPlanRepository planRepo;
    private final TariffService tariffService;
    private final ApartmentService apartmentService;

    @Transactional
    public BillingCycleResponse create(Long apartmentId, BillingCycleRequest r) {
        checkDates(r.startDate(), r.endDate());
        Apartment apartment = apartmentService.find(apartmentId);

        if (cycleRepo.existsByApartmentIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                apartmentId, r.endDate(), r.startDate()))
            throw new ConflictException("Billing cycle overlaps with an existing cycle of this apartment");

        TariffPlan plan = resolvePlan(apartmentId, r.tariffPlanId(), r.startDate());

        BillingCycle cycle = new BillingCycle();
        cycle.setApartment(apartment);
        cycle.setTariffPlan(plan);
        cycle.setStartDate(r.startDate());
        cycle.setEndDate(r.endDate());
        cycle.setStatus(CycleStatus.OPEN);
        return toDto(cycleRepo.save(cycle));
    }

    @Transactional(readOnly = true)
    public List<BillingCycleResponse> list(Long apartmentId, CycleStatus status) {
        apartmentService.find(apartmentId);
        List<BillingCycle> cycles = (status == null)
                ? cycleRepo.findByApartmentIdOrderByStartDateDesc(apartmentId)
                : cycleRepo.findByApartmentIdAndStatusOrderByStartDateDesc(apartmentId, status);
        return cycles.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BillingCycleResponse get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public BillingCycleResponse update(Long id, BillingCycleRequest r) {
        BillingCycle cycle = find(id);
        requireOpen(cycle);
        checkDates(r.startDate(), r.endDate());

        Long apartmentId = cycle.getApartment().getId();
        if (cycleRepo.existsByApartmentIdAndIdNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                apartmentId, id, r.endDate(), r.startDate()))
            throw new ConflictException("Billing cycle overlaps with an existing cycle of this apartment");

        TariffPlan plan = (r.tariffPlanId() == null)
                ? cycle.getTariffPlan()
                : resolvePlan(apartmentId, r.tariffPlanId(), r.startDate());

        cycle.setTariffPlan(plan);
        cycle.setStartDate(r.startDate());
        cycle.setEndDate(r.endDate());
        return toDto(cycle);
    }

    @Transactional
    public void delete(Long id) {
        BillingCycle cycle = find(id);
        requireOpen(cycle);
        cycleRepo.delete(cycle);
    }

    @Transactional(readOnly = true)
    public BillingCycle find(Long id) {
        return cycleRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Billing cycle not found: " + id));
    }

    private void checkDates(LocalDate start, LocalDate end) {
        if (end.isBefore(start))
            throw new BadRequestException("endDate must not be before startDate");
    }

    private void requireOpen(BillingCycle cycle) {
        if (cycle.getStatus() != CycleStatus.OPEN)
            throw new ConflictException("Only OPEN billing cycles can be modified (current status: "
                    + cycle.getStatus() + ")");
    }

    private TariffPlan resolvePlan(Long apartmentId, Long tariffPlanId, LocalDate startDate) {
        if (tariffPlanId == null) {
            return planRepo
                    .findFirstByApartmentIdAndActiveTrueAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
                            apartmentId, startDate)
                    .orElseThrow(() -> new BadRequestException(
                            "No active tariff plan is effective on " + startDate + ". Create one first."));
        }

        TariffPlan plan = tariffService.find(tariffPlanId);
        if (!plan.getApartment().getId().equals(apartmentId))
            throw new BadRequestException("Tariff plan belongs to a different apartment");
        if (!plan.isActive())
            throw new BadRequestException("Tariff plan is not active");
        if (plan.getEffectiveFrom().isAfter(startDate))
            throw new BadRequestException("Tariff plan is not effective on the cycle's start date");
        return plan;
    }

    private BillingCycleResponse toDto(BillingCycle c) {
        return new BillingCycleResponse(c.getId(), c.getApartment().getId(),
                c.getTariffPlan().getId(), c.getTariffPlan().getName(),
                c.getStartDate(), c.getEndDate(), c.getStatus().name());
    }
}