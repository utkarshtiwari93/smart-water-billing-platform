package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;

public record TariffSlabResponse(int slabNumber, BigDecimal fromKl, BigDecimal upToKl, BigDecimal rate) {
}
