package dev.merchantrail.ledger.domain;

/**
 * Status lifecycle of a ledger entry in double-entry bookkeeping.
 */
public enum LedgerEntryStatus {
    PENDING,
    SETTLED,
    REVERSED
}
