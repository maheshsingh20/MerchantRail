package dev.merchantrail.fraud.adapter.in.messaging;

import dev.merchantrail.fraud.application.port.in.CheckFraudCommand;
import dev.merchantrail.fraud.application.usecase.CheckFraudUseCase;
import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for transaction.initiated events.
 * Triggers fraud check when new transaction is created.
 */
@Component
public class TransactionEventConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);
    
    private final CheckFraudUseCase checkFraudUseCase;
    
    public TransactionEventConsumer(CheckFraudUseCase checkFraudUseCase) {
        this.checkFraudUseCase = checkFraudUseCase;
    }
    
    @KafkaListener(topics = "transaction.initiated", groupId = "fraud-service")
    public void handleTransactionInitiated(TransactionEventDto event) {
        log.info("Received transaction.initiated event for transaction {}", event.getTransactionId());
        
        try {
            CheckFraudCommand command = new CheckFraudCommand(
                TransactionId.of(event.getTransactionId()),
                MerchantId.of(event.getMerchantId()),
                Money.of(event.getAmount(), event.getCurrency())
            );
            
            checkFraudUseCase.execute(command);
            
        } catch (Exception e) {
            log.error("Failed to process transaction.initiated event", e);
            throw e; // Will be retried by Kafka
        }
    }
}
