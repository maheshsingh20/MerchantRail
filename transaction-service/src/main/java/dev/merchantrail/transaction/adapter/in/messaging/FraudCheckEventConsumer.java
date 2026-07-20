package dev.merchantrail.transaction.adapter.in.messaging;

import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import dev.merchantrail.transaction.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes fraud check results and updates transaction status.
 * Part of saga pattern - reacts to fraud-service decisions.
 */
@Component
public class FraudCheckEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(FraudCheckEventConsumer.class);
    
    private final TransactionRepository transactionRepository;
    
    public FraudCheckEventConsumer(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    @KafkaListener(topics = "fraud.passed", groupId = "transaction-service")
    public void handleFraudPassed(FraudCheckEventDto event) {
        log.info("Fraud check PASSED for transaction {}", event.getTransactionId());
        
        TransactionId txnId = TransactionId.of(event.getTransactionId());
        Transaction transaction = transactionRepository.findById(txnId)
            .orElseThrow(() -> new IllegalStateException("Transaction not found: " + txnId));
        
        transaction.approve();
        transactionRepository.save(transaction);
        
        log.info("Transaction {} approved after fraud check", txnId);
        // Next step in saga: call bank-simulator (to be implemented)
    }
    
    @KafkaListener(topics = "fraud.failed", groupId = "transaction-service")
    public void handleFraudFailed(FraudCheckEventDto event) {
        log.info("Fraud check FAILED for transaction {}", event.getTransactionId());
        
        TransactionId txnId = TransactionId.of(event.getTransactionId());
        Transaction transaction = transactionRepository.findById(txnId)
            .orElseThrow(() -> new IllegalStateException("Transaction not found: " + txnId));
        
        transaction.reject("Fraud check failed: " + event.getReason());
        transactionRepository.save(transaction);
        
        log.info("Transaction {} rejected due to fraud", txnId);
    }
}
