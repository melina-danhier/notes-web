ALTER TABLE users
    ADD COLUMN demo_account BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN expires_at TIMESTAMPTZ;

CREATE INDEX idx_users_demo_expiry
    ON users (expires_at)
    WHERE demo_account = TRUE;
