package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.BulkPurchaseRequest;
import com.infosys.Water_Usage_Monitoring.dto.BulkPurchaseResponse;
import com.infosys.Water_Usage_Monitoring.dto.PurchaseSummaryResponse;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.BulkPurchase;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.BulkPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BulkPurchaseService {

    private final BulkPurchaseRepository repo;
    private final BillingCycleService cycleService;

    @Transactional
    public BulkPurchaseResponse create(Long cycleId, BulkPurchaseRequest r) {
        BillingCycle cycle = cycleService.find(cycleId);
        requireOpen(cycle);
        checkDateWithinCycle(cycle, r.purchaseDate());

        BulkPurchase p = new BulkPurchase();
        p.setBillingCycle(cycle);
        apply(p, r);
        return toDto(repo.save(p));
    }

    @Transactional(readOnly = true)
    public List<BulkPurchaseResponse> list(Long cycleId) {
        cycleService.find(cycleId);
        return repo.findByBillingCycleIdOrderByPurchaseDateAscIdAsc(cycleId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public BulkPurchaseResponse get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public BulkPurchaseResponse update(Long id, BulkPurchaseRequest r) {
        BulkPurchase p = find(id);
        BillingCycle cycle = p.getBillingCycle();
        requireOpen(cycle);
        checkDateWithinCycle(cycle, r.purchaseDate());
        apply(p, r);
        return toDto(p);
    }

    @Transactional
    public void delete(Long id) {
        BulkPurchase p = find(id);
        requireOpen(p.getBillingCycle());
        repo.delete(p);
    }

    @Transactional(readOnly = true)
    public PurchaseSummaryResponse summary(Long cycleId) {
        cycleService.find(cycleId);
        List<BulkPurchase> purchases = repo.findByBillingCycleIdOrderByPurchaseDateAscIdAsc(cycleId);

        BigDecimal totalVolume = purchases.stream()
                .map(BulkPurchase::getVolumeKl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCost = purchases.stream()
                .map(BulkPurchase::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal average = totalVolume.signum() == 0
                ? BigDecimal.ZERO.setScale(2)
                : totalCost.divide(totalVolume, 2, RoundingMode.HALF_UP);

        return new PurchaseSummaryResponse(cycleId, purchases.size(), totalVolume, totalCost, average);
    }

    private BulkPurchase find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bulk purchase not found: " + id));
    }

    private void requireOpen(BillingCycle cycle) {
        if (cycle.getStatus() != CycleStatus.OPEN)
            throw new ConflictException("Purchases can only be changed in an OPEN billing cycle (current status: "
                    + cycle.getStatus() + ")");
    }

    private void checkDateWithinCycle(BillingCycle cycle, LocalDate date) {
        if (date.isBefore(cycle.getStartDate()) || date.isAfter(cycle.getEndDate()))
            throw new BadRequestException("purchaseDate must be within the billing cycle ("
                    + cycle.getStartDate() + " to " + cycle.getEndDate() + ")");
    }

    private void apply(BulkPurchase p, BulkPurchaseRequest r) {
        p.setPurchaseDate(r.purchaseDate());
        p.setSource(r.source());
        p.setSupplier(r.supplier() == null || r.supplier().isBlank() ? null : r.supplier().trim());
        p.setVolumeKl(r.volumeKl());
        p.setUnitCost(r.unitCost());
        p.setTotalCost(r.volumeKl().multiply(r.unitCost()).setScale(2, RoundingMode.HALF_UP));
        p.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
    }

    private BulkPurchaseResponse toDto(BulkPurchase p) {
        return new BulkPurchaseResponse(p.getId(), p.getBillingCycle().getId(), p.getPurchaseDate(),
                p.getSource().name(), p.getSupplier(), p.getVolumeKl(), p.getUnitCost(),
                p.getTotalCost(), p.getNotes());
    }
}