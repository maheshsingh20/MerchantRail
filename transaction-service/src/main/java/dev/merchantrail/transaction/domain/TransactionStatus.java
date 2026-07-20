package dev.merchantrail.transaction.domain;

/**
 * Enumeration of transaction statuses in the payment lifecycle.
 */
public enum TransactionStatus {
    /**
     * Initial status when transaction is created.
     */
    PENDING,
    
    /**
     * Transaction passed fraud checks and is approved for processing.
     */
    APPROVED,
    
    /**
     * Transaction failed fraud checks or validation.
     */
    REJECTED,
    
    /**
     * Transaction successfully authorized by bank and settled.
     */
    SETTLED,
    
    /**
     * Transaction was reversed (compensating action in saga).
     */
    REVERSED
}
