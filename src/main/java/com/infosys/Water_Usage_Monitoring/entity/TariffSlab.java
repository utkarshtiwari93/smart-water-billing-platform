package com.infosys.Water_Usage_Monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "tariff_slabs", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tariff_slab_order", columnNames = {"tariff_plan_id", "slab_index"})
})
@Getter
@Setter
@NoArgsConstructor
public class TariffSlab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariff_plan_id", nullable = false)
    private TariffPlan tariffPlan;

    @Column(name = "slab_index", nullable = false)
    private int order;

    @Column(name = "upper_limit_kl", precision = 8, scale = 2)
    private BigDecimal upperLimitKl;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;
}
