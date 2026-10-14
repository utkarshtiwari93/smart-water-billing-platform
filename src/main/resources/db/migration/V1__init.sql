CREATE TABLE apartments (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  address VARCHAR(255),
  city VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE households (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  apartment_id BIGINT NOT NULL,
  flat_number VARCHAR(20) NOT NULL,
  area_sqft DECIMAL(8,2),
  occupancy INT,
  meter_number VARCHAR(50) UNIQUE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_flat UNIQUE (apartment_id, flat_number),
  CONSTRAINT fk_hh_apartment FOREIGN KEY (apartment_id) REFERENCES apartments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  apartment_id BIGINT NOT NULL,
  household_id BIGINT NULL,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role ENUM('ADMIN','RESIDENT') NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_user_apartment FOREIGN KEY (apartment_id) REFERENCES apartments(id),
  CONSTRAINT fk_user_household FOREIGN KEY (household_id) REFERENCES households(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE water_usage_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  household_id BIGINT NOT NULL,
  reading_date DATE NOT NULL,
  meter_reading DECIMAL(12,2) NOT NULL,
  consumption_litres DECIMAL(12,2) NOT NULL,
  source ENUM('MANUAL','CSV') NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_reading UNIQUE (household_id, reading_date),
  CONSTRAINT fk_log_household FOREIGN KEY (household_id) REFERENCES households(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE tariff_plans (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  apartment_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  tier1_limit_kl DECIMAL(8,2) NOT NULL DEFAULT 10.00,
  base_rate DECIMAL(10,2) NOT NULL,
  higher_rate DECIMAL(10,2) NOT NULL,
  effective_from DATE NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_tariff_apartment FOREIGN KEY (apartment_id) REFERENCES apartments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE billing_cycles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  apartment_id BIGINT NOT NULL,
  tariff_plan_id BIGINT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  status ENUM('OPEN','FINALIZED','ARCHIVED') NOT NULL DEFAULT 'OPEN',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_cycle_apartment FOREIGN KEY (apartment_id) REFERENCES apartments(id),
  CONSTRAINT fk_cycle_tariff FOREIGN KEY (tariff_plan_id) REFERENCES tariff_plans(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;