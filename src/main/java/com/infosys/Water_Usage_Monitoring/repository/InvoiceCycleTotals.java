package com.infosys.Water_Usage_Monitoring.repository;

import java.math.BigDecimal;

public interface InvoiceCycleTotals {

    Long getInvoiceCount();

    BigDecimal getInvoiceTotal();
}
