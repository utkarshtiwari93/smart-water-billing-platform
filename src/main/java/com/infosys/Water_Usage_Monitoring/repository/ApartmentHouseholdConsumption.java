package com.infosys.Water_Usage_Monitoring.repository;

import java.math.BigDecimal;

public interface ApartmentHouseholdConsumption {

    Long getHouseholdId();

    String getFlatNumber();

    BigDecimal getConsumptionLitres();
}
