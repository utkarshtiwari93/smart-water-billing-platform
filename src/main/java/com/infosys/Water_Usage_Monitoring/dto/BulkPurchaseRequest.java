package com.infosys.Water_Usage_Monitoring.dto;

import com.infosys.Water_Usage_Monitoring.enums.PurchaseSource;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BulkPurchaseRequest(
        @NotNull LocalDate purchaseDate,
        @NotNull PurchaseSource source,
        @Size(max = 150) String supplier,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal volumeKl,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal unitCost,
        @Size(max = 255) String notes) {}