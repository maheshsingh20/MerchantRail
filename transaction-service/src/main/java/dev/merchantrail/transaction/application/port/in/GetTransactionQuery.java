package dev.merchantrail.transaction.application.port.in;

import dev.merchantrail.shared.TransactionId;

/**
 * Query to retrieve a transaction by ID.
 */
public record GetTransactionQuery(TransactionId transactionId) {
    public GetTransactionQuery {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
    }
}
