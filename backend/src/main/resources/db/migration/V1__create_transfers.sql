CREATE TABLE transfers (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    pickup_code VARCHAR(6) NOT NULL UNIQUE,
    content TEXT NOT NULL,
    delete_token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX idx_transfers_expires_at ON transfers (expires_at);
