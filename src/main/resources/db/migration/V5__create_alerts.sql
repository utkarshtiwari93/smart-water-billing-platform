ALTER TABLE water_usage_logs ADD COLUMN alert_checked BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE water_usage_logs SET alert_checked = TRUE;
CREATE INDEX idx_logs_alert_checked ON water_usage_logs (alert_checked);

CREATE TABLE alert_settings (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  apartment_id BIGINT NOT NULL,
  usage_threshold_litres DECIMAL(12,2) NULL,
  leak_detection_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uq_alert_settings_apartment UNIQUE (apartment_id),
  CONSTRAINT fk_alert_settings_apartment FOREIGN KEY (apartment_id)
    REFERENCES apartments(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  household_id BIGINT NULL,
  usage_log_id BIGINT NOT NULL,
  notification_type ENUM('LEAK_SUSPECTED','OVERUSE') NOT NULL,
  message VARCHAR(500) NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  email_sent BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_notification UNIQUE (usage_log_id, notification_type, user_id),
  CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_notification_household FOREIGN KEY (household_id) REFERENCES households(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);