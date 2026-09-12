package dev.merchantrail.transaction.application.usecase;

import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.application.port.in.SubmitTransactionCommand;
import dev.merchantrail.transaction.application.port.out.EventPublisher;
import dev.merchantrail.transaction.application.port.out.IdempotencyService;
import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import dev.merchantrail.transaction.domain.Transaction;

import java.util.Optional;

/**
 * Use case for submitting a new transaction with idempotency and card routing support.
 */
public class SubmitTransactionUseCase {
    
    private final TransactionRepository transactionRepository;
    private final IdempotencyService idempotencyService;
    private final EventPublisher eventPublisher;
    
    public SubmitTransactionUseCase(TransactionRepository transactionRepository,
                                   IdempotencyService idempotencyService,
                                   EventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.idempotencyService = idempotencyService;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Submits a transaction, checking for duplicate submissions via idempotency key.
     * If the idempotency key already exists, returns the existing transaction.
     */
    public Transaction execute(SubmitTransactionCommand command) {
        // Check for duplicate submission
        Optional<TransactionId> existingTransactionId = 
            idempotencyService.getTransactionId(command.idempotencyKey());
        
        if (existingTransactionId.isPresent()) {
            // Return existing transaction (idempotent behavior)
            return transactionRepository.findById(existingTransactionId.get())
                .orElseThrow(() -> new IllegalStateException(
                    "Idempotency key exists but transaction not found"
                ));
        }
        
        // Create new transaction with optional card switching attributes
        Transaction transaction = Transaction.create(
            command.merchantId(),
            command.amount(),
            command.idempotencyKey(),
            command.cardPan()
        );
        
        // Persist transaction
        Transaction savedTransaction = transactionRepository.save(transaction);
        
        // Store idempotency key
        idempotencyService.store(
            command.idempotencyKey(),
            savedTransaction.getTransactionId()
        );
        
        // Publish event
        eventPublisher.publishTransactionInitiated(savedTransaction);
        
        return savedTransaction;
    }
}
