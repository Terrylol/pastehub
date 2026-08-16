CREATE TABLE image_transfers (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    pickup_code VARCHAR(6) NULL UNIQUE,
    object_key VARCHAR(128) NOT NULL UNIQUE,
    mime_type VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL,
    upload_token_hash VARCHAR(64) NOT NULL,
    delete_token_hash VARCHAR(64) NULL,
    state VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    deleted_at TIMESTAMP NULL
);
CREATE INDEX idx_image_transfers_expires_at ON image_transfers (expires_at);
