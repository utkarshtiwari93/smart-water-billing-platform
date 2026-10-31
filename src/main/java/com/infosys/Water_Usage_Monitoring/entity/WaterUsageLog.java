package com.infosys.Water_Usage_Monitoring.entity;

import com.infosys.Water_Usage_Monitoring.enums.ReadingSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "water_usage_logs",
        uniqueConstraints = @UniqueConstraint(name = "uq_reading", columnNames = {"household_id", "reading_date"}))
@Getter
@Setter
@NoArgsConstructor
public class WaterUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "household_id", nullable = false)
    private Household household;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "meter_reading", nullable = false, precision = 12, scale = 2)
    private BigDecimal meterReading;

    @Column(name = "consumption_litres", nullable = false, precision = 12, scale = 2)
    private BigDecimal consumptionLitres;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingSource source;

    @Column(name = "alert_checked", nullable = false)
    private boolean alertChecked;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}