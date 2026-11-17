CREATE TABLE tariff_slabs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tariff_plan_id BIGINT NOT NULL,
  slab_index INT NOT NULL,
  upper_limit_kl DECIMAL(8,2) NULL,
  rate DECIMAL(10,2) NOT NULL,
  CONSTRAINT uq_tariff_slab_order UNIQUE (tariff_plan_id, slab_index),
  CONSTRAINT fk_tariff_slab_plan FOREIGN KEY (tariff_plan_id)
    REFERENCES tariff_plans(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO tariff_slabs (tariff_plan_id, slab_index, upper_limit_kl, rate)
SELECT id, 0, tier1_limit_kl, base_rate
FROM tariff_plans;

INSERT INTO tariff_slabs (tariff_plan_id, slab_index, upper_limit_kl, rate)
SELECT id, 1, NULL, higher_rate
FROM tariff_plans;
