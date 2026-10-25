package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TariffCalculatorTest {

    private final TariffCalculator calculator = new TariffCalculator();

    private TariffPlan plan(String limit, String base, String higher) {
        TariffPlan p = new TariffPlan();
        p.setTier1LimitKl(new BigDecimal(limit));
        p.setBaseRate(new BigDecimal(base));
        p.setHigherRate(new BigDecimal(higher));
        return p;
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void belowLimit_chargesOnlyBaseRate() {
        TariffCalculator.Charge c = calculator.calculate(new BigDecimal("8000"), plan("10", "10", "20"));

        assertMoney("8", c.tier1Kl());
        assertMoney("0", c.tier2Kl());
        assertMoney("80.00", c.total());
    }

    @Test
    void aboveLimit_splitsAcrossBothTiers() {
        TariffCalculator.Charge c = calculator.calculate(new BigDecimal("14000"), plan("10", "10", "20"));

        assertMoney("10", c.tier1Kl());
        assertMoney("4", c.tier2Kl());
        assertMoney("100.00", c.tier1Charge());
        assertMoney("80.00", c.tier2Charge());
        assertMoney("180.00", c.total());
    }

    @Test
    void exactlyAtLimit_hasNoHigherTierCharge() {
        TariffCalculator.Charge c = calculator.calculate(new BigDecimal("10000"), plan("10", "10", "20"));

        assertMoney("0", c.tier2Kl());
        assertMoney("100.00", c.total());
    }

    @Test
    void zeroConsumption_costsNothing() {
        assertMoney("0.00", calculator.calculate(BigDecimal.ZERO, plan("10", "10", "20")).total());
    }

    @Test
    void largeConsumption_scalesHigherTier() {
        assertMoney("300.00", calculator.calculate(new BigDecimal("20000"), plan("10", "10", "20")).total());
    }

    @Test
    void fractionalKilolitres_arePricedExactly() {
        // 12.345 kL = 10 kL x 10 + 2.345 kL x 20 = 100.00 + 46.90
        TariffCalculator.Charge c = calculator.calculate(new BigDecimal("12345"), plan("10", "10", "20"));

        assertMoney("46.90", c.tier2Charge());
        assertMoney("146.90", c.total());
    }

    @Test
    void charges_areRoundedHalfUpToTwoDecimals() {
        // 0.333 kL x 3.33 = 1.10889 -> 1.11
        TariffCalculator.Charge c = calculator.calculate(new BigDecimal("333"), plan("10", "3.33", "6.66"));

        assertMoney("1.11", c.total());
    }

    @Test
    void negativeConsumption_isRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculate(new BigDecimal("-1"), plan("10", "10", "20")));
    }
}