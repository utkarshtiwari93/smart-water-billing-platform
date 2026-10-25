package com.infosys.Water_Usage_Monitoring.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChargeDistributorTest {

    private final ChargeDistributor distributor = new ChargeDistributor();

    private ChargeDistributor.Input in(long id, boolean meter, String litres, String area) {
        return new ChargeDistributor.Input(id, meter, new BigDecimal(litres),
                area == null ? null : new BigDecimal(area));
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    private BigDecimal sum(List<ChargeDistributor.Share> shares) {
        return shares.stream().map(ChargeDistributor.Share::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    void meteredHouseholds_shareInProportionToConsumption() {
        List<ChargeDistributor.Share> r = distributor.distribute(new BigDecimal("300.00"),
                List.of(in(1, true, "14000", "1200"), in(2, true, "6000", "950")));

        assertMoney("210.00", r.get(0).amount());
        assertMoney("90.00", r.get(1).amount());
        assertEquals(ChargeDistributor.Basis.CONSUMPTION, r.get(0).basis());
    }

    @Test
    void sharesAlwaysAddUpToExactAmount_evenWhenNotDivisible() {
        List<ChargeDistributor.Share> r = distributor.distribute(new BigDecimal("100.00"),
                List.of(in(1, true, "1000", "100"), in(2, true, "1000", "100"), in(3, true, "1000", "100")));

        assertMoney("33.34", r.get(0).amount());
        assertMoney("33.33", r.get(1).amount());
        assertMoney("33.33", r.get(2).amount());
        assertMoney("100.00", sum(r));
    }

    @Test
    void unevenWeights_stillSumExactly() {
        List<ChargeDistributor.Share> r = distributor.distribute(new BigDecimal("100.01"),
                List.of(in(1, true, "1", "10"), in(2, true, "2", "10"), in(3, true, "3", "10"), in(4, true, "7", "10")));

        assertMoney("100.01", sum(r));
    }

    @Test
    void unmeteredHousehold_isEstimatedFromFlatArea() {
        // metered: 10000 L in 1000 sqft => 10 L/sqft; unmetered 500 sqft => weight 5000
        List<ChargeDistributor.Share> r = distributor.distribute(new BigDecimal("300.00"),
                List.of(in(1, true, "10000", "1000"), in(2, false, "0", "500")));

        assertMoney("200.00", r.get(0).amount());
        assertMoney("100.00", r.get(1).amount());
        assertEquals(ChargeDistributor.Basis.AREA_ESTIMATE, r.get(1).basis());
    }

    @Test
    void noConsumptionAnywhere_fallsBackToFlatArea() {
        List<ChargeDistributor.Share> r = distributor.distribute(new BigDecimal("90.00"),
                List.of(in(1, true, "0", "1000"), in(2, false, "0", "500")));

        assertMoney("60.00", r.get(0).amount());
        assertMoney("30.00", r.get(1).amount());
        assertEquals(ChargeDistributor.Basis.AREA, r.get(0).basis());
    }

    @Test
    void noConsumptionAndNoArea_splitsEqually() {
        List<ChargeDistributor.Share> r = distributor.distribute(new BigDecimal("90.00"),
                List.of(in(1, false, "0", null), in(2, false, "0", null)));

        assertMoney("45.00", r.get(0).amount());
        assertMoney("45.00", r.get(1).amount());
        assertEquals(ChargeDistributor.Basis.EQUAL, r.get(0).basis());
    }

    @Test
    void zeroSharedCost_allocatesNothing() {
        List<ChargeDistributor.Share> r = distributor.distribute(BigDecimal.ZERO,
                List.of(in(1, true, "5000", "100"), in(2, true, "5000", "100")));

        assertMoney("0.00", r.get(0).amount());
        assertMoney("0.00", sum(r));
    }

    @Test
    void emptyList_returnsEmpty() {
        assertTrue(distributor.distribute(new BigDecimal("100"), List.of()).isEmpty());
    }
}