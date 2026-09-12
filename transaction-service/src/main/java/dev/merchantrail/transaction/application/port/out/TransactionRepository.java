package dev.merchantrail.transaction.application.port.out;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.domain.Transaction;

import java.util.List;
import java.util.Optional;

/**
 * Output port for transaction persistence.
 */
public interface TransactionRepository {
    
    /**
     * Saves a transaction (insert or update).
     */
    Transaction save(Transaction transaction);
    
    /**
     * Finds a transaction by its ID.
     */
    Optional<Transaction> findById(TransactionId transactionId);
    
    /**
     * Finds all transactions for a merchant.
     */
    List<Transaction> findByMerchantId(MerchantId merchantId);

    /**
     * Finds all transactions in the system.
     */
    List<Transaction> findAll();
}
