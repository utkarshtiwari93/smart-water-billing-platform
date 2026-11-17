package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.TariffPlanRequest;
import com.infosys.Water_Usage_Monitoring.dto.TariffPlanResponse;
import com.infosys.Water_Usage_Monitoring.dto.TariffSlabRequest;
import com.infosys.Water_Usage_Monitoring.dto.TariffSlabResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.entity.TariffSlab;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.BillingCycleRepository;
import com.infosys.Water_Usage_Monitoring.repository.TariffPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TariffService {

    private final TariffPlanRepository repo;
    private final BillingCycleRepository cycleRepo;
    private final ApartmentService apartmentService;

    @Transactional
    public TariffPlanResponse create(Long apartmentId, TariffPlanRequest r) {
        Apartment apartment = apartmentService.find(apartmentId);
        validate(r, apartmentId, null);

        TariffPlan plan = new TariffPlan();
        plan.setApartment(apartment);
        apply(plan, r, normalizedSlabs(r));
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
        validate(r, plan.getApartment().getId(), id);

        List<TariffSlabRequest> requestedSlabs = requestedSlabsForUpdate(r, plan);
        boolean ratesChanged = !sameSlabs(slabsFor(plan), requestedSlabs);

        if (ratesChanged && cycleRepo.existsByTariffPlanIdAndStatusIn(
                id, List.of(CycleStatus.FINALIZED, CycleStatus.ARCHIVED)))
            throw new ConflictException(
                    "Rates cannot be changed: this plan is used by a finalized or archived billing cycle");

        apply(plan, r, requestedSlabs);
        return toDto(plan);
    }

    @Transactional
    public TariffPlanResponse setActive(Long id, boolean active) {
        TariffPlan plan = find(id);
        if (active && repo.existsByApartmentIdAndIdNotAndEffectiveFromAndActiveTrue(
                plan.getApartment().getId(), id, plan.getEffectiveFrom())) {
            throw new ConflictException("Another active tariff plan already has this effective date");
        }
        plan.setActive(active);
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

    private void validate(TariffPlanRequest r, Long apartmentId, Long excludedPlanId) {
        List<TariffSlabRequest> slabs = normalizedSlabs(r);
        if (slabs.isEmpty())
            throw new BadRequestException("At least one tariff slab is required");

        BigDecimal previousLimit = BigDecimal.ZERO;
        BigDecimal previousRate = null;
        for (int i = 0; i < slabs.size(); i++) {
            TariffSlabRequest slab = slabs.get(i);
            if (slab == null || slab.rate() == null || slab.rate().signum() < 0)
                throw new BadRequestException("Tariff slab rates cannot be negative or missing");
            if (previousRate != null && slab.rate().compareTo(previousRate) < 0)
                throw new BadRequestException("Progressive tariff rates cannot decrease");
            boolean last = i == slabs.size() - 1;
            if (slab.upToKl() == null) {
                if (!last)
                    throw new BadRequestException("Only the final tariff slab can be open-ended");
            } else {
                if (slab.upToKl().compareTo(previousLimit) <= 0)
                    throw new BadRequestException("Tariff slab upper limits must increase");
                if (last)
                    throw new BadRequestException("The final tariff slab must be open-ended");
                previousLimit = slab.upToKl();
            }
            previousRate = slab.rate();
        }
        boolean active = r.active() == null || r.active();
        if (active) {
            boolean duplicateDate = excludedPlanId == null
                    ? repo.existsByApartmentIdAndEffectiveFromAndActiveTrue(apartmentId, r.effectiveFrom())
                    : repo.existsByApartmentIdAndIdNotAndEffectiveFromAndActiveTrue(
                            apartmentId, excludedPlanId, r.effectiveFrom());
            if (duplicateDate)
                throw new ConflictException("An active tariff plan already has this effective date");
        }
    }

    private void apply(TariffPlan plan, TariffPlanRequest r, List<TariffSlabRequest> slabs) {
        plan.setName(r.name().trim());
        TariffSlabRequest first = slabs.get(0);
        TariffSlabRequest second = slabs.size() > 1 ? slabs.get(1) : first;
        plan.setTier1LimitKl(first.upToKl() == null ? BigDecimal.ZERO : first.upToKl());
        plan.setBaseRate(first.rate());
        plan.setHigherRate(second.rate());
        plan.setEffectiveFrom(r.effectiveFrom());
        if (r.active() != null) plan.setActive(r.active());

        plan.getSlabs().clear();
        for (int i = 0; i < slabs.size(); i++) {
            TariffSlabRequest request = slabs.get(i);
            TariffSlab slab = new TariffSlab();
            slab.setTariffPlan(plan);
            slab.setOrder(i);
            slab.setUpperLimitKl(request.upToKl());
            slab.setRate(request.rate());
            plan.getSlabs().add(slab);
        }
    }

    private TariffPlanResponse toDto(TariffPlan t) {
        List<TariffSlab> slabs = slabsFor(t);
        List<TariffSlabResponse> slabResponses = new java.util.ArrayList<>();
        BigDecimal from = BigDecimal.ZERO;
        for (int i = 0; i < slabs.size(); i++) {
            TariffSlab slab = slabs.get(i);
            slabResponses.add(new TariffSlabResponse(i + 1, from, slab.getUpperLimitKl(), slab.getRate()));
            if (slab.getUpperLimitKl() != null) from = slab.getUpperLimitKl();
        }
        return new TariffPlanResponse(t.getId(), t.getApartment().getId(), t.getName(),
                t.getTier1LimitKl(), t.getBaseRate(), t.getHigherRate(),
                t.getEffectiveFrom(), t.isActive(), slabResponses);
    }

    private List<TariffSlabRequest> normalizedSlabs(TariffPlanRequest request) {
        if (request.slabs() != null && !request.slabs().isEmpty())
            return request.slabs();
        if (request.tier1LimitKl() == null || request.baseRate() == null || request.higherRate() == null)
            return List.of();
        return List.of(new TariffSlabRequest(request.tier1LimitKl(), request.baseRate()),
                new TariffSlabRequest(null, request.higherRate()));
    }

    private List<TariffSlabRequest> requestedSlabsForUpdate(TariffPlanRequest request, TariffPlan plan) {
        if ((request.slabs() == null || request.slabs().isEmpty())
                && same(request.tier1LimitKl(), plan.getTier1LimitKl())
                && same(request.baseRate(), plan.getBaseRate())
                && same(request.higherRate(), plan.getHigherRate())) {
            return slabsFor(plan).stream()
                    .map(slab -> new TariffSlabRequest(slab.getUpperLimitKl(), slab.getRate()))
                    .toList();
        }
        return normalizedSlabs(request);
    }

    private boolean same(BigDecimal first, BigDecimal second) {
        return first != null && second != null && first.compareTo(second) == 0;
    }

    private List<TariffSlab> slabsFor(TariffPlan plan) {
        if (plan.getSlabs() != null && !plan.getSlabs().isEmpty())
            return plan.getSlabs().stream().sorted(java.util.Comparator.comparingInt(TariffSlab::getOrder)).toList();
        TariffSlab first = new TariffSlab();
        first.setOrder(0);
        first.setUpperLimitKl(plan.getTier1LimitKl());
        first.setRate(plan.getBaseRate());
        TariffSlab second = new TariffSlab();
        second.setOrder(1);
        second.setRate(plan.getHigherRate());
        return List.of(first, second);
    }

    private boolean sameSlabs(List<TariffSlab> current, List<TariffSlabRequest> requested) {
        if (current.size() != requested.size()) return false;
        for (int i = 0; i < current.size(); i++) {
            TariffSlab existing = current.get(i);
            TariffSlabRequest next = requested.get(i);
            if (!Objects.equals(existing.getUpperLimitKl(), next.upToKl())
                    || existing.getRate().compareTo(next.rate()) != 0) return false;
        }
        return true;
    }
}