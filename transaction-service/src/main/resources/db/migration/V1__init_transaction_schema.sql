-- Initial Transaction Schema
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(16) PRIMARY KEY,
    merchant_id VARCHAR(8) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    status_reason VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_merchant_id ON transactions (merchant_id);
CREATE INDEX IF NOT EXISTS idx_status ON transactions (status);
CREATE INDEX IF NOT EXISTS idx_created_at ON transactions (created_at);
