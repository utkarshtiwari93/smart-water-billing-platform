package com.infosys.Water_Usage_Monitoring.entity;

import com.infosys.Water_Usage_Monitoring.enums.InvoiceItemType;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false)
    private InvoiceItemType itemType;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(precision = 12, scale = 4)
    private BigDecimal quantity;

    @Column(name = "unit_rate", precision = 10, scale = 2)
    private BigDecimal unitRate;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;
}