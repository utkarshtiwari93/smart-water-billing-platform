package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;

public record TariffSlabChargeResponse(int slabNumber, BigDecimal fromKl, BigDecimal upToKl,
                                       BigDecimal consumptionKl, BigDecimal rate, BigDecimal amount) {
}
