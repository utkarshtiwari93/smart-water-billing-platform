package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;

public record InvoiceItemResponse(String type, String description, BigDecimal quantity,
                                  BigDecimal unitRate, BigDecimal amount) {}