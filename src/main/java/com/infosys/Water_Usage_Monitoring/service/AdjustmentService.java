package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.AdjustmentRequest;
import com.infosys.Water_Usage_Monitoring.dto.AdjustmentResponse;
import com.infosys.Water_Usage_Monitoring.entity.Adjustment;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.AdjustmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdjustmentService {

    private final AdjustmentRepository repo;
    private final BillingCycleService cycleService;
    private final HouseholdRepository householdRepo;

    @Transactional
    public AdjustmentResponse create(Long cycleId, AdjustmentRequest r, String adminEmail) {
        BillingCycle cycle = cycleService.find(cycleId);
        requireOpen(cycle);

        if (r.amount().signum() == 0)
            throw new BadRequestException("amount must not be zero (use a negative value for a credit)");

        Household household = householdRepo.findById(r.householdId())
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + r.householdId()));
        if (!household.getApartment().getId().equals(cycle.getApartment().getId()))
            throw new BadRequestException("Household does not belong to this billing cycle's apartment");

        Adjustment a = new Adjustment();
        a.setBillingCycle(cycle);
        a.setHousehold(household);
        a.setAmount(r.amount());
        a.setReason(r.reason().trim());
        a.setCreatedBy(adminEmail);
        return toDto(repo.save(a));
    }

    @Transactional(readOnly = true)
    public List<AdjustmentResponse> list(Long cycleId) {
        cycleService.find(cycleId);
        return repo.findByBillingCycleIdOrderByIdAsc(cycleId).stream().map(this::toDto).toList();
    }

    @Transactional
    public void delete(Long id) {
        Adjustment a = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found: " + id));
        requireOpen(a.getBillingCycle());
        repo.delete(a);
    }

    private void requireOpen(BillingCycle cycle) {
        if (cycle.getStatus() != CycleStatus.OPEN)
            throw new ConflictException("Adjustments can only be changed in an OPEN billing cycle (current status: "
                    + cycle.getStatus() + ")");
    }

    private AdjustmentResponse toDto(Adjustment a) {
        return new AdjustmentResponse(a.getId(), a.getBillingCycle().getId(), a.getHousehold().getId(),
                a.getHousehold().getFlatNumber(), a.getAmount(), a.getReason(), a.getCreatedBy());
    }
}