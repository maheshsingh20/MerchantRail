package dev.merchantrail.ledger.application.port.out;

import dev.merchantrail.ledger.domain.LedgerEntry;

import java.time.LocalDate;
import java.util.List;

/**
 * Port for ledger persistence operations
 */
public interface LedgerRepository {
    
    LedgerEntry save(LedgerEntry entry);
    
    List<LedgerEntry> findByTransactionId(String transactionId);
    
    List<LedgerEntry> findSettlementEntries(LocalDate date);
    
    List<LedgerEntry> findUnsettledEntries();
}
