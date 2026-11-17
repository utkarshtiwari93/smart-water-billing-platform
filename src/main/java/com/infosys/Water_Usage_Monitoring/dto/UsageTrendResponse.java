package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UsageTrendResponse(
        LocalDate from,
        LocalDate to,
        String granularity,
        BigDecimal totalConsumptionLitres,
        int coveredDays,
        List<Point> points) {

    public record Point(String period, BigDecimal consumptionLitres, int coveredDays, int periodDays) {}
}
