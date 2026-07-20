package dev.merchantrail.transaction.application.usecase;

import dev.merchantrail.transaction.application.port.in.GetTransactionQuery;
import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import dev.merchantrail.transaction.domain.Transaction;

/**
 * Use case for retrieving a transaction by ID.
 */
public class GetTransactionUseCase {
    
    private final TransactionRepository transactionRepository;
    
    public GetTransactionUseCase(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    public Transaction execute(GetTransactionQuery query) {
        return transactionRepository.findById(query.transactionId())
            .orElseThrow(() -> new TransactionNotFoundException(
                "Transaction not found: " + query.transactionId()
            ));
    }
    
    public static class TransactionNotFoundException extends RuntimeException {
        public TransactionNotFoundException(String message) {
            super(message);
        }
    }
}
