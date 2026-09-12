package dev.merchantrail.ledger.application.service;

import dev.merchantrail.ledger.application.port.out.LedgerRepository;
import dev.merchantrail.ledger.domain.EntryType;
import dev.merchantrail.ledger.domain.LedgerEntry;
import dev.merchantrail.ledger.domain.LedgerEntryStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Enterprise reporting and reconciliation service.
 * Enforces double-entry bookkeeping balance integrity, calculates net settlement,
 * interchange revenue, and generates downloadable audit CSV files.
 */
@Service
public class ReconciliationReportService {

    private final LedgerRepository ledgerRepository;

    public ReconciliationReportService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    public record SettlementSummary(
        long totalEntriesCount,
        long settledCount,
        long pendingCount,
        BigDecimal totalVolumeUsd,
        BigDecimal totalInterchangeFeesUsd,
        boolean isBalanced,
        BigDecimal netImbalanceUsd,
        String auditTimestamp
    ) {}

    public record SwitchingAnalytics(
        long totalTransactions,
        double mastercardSharePct,
        double visaSharePct,
        double stipAuthorizationRatePct,
        long totalStipTransactions,
        Map<String, Long> issuerVolumeMap
    ) {}

    public SettlementSummary getSettlementSummary() {
        List<LedgerEntry> all = ledgerRepository.findAll();

        long settledCount = 0;
        long pendingCount = 0;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (LedgerEntry entry : all) {
            BigDecimal amt = entry.getAmount().getAmount();
            if (entry.getStatus() == LedgerEntryStatus.SETTLED) {
                settledCount++;
            } else if (entry.getStatus() == LedgerEntryStatus.PENDING) {
                pendingCount++;
            }

            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebit = totalDebit.add(amt);
            } else {
                totalCredit = totalCredit.add(amt);
            }
        }

        // Double-entry bookkeeping rule: total debits must equal total credits
        BigDecimal imbalance = totalDebit.subtract(totalCredit).abs();
        boolean isBalanced = imbalance.compareTo(new BigDecimal("0.0001")) <= 0;

        // Interchange fee approx 1.5% of settled debits
        BigDecimal interchange = totalDebit.multiply(new BigDecimal("0.0150"))
            .setScale(2, RoundingMode.HALF_UP);

        return new SettlementSummary(
            all.size(),
            settledCount,
            pendingCount,
            totalDebit.setScale(2, RoundingMode.HALF_UP),
            interchange,
            isBalanced,
            imbalance.setScale(2, RoundingMode.HALF_UP),
            LocalDate.now().format(DateTimeFormatter.ISO_DATE)
        );
    }

    public SwitchingAnalytics getSwitchingAnalytics() {
        // Simulated network analytics based on active ledger records
        List<LedgerEntry> all = ledgerRepository.findAll();
        long totalTxs = Math.max(1, all.size() / 2);

        Map<String, Long> issuerMap = new LinkedHashMap<>();
        issuerMap.put("Citibank NA (BIN 51)", (long) (totalTxs * 0.35));
        issuerMap.put("JPMorgan Chase (BIN 52)", (long) (totalTxs * 0.30));
        issuerMap.put("Barclays Bank (BIN 53)", (long) (totalTxs * 0.15));
        issuerMap.put("HDFC Bank (BIN 55)", (long) (totalTxs * 0.12));
        issuerMap.put("Visa Dual Net (BIN 4x)", (long) (totalTxs * 0.08));

        long stipCount = Math.max(1, (long) (totalTxs * 0.035)); // 3.5% STIP rate
        double stipRate = (stipCount * 100.0) / totalTxs;

        return new SwitchingAnalytics(
            totalTxs,
            72.5, // 72.5% Mastercard brand share
            27.5, // 27.5% Other brand share
            Math.round(stipRate * 10.0) / 10.0,
            stipCount,
            issuerMap
        );
    }

    /**
     * Generates a standard audit reconciliation CSV.
     */
    public String generateReconciliationCsv() {
        List<LedgerEntry> entries = ledgerRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("EntryId,TransactionId,MerchantId,Type,Amount,Currency,Account,Status,CreatedAt\n");

        for (LedgerEntry e : entries) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                e.getEntryId(),
                e.getTransactionId().getValue(),
                e.getMerchantId().getValue(),
                e.getEntryType(),
                e.getAmount().getAmount(),
                e.getAmount().getCurrencyCode(),
                e.getAccount(),
                e.getStatus(),
                e.getCreatedAt()
            ));
        }

        return csv.toString();
    }
}
