package com.infosys.Water_Usage_Monitoring.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class ChargeDistributor {

    public enum Basis { CONSUMPTION, AREA_ESTIMATE, AREA, EQUAL }

    public record Input(Long householdId, boolean hasMeter, BigDecimal litres, BigDecimal areaSqft) {}

    public record Share(Long householdId, Basis basis, BigDecimal weight, BigDecimal amount) {}

    public List<Share> distribute(BigDecimal sharedCost, List<Input> inputs) {
        if (inputs.isEmpty()) return List.of();

        List<BigDecimal> weights = new ArrayList<>();
        List<Basis> bases = new ArrayList<>();
        buildWeights(inputs, weights, bases);

        List<BigDecimal> amounts = allocate(sharedCost, weights);

        List<Share> result = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            result.add(new Share(inputs.get(i).householdId(), bases.get(i), weights.get(i), amounts.get(i)));
        }
        return result;
    }

    private void buildWeights(List<Input> inputs, List<BigDecimal> weights, List<Basis> bases) {
        BigDecimal meteredLitres = BigDecimal.ZERO;
        BigDecimal meteredArea = BigDecimal.ZERO;
        int meteredCount = 0;

        for (Input in : inputs) {
            if (!in.hasMeter()) continue;
            meteredCount++;
            meteredLitres = meteredLitres.add(nz(in.litres()));
            if (positive(in.areaSqft())) meteredArea = meteredArea.add(in.areaSqft());
        }

        if (meteredLitres.signum() > 0) {
            // consumption mode
            BigDecimal avgLitres = meteredLitres.divide(BigDecimal.valueOf(meteredCount), 6, RoundingMode.HALF_UP);
            BigDecimal litresPerSqft = meteredArea.signum() > 0
                    ? meteredLitres.divide(meteredArea, 10, RoundingMode.HALF_UP) : null;

            for (Input in : inputs) {
                if (in.hasMeter()) {
                    weights.add(nz(in.litres()));
                    bases.add(Basis.CONSUMPTION);
                } else if (positive(in.areaSqft()) && litresPerSqft != null) {
                    weights.add(in.areaSqft().multiply(litresPerSqft));
                    bases.add(Basis.AREA_ESTIMATE);
                } else {
                    weights.add(avgLitres);
                    bases.add(Basis.AREA_ESTIMATE);
                }
            }
            return;
        }

        boolean anyArea = inputs.stream().anyMatch(in -> positive(in.areaSqft()));
        for (Input in : inputs) {
            if (anyArea) {
                weights.add(positive(in.areaSqft()) ? in.areaSqft() : BigDecimal.ZERO);
                bases.add(Basis.AREA);
            } else {
                weights.add(BigDecimal.ONE);
                bases.add(Basis.EQUAL);
            }
        }
    }

    private List<BigDecimal> allocate(BigDecimal amount, List<BigDecimal> weights) {
        int n = weights.size();
        BigDecimal rounded = amount == null ? BigDecimal.ZERO : amount.setScale(2, RoundingMode.HALF_UP);

        if (rounded.signum() <= 0) {
            List<BigDecimal> zeros = new ArrayList<>();
            for (int i = 0; i < n; i++) zeros.add(BigDecimal.ZERO.setScale(2));
            return zeros;
        }

        BigDecimal total = weights.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> w = weights;
        if (total.signum() == 0) {
            w = new ArrayList<>();
            for (int i = 0; i < n; i++) w.add(BigDecimal.ONE);
            total = BigDecimal.valueOf(n);
        }

        long totalCents = rounded.movePointRight(2).longValueExact();
        long[] cents = new long[n];
        BigDecimal[] fraction = new BigDecimal[n];
        long assigned = 0;

        for (int i = 0; i < n; i++) {
            BigDecimal exact = BigDecimal.valueOf(totalCents).multiply(w.get(i))
                    .divide(total, 10, RoundingMode.HALF_UP);
            cents[i] = exact.setScale(0, RoundingMode.DOWN).longValue();
            fraction[i] = exact.subtract(BigDecimal.valueOf(cents[i]));
            assigned += cents[i];
        }

        long remaining = totalCents - assigned;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        order.sort(Comparator.<Integer, BigDecimal>comparing(i -> fraction[i]).reversed()
                .thenComparing(Comparator.naturalOrder()));

        for (int k = 0; k < remaining && k < n; k++) cents[order.get(k)]++;

        List<BigDecimal> result = new ArrayList<>();
        for (int i = 0; i < n; i++) result.add(BigDecimal.valueOf(cents[i], 2));
        return result;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private boolean positive(BigDecimal v) {
        return v != null && v.signum() > 0;
    }
}