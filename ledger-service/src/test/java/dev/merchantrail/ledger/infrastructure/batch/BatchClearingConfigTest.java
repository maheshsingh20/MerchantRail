package dev.merchantrail.ledger.infrastructure.batch;

import dev.merchantrail.ledger.adapter.out.persistence.LedgerRepositoryImpl;
import dev.merchantrail.ledger.domain.EntryType;
import dev.merchantrail.ledger.domain.LedgerEntry;
import dev.merchantrail.ledger.domain.LedgerEntryStatus;
import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Spring Batch Clearing Engine Tests")
class BatchClearingConfigTest {

    private LedgerRepositoryImpl ledgerRepository;
    private BatchClearingConfig batchConfig;

    @BeforeEach
    void setUp() {
        ledgerRepository = new LedgerRepositoryImpl();
        batchConfig = new BatchClearingConfig();
    }

    @Test
    @DisplayName("Should process clearing item, transition status to SETTLED and calculate interchange")
    void shouldProcessClearingItem() throws Exception {
        LedgerEntry entry = LedgerEntry.createDebit(
            TransactionId.generate(),
            MerchantId.of("MERCH001"),
            Money.of(new BigDecimal("100.00"), "USD"),
            "1010-CASH"
        );

        assertThat(entry.getStatus()).isEqualTo(LedgerEntryStatus.PENDING);

        var processor = batchConfig.clearingProcessor();
        ClearedLedgerBatchItem clearedItem = processor.process(entry);

        assertThat(clearedItem).isNotNull();
        assertThat(clearedItem.entry().getStatus()).isEqualTo(LedgerEntryStatus.SETTLED);
        // $100.00 * 0.0150 = $1.50 interchange fee
        assertThat(clearedItem.estimatedInterchange()).isEqualByComparingTo("1.5000");
    }

    @Test
    @DisplayName("Should query only unsettled entries for the batch reader")
    void shouldQueryOnlyUnsettledEntries() {
        TransactionId txn1 = TransactionId.generate();
        TransactionId txn2 = TransactionId.generate();
        MerchantId merchant = MerchantId.of("MERCH001");

        LedgerEntry pendingEntry = LedgerEntry.createDebit(txn1, merchant, Money.of(new BigDecimal("50.00"), "USD"), "1010");
        LedgerEntry settledEntry = LedgerEntry.createCredit(txn2, merchant, Money.of(new BigDecimal("50.00"), "USD"), "2010");
        settledEntry.settle();

        ledgerRepository.save(pendingEntry);
        ledgerRepository.save(settledEntry);

        List<LedgerEntry> unsettled = ledgerRepository.findUnsettledEntries();
        assertThat(unsettled).hasSize(1);
        assertThat(unsettled.get(0).getEntryId()).isEqualTo(pendingEntry.getEntryId());
    }

    @Test
    @DisplayName("Should batch write cleared items and update repository status")
    void shouldBatchWriteClearedItems() throws Exception {
        LedgerEntry entry = LedgerEntry.createDebit(
            TransactionId.generate(),
            MerchantId.of("MERCH001"),
            Money.of(new BigDecimal("75.00"), "USD"),
            "1010-CASH"
        );
        ledgerRepository.save(entry);

        var processor = batchConfig.clearingProcessor();
        ClearedLedgerBatchItem cleared = processor.process(entry);

        var writer = batchConfig.clearingBatchWriter(ledgerRepository);
        writer.write(new org.springframework.batch.item.Chunk<>(List.of(cleared)));

        List<LedgerEntry> unsettledAfter = ledgerRepository.findUnsettledEntries();
        assertThat(unsettledAfter).isEmpty();

        List<LedgerEntry> byTxn = ledgerRepository.findByTransactionId(entry.getTransactionId().getValue());
        assertThat(byTxn.get(0).getStatus()).isEqualTo(LedgerEntryStatus.SETTLED);
    }
}
