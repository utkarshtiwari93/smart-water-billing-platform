package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.entity.TariffSlab;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class TariffCalculator {

    private static final BigDecimal LITRES_PER_KL = new BigDecimal("1000");

    public record SlabCharge(int slabNumber, BigDecimal fromKl, BigDecimal upToKl,
                             BigDecimal consumptionKl, BigDecimal rate, BigDecimal amount) {}

    public record Charge(BigDecimal consumptionKl, BigDecimal tier1Kl, BigDecimal tier2Kl,
                         BigDecimal tier1Charge, BigDecimal tier2Charge, BigDecimal total,
                         List<SlabCharge> slabs) {}

    public Charge calculate(BigDecimal consumptionLitres, TariffPlan plan) {
        if (consumptionLitres == null || consumptionLitres.signum() < 0)
            throw new IllegalArgumentException("Consumption must not be negative");

        BigDecimal kl = consumptionLitres.divide(LITRES_PER_KL, 4, RoundingMode.HALF_UP);
        List<TariffSlab> configuredSlabs = plan.getSlabs() == null ? List.of()
                : plan.getSlabs().stream().sorted(Comparator.comparingInt(TariffSlab::getOrder)).toList();
        if (configuredSlabs.isEmpty()) configuredSlabs = legacySlabs(plan);

        BigDecimal remaining = kl;
        BigDecimal from = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal firstKl = BigDecimal.ZERO;
        BigDecimal laterKl = BigDecimal.ZERO;
        BigDecimal firstCharge = BigDecimal.ZERO;
        BigDecimal laterCharge = BigDecimal.ZERO;
        List<SlabCharge> charges = new ArrayList<>();

        for (int i = 0; i < configuredSlabs.size(); i++) {
            TariffSlab slab = configuredSlabs.get(i);
            BigDecimal consumed;
            if (slab.getUpperLimitKl() == null) {
                consumed = remaining;
            } else {
                BigDecimal capacity = slab.getUpperLimitKl().subtract(from);
                consumed = remaining.min(capacity).max(BigDecimal.ZERO);
            }
            consumed = consumed.setScale(4, RoundingMode.HALF_UP);
            BigDecimal amount = consumed.multiply(slab.getRate()).setScale(2, RoundingMode.HALF_UP);
            charges.add(new SlabCharge(i + 1, from, slab.getUpperLimitKl(), consumed, slab.getRate(), amount));
            total = total.add(amount);
            if (i == 0) {
                firstKl = consumed;
                firstCharge = amount;
            } else {
                laterKl = laterKl.add(consumed);
                laterCharge = laterCharge.add(amount);
            }
            remaining = remaining.subtract(consumed).max(BigDecimal.ZERO);
            if (slab.getUpperLimitKl() != null) from = slab.getUpperLimitKl();
        }

        return new Charge(kl, firstKl, laterKl, firstCharge, laterCharge, total, List.copyOf(charges));
    }

    private List<TariffSlab> legacySlabs(TariffPlan plan) {
        TariffSlab first = new TariffSlab();
        first.setOrder(0);
        first.setUpperLimitKl(plan.getTier1LimitKl());
        first.setRate(plan.getBaseRate());
        TariffSlab second = new TariffSlab();
        second.setOrder(1);
        second.setRate(plan.getHigherRate());
        return List.of(first, second);
    }
}