-- V2 Migration: Add Card Switching, STIP, and Performance Indexes
ALTER TABLE transactions
    ADD COLUMN IF NOT EXISTS card_pan VARCHAR(32),
    ADD COLUMN IF NOT EXISTS card_bin VARCHAR(8),
    ADD COLUMN IF NOT EXISTS card_brand VARCHAR(20),
    ADD COLUMN IF NOT EXISTS routed_issuer_id VARCHAR(50),
    ADD COLUMN IF NOT EXISTS is_stip BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS auth_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS interchange_fee NUMERIC(19, 4),
    ADD COLUMN IF NOT EXISTS switch_fee NUMERIC(19, 4);

-- High-throughput composite indexes for card switching and reconciliation
CREATE INDEX IF NOT EXISTS idx_txn_card_bin_created ON transactions (card_bin, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_txn_merchant_status ON transactions (merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_txn_is_stip ON transactions (is_stip, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_txn_routed_issuer ON transactions (routed_issuer_id);
