CREATE TABLE auth_challenges (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(150) NOT NULL,
  purpose VARCHAR(32) NOT NULL,
  code_hash VARCHAR(100) NULL,
  reset_token_hash VARCHAR(100) NULL,
  attempts INT NOT NULL DEFAULT 0,
  expires_at DATETIME NOT NULL,
  sent_at DATETIME NOT NULL,
  CONSTRAINT uq_auth_challenge_email_purpose UNIQUE (email, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
