package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TariffCalculator {

    private static final BigDecimal LITRES_PER_KL = new BigDecimal("1000");

    public record Charge(BigDecimal consumptionKl, BigDecimal tier1Kl, BigDecimal tier2Kl,
                         BigDecimal tier1Charge, BigDecimal tier2Charge, BigDecimal total) {}

    public Charge calculate(BigDecimal consumptionLitres, TariffPlan plan) {
        if (consumptionLitres == null || consumptionLitres.signum() < 0)
            throw new IllegalArgumentException("Consumption must not be negative");

        BigDecimal kl = consumptionLitres.divide(LITRES_PER_KL, 4, RoundingMode.HALF_UP);
        BigDecimal limit = plan.getTier1LimitKl();

        BigDecimal tier1 = kl.min(limit).setScale(4, RoundingMode.HALF_UP);
        BigDecimal tier2 = kl.subtract(limit).max(BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);

        BigDecimal tier1Charge = tier1.multiply(plan.getBaseRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tier2Charge = tier2.multiply(plan.getHigherRate()).setScale(2, RoundingMode.HALF_UP);

        return new Charge(kl, tier1, tier2, tier1Charge, tier2Charge, tier1Charge.add(tier2Charge));
    }
}