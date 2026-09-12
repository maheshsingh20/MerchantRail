package dev.merchantrail.ledger.domain;

/**
 * Double-entry bookkeeping entry type.
 */
public enum EntryType {
    DEBIT,  // Money leaving account
    CREDIT  // Money entering account
}
