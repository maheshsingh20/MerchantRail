package dev.merchantrail.ledger.adapter.out.persistence;

import dev.merchantrail.ledger.application.port.out.LedgerRepository;
import dev.merchantrail.ledger.domain.LedgerEntry;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of LedgerRepository
 * TODO: Replace with JPA implementation for production
 */
@Repository
public class LedgerRepositoryImpl implements LedgerRepository {
    
    private final Map<String, List<LedgerEntry>> entries = new ConcurrentHashMap<>();
    
    @Override
    public LedgerEntry save(LedgerEntry entry) {
        entries.computeIfAbsent(entry.getTransactionId().getValue(), k -> new ArrayList<>()).add(entry);
        return entry;
    }
    
    @Override
    public List<LedgerEntry> findByTransactionId(String transactionId) {
        return entries.getOrDefault(transactionId, new ArrayList<>());
    }
    
    @Override
    public List<LedgerEntry> findSettlementEntries(LocalDate date) {
        return new ArrayList<>();
    }
    
    @Override
    public List<LedgerEntry> findUnsettledEntries() {
        return new ArrayList<>();
    }
}
