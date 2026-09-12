package dev.merchantrail.ledger.adapter.out.persistence;

import dev.merchantrail.ledger.application.port.out.LedgerRepository;
import dev.merchantrail.ledger.domain.LedgerEntry;
import dev.merchantrail.ledger.domain.LedgerEntryStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe implementation of LedgerRepository supporting
 * double-entry bookkeeping queries and Spring Batch clearing chunks.
 */
@Repository
public class LedgerRepositoryImpl implements LedgerRepository {
    
    private final Map<String, LedgerEntry> entriesById = new ConcurrentHashMap<>();
    private final Map<String, List<LedgerEntry>> entriesByTxnId = new ConcurrentHashMap<>();
    
    @Override
    public LedgerEntry save(LedgerEntry entry) {
        entriesById.put(entry.getEntryId(), entry);
        entriesByTxnId.computeIfAbsent(entry.getTransactionId().getValue(), k -> Collections.synchronizedList(new ArrayList<>()))
                      .removeIf(e -> e.getEntryId().equals(entry.getEntryId()));
        entriesByTxnId.get(entry.getTransactionId().getValue()).add(entry);
        return entry;
    }
    
    @Override
    public List<LedgerEntry> findByTransactionId(String transactionId) {
        List<LedgerEntry> list = entriesByTxnId.get(transactionId);
        return list != null ? new ArrayList<>(list) : Collections.emptyList();
    }
    
    @Override
    public List<LedgerEntry> findSettlementEntries(LocalDate date) {
        List<LedgerEntry> result = new ArrayList<>();
        for (LedgerEntry entry : entriesById.values()) {
            LocalDate entryDate = entry.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
            if (entryDate.equals(date)) {
                result.add(entry);
            }
        }
        return result;
    }
    
    @Override
    public List<LedgerEntry> findUnsettledEntries() {
        List<LedgerEntry> unsettled = new ArrayList<>();
        for (LedgerEntry entry : entriesById.values()) {
            if (entry.getStatus() == LedgerEntryStatus.PENDING) {
                unsettled.add(entry);
            }
        }
        return unsettled;
    }

    @Override
    public List<LedgerEntry> findAll() {
        return new ArrayList<>(entriesById.values());
    }
}
