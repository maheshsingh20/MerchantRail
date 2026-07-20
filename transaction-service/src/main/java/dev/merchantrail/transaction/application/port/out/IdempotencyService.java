package dev.merchantrail.transaction.application.port.out;

import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.domain.IdempotencyKey;

import java.util.Optional;

/**
 * Output port for idempotency key management.
 */
public interface IdempotencyService {
    
    /**
     * Checks if an idempotency key exists and returns associated transaction ID.
     */
    Optional<TransactionId> getTransactionId(IdempotencyKey key);
    
    /**
     * Stores an idempotency key with its transaction ID.
     */
    void store(IdempotencyKey key, TransactionId transactionId);
}
