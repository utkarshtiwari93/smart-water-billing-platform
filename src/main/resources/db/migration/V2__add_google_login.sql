ALTER TABLE users MODIFY password_hash VARCHAR(255) NULL;

ALTER TABLE users
  ADD COLUMN google_sub VARCHAR(64) NULL,
  ADD CONSTRAINT uq_users_google_sub UNIQUE (google_sub);