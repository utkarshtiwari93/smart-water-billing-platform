package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.DistributionResponse;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdShareResponse;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.BulkPurchase;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.repository.BulkPurchaseRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DistributionService {

    private final BillingCycleService cycleService;
    private final BulkPurchaseRepository purchaseRepo;
    private final HouseholdRepository householdRepo;
    private final WaterUsageLogRepository logRepo;
    private final TariffCalculator calculator;
    private final ChargeDistributor distributor;

    private record Row(Household household, boolean hasMeter, BigDecimal litres, BigDecimal waterCharge) {}

    @Transactional(readOnly = true)
    public DistributionResponse calculate(Long cycleId) {
        BillingCycle cycle = cycleService.find(cycleId);
        TariffPlan plan = cycle.getTariffPlan();

        BigDecimal pool = purchaseRepo.findByBillingCycleIdOrderByPurchaseDateAscIdAsc(cycleId).stream()
                .map(BulkPurchase::getTotalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Household> households = householdRepo.findByApartmentId(cycle.getApartment().getId()).stream()
                .sorted(Comparator.comparing(Household::getFlatNumber))
                .toList();

        List<Row> rows = new ArrayList<>();
        BigDecimal totalWater = BigDecimal.ZERO;
        for (Household h : households) {
            boolean hasMeter = h.getMeterNumber() != null && !h.getMeterNumber().isBlank();
            BigDecimal litres = BigDecimal.ZERO;
            BigDecimal water = BigDecimal.ZERO.setScale(2);
            if (hasMeter) {
                BigDecimal sum = logRepo.sumConsumption(h.getId(), cycle.getStartDate(), cycle.getEndDate());
                litres = sum == null ? BigDecimal.ZERO : sum;
                water = calculator.calculate(litres, plan).total();
            }
            rows.add(new Row(h, hasMeter, litres, water));
            totalWater = totalWater.add(water);
        }

        BigDecimal shared = BigDecimal.ZERO.setScale(2);
        BigDecimal surplus = BigDecimal.ZERO.setScale(2);
        String note = null;

        if (pool.signum() == 0) {
            note = "No bulk purchases recorded for this cycle, so there is no shared cost to distribute";
        } else if (pool.compareTo(totalWater) >= 0) {
            shared = pool.subtract(totalWater);
        } else {
            surplus = totalWater.subtract(pool);
            note = "Water charges exceed the purchase cost; no shared cost to distribute";
        }

        List<ChargeDistributor.Input> inputs = rows.stream()
                .map(r -> new ChargeDistributor.Input(r.household().getId(), r.hasMeter(), r.litres(),
                        r.household().getAreaSqft()))
                .toList();
        List<ChargeDistributor.Share> shares = distributor.distribute(shared, inputs);

        List<HouseholdShareResponse> out = new ArrayList<>();
        BigDecimal totalBilled = BigDecimal.ZERO;
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            ChargeDistributor.Share s = shares.get(i);
            BigDecimal total = r.waterCharge().add(s.amount());
            totalBilled = totalBilled.add(total);
            out.add(new HouseholdShareResponse(r.household().getId(), r.household().getFlatNumber(),
                    r.hasMeter(), r.litres(), r.waterCharge(), s.basis().name(), s.amount(), total));
        }

        return new DistributionResponse(cycleId, pool, totalWater, shared, surplus, totalBilled, note, out);
    }
}