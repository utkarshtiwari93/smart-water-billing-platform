package com.infosys.Water_Usage_Monitoring.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AnomalyDetector {

    public static final int MIN_HISTORY = 5;
    public static final double SIGMA = 2.0;
    private static final double MIN_BAND = 0.05;

    public record Verdict(boolean leak, boolean overuse, double mean, double limit) {}


    public Verdict analyze(BigDecimal current, List<BigDecimal> history,
                           BigDecimal threshold, boolean leakEnabled) {

        boolean overuse = threshold != null
                && current.compareTo(threshold) > 0;

        boolean leak = false;
        double mean = 0;
        double limit = 0;

        if (leakEnabled && history.size() >= MIN_HISTORY) {

            mean = history.stream()
                    .mapToDouble(BigDecimal::doubleValue)
                    .average()
                    .orElse(0);

            final double historyMean = mean;

            double variance = history.stream()
                    .mapToDouble(h -> Math.pow(
                            h.doubleValue() - historyMean, 2))
                    .average()
                    .orElse(0);

            double sigma = Math.max(
                    Math.sqrt(variance),
                    mean * MIN_BAND
            );

            limit = mean + SIGMA * sigma;
            leak = current.doubleValue() > limit;
        }

        return new Verdict(leak, overuse, mean, limit);
    }

}