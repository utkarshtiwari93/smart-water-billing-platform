package com.infosys.Water_Usage_Monitoring.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "households")
@Getter
@Setter
@NoArgsConstructor
public class Household {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "apartment_id", nullable = false)
    private Apartment apartment;

    @Column(name = "flat_number", nullable = false, length = 20)
    private String flatNumber;

    @Column(name = "area_sqft", precision = 8, scale = 2)
    private BigDecimal areaSqft;
    private Integer occupancy;

    @Column(name = "meter_number", unique = true, length = 50)
    private String meterNumber;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // REMOVE jaan bujh ke nahi lagaya: household delete se usage history na udde
    @OneToMany(mappedBy = "household", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<WaterUsageLog> usageLogs = new ArrayList<>();
}
