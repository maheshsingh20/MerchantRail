package dev.merchantrail.ledger.application.usecase;

import dev.merchantrail.ledger.application.port.in.RecordTransactionCommand;
import dev.merchantrail.ledger.application.port.out.LedgerRepository;
import dev.merchantrail.ledger.domain.LedgerEntry;
import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Records transaction as double-entry: debit merchant liability, credit bank asset.
 */
@Service
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
        
        // Convert to value objects
        TransactionId transactionId = TransactionId.of(command.transactionId());
        MerchantId merchantId = MerchantId.of(command.merchantId());
        Money amount = Money.of(command.amount(), command.currency());
        
        // Double-entry: Debit merchant liability, Credit bank asset
        LedgerEntry debit = LedgerEntry.createDebit(
            transactionId,
            merchantId,
            amount,
            MERCHANT_LIABILITY_ACCOUNT
        );
        
        LedgerEntry credit = LedgerEntry.createCredit(
            transactionId,
            merchantId,
            amount,
            BANK_ASSET_ACCOUNT
        );
        
        // Save both entries
        ledgerRepository.save(debit);
        ledgerRepository.save(credit);
        
        log.info("Recorded ledger entries for transaction {}: debit={}, credit={}",
            command.transactionId(), debit.getEntryId(), credit.getEntryId());
    }
}

