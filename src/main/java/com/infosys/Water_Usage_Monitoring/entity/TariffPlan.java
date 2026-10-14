package com.infosys.Water_Usage_Monitoring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tariff_plans")
@Getter
@Setter
@NoArgsConstructor
public class TariffPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "tier1_limit_kl", nullable = false, precision = 8, scale = 2)
    private BigDecimal tier1LimitKl = new BigDecimal("10.00");

    @Column(name = "base_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseRate;

    @Column(name = "higher_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal higherRate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false)
    private boolean active = true;
}