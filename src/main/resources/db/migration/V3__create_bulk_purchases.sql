CREATE TABLE bulk_purchases (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  billing_cycle_id BIGINT NOT NULL,
  purchase_date DATE NOT NULL,
  source ENUM('TANKER','MUNICIPAL','BOREWELL','OTHER') NOT NULL,
  supplier VARCHAR(150) NULL,
  volume_kl DECIMAL(12,2) NOT NULL,
  unit_cost DECIMAL(10,2) NOT NULL,
  total_cost DECIMAL(14,2) NOT NULL,
  notes VARCHAR(255) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_purchase_cycle FOREIGN KEY (billing_cycle_id)
    REFERENCES billing_cycles(id) ON DELETE CASCADE,
  CONSTRAINT chk_purchase_volume CHECK (volume_kl > 0),
  CONSTRAINT chk_purchase_unit_cost CHECK (unit_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;