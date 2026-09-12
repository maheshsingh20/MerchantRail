package dev.merchantrail.ledger.infrastructure.batch;

import dev.merchantrail.ledger.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Intermediate DTO representing a processed clearing item in the batch chunk.
 */
public record ClearedLedgerBatchItem(
    LedgerEntry entry,
    BigDecimal estimatedInterchange,
    Instant clearedTimestamp
) {}
