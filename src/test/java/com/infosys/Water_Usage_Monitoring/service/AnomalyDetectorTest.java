package com.infosys.Water_Usage_Monitoring.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnomalyDetectorTest {

    private final AnomalyDetector detector = new AnomalyDetector();

    private List<BigDecimal> history(String... values) {
        return Stream.of(values).map(BigDecimal::new).toList();
    }

    private final List<BigDecimal> normal = history("100", "110", "95", "105", "100");   // average 102, sigma about 5.1

    @Test
    void bigSpike_isFlaggedAsLeak() {
        AnomalyDetector.Verdict v = detector.analyze(new BigDecimal("400"), normal, null, true);

        assertTrue(v.leak());
        assertFalse(v.overuse());
        assertEquals(102.0, v.mean(), 0.001);
        assertEquals(112.2, v.limit(), 0.1);
    }

    @Test
    void usageWithinTwoSigma_isNotALeak() {
        assertFalse(detector.analyze(new BigDecimal("110"), normal, null, true).leak());
    }

    @Test
    void justAboveLimit_isALeak() {
        assertTrue(detector.analyze(new BigDecimal("113"), normal, null, true).leak());
    }

    @Test
    void tooLittleHistory_neverFlagsLeak() {
        List<BigDecimal> fewer = history("100", "110", "95", "105");

        assertFalse(detector.analyze(new BigDecimal("900"), fewer, null, true).leak());
    }

    @Test
    void leakDetectionSwitchedOff_neverFlagsLeak() {
        assertFalse(detector.analyze(new BigDecimal("900"), normal, null, false).leak());
    }

    @Test
    void identicalHistory_usesMinimumBand() {
        List<BigDecimal> flat = history("100", "100", "100", "100", "100");   // sigma 0, band 5% = 5

        assertFalse(detector.analyze(new BigDecimal("105"), flat, null, true).leak());   // limit 110
        assertTrue(detector.analyze(new BigDecimal("115"), flat, null, true).leak());
    }

    @Test
    void thresholdCrossed_isOveruse_evenWithoutHistory() {
        AnomalyDetector.Verdict v = detector.analyze(new BigDecimal("500"), history(), new BigDecimal("350"), true);

        assertTrue(v.overuse());
        assertFalse(v.leak());
    }

    @Test
    void exactlyAtThreshold_isNotOveruse() {
        assertFalse(detector.analyze(new BigDecimal("350"), normal, new BigDecimal("350"), false).overuse());
    }

    @Test
    void noThreshold_neverOveruse() {
        assertFalse(detector.analyze(new BigDecimal("9999"), normal, null, false).overuse());
    }
}