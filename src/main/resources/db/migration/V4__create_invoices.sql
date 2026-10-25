CREATE TABLE adjustments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  billing_cycle_id BIGINT NOT NULL,
  household_id BIGINT NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  reason VARCHAR(255) NOT NULL,
  created_by VARCHAR(150) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_adj_cycle FOREIGN KEY (billing_cycle_id)
    REFERENCES billing_cycles(id) ON DELETE CASCADE,
  CONSTRAINT fk_adj_household FOREIGN KEY (household_id) REFERENCES households(id),
  CONSTRAINT chk_adj_amount CHECK (amount <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoices (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_number VARCHAR(40) NOT NULL,
  billing_cycle_id BIGINT NOT NULL,
  household_id BIGINT NOT NULL,
  consumption_litres DECIMAL(12,2) NOT NULL,
  total_amount DECIMAL(14,2) NOT NULL,
  issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_invoice_number UNIQUE (invoice_number),
  CONSTRAINT uq_invoice_cycle_household UNIQUE (billing_cycle_id, household_id),
  CONSTRAINT fk_invoice_cycle FOREIGN KEY (billing_cycle_id) REFERENCES billing_cycles(id),
  CONSTRAINT fk_invoice_household FOREIGN KEY (household_id) REFERENCES households(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_id BIGINT NOT NULL,
  item_type ENUM('WATER_TIER1','WATER_TIER2','SHARED_AREA','ADJUSTMENT') NOT NULL,
  description VARCHAR(255) NOT NULL,
  quantity DECIMAL(12,4) NULL,
  unit_rate DECIMAL(10,2) NULL,
  amount DECIMAL(14,2) NOT NULL,
  CONSTRAINT fk_item_invoice FOREIGN KEY (invoice_id)
    REFERENCES invoices(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;