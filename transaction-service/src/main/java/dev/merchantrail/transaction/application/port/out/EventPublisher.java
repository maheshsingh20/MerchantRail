package dev.merchantrail.transaction.application.port.out;

import dev.merchantrail.transaction.domain.Transaction;

/**
 * Output port for publishing domain events.
 */
public interface EventPublisher {
    
    /**
     * Publishes a transaction initiated event.
     */
    void publishTransactionInitiated(Transaction transaction);
    
    /**
     * Publishes a transaction approved event.
     */
    void publishTransactionApproved(Transaction transaction);
    
    /**
     * Publishes a transaction settled event.
     */
    void publishTransactionSettled(Transaction transaction);
    
    /**
     * Publishes a transaction reversed event.
     */
    void publishTransactionReversed(Transaction transaction);
}
