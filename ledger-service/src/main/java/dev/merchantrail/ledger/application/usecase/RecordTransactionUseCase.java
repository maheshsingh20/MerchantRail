package dev.merchantrail.ledger.application.usecase;

import dev.merchantrail.ledger.application.port.in.RecordTransactionCommand;
import dev.merchantrail.ledger.application.port.out.LedgerRepository;
import dev.merchantrail.ledger.domain.LedgerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Records transaction as double-entry: debit merchant liability, credit bank asset.
 */
public class RecordTransactionUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(RecordTransactionUseCase.class);
    private static final String MERCHANT_LIABILITY_ACCOUNT = "MERCHANT_LIABILITY";
    private static final String BANK_ASSET_ACCOUNT = "BANK_ASSET";
    
    private final LedgerRepository ledgerRepository;
    
    public RecordTransactionUseCase(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }
    
    public void execute(RecordTransactionCommand command) {
        log.info("Recording transaction {} in ledger", command.transactionId());
        
        // Double-entry: Debit merchant liability, Credit bank asset
        LedgerEntry debit = LedgerEntry.createDebit(
            command.transactionId(),
            command.merchantId(),
            command.amount(),
            MERCHANT_LIABILITY_ACCOUNT
        );
        
        LedgerEntry credit = LedgerEntry.createCredit(
            command.transactionId(),
            command.merchantId(),
            command.amount(),
            BANK_ASSET_ACCOUNT
        );
        
        // Save both entries atomically
        ledgerRepository.saveAll(List.of(debit, credit));
        
        log.info("Recorded ledger entries for transaction {}: debit={}, credit={}",
            command.transactionId(), debit.getEntryId(), credit.getEntryId());
    }
}
