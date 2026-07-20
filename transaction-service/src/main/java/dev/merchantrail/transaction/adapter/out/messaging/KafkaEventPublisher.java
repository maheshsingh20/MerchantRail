package dev.merchantrail.transaction.adapter.out.messaging;

import dev.merchantrail.transaction.application.port.out.EventPublisher;
import dev.merchantrail.transaction.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka-based implementation of event publisher.
 */
@Component
public class KafkaEventPublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    
    private static final String TOPIC_TRANSACTION_INITIATED = "transaction.initiated";
    private static final String TOPIC_TRANSACTION_APPROVED = "transaction.approved";
    private static final String TOPIC_TRANSACTION_SETTLED = "transaction.settled";
    private static final String TOPIC_TRANSACTION_REVERSED = "transaction.reversed";
    
    private final KafkaTemplate<String, TransactionEventDto> kafkaTemplate;
    
    public KafkaEventPublisher(KafkaTemplate<String, TransactionEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Override
    public void publishTransactionInitiated(Transaction transaction) {
        TransactionEventDto event = TransactionEventDto.fromDomain(transaction);
        kafkaTemplate.send(TOPIC_TRANSACTION_INITIATED, 
                          transaction.getTransactionId().getValue(), 
                          event);
        log.info("Published transaction.initiated event for {}", transaction.getTransactionId());
    }
    
    @Override
    public void publishTransactionApproved(Transaction transaction) {
        TransactionEventDto event = TransactionEventDto.fromDomain(transaction);
        kafkaTemplate.send(TOPIC_TRANSACTION_APPROVED, 
                          transaction.getTransactionId().getValue(), 
                          event);
        log.info("Published transaction.approved event for {}", transaction.getTransactionId());
    }
    
    @Override
    public void publishTransactionSettled(Transaction transaction) {
        TransactionEventDto event = TransactionEventDto.fromDomain(transaction);
        kafkaTemplate.send(TOPIC_TRANSACTION_SETTLED, 
                          transaction.getTransactionId().getValue(), 
                          event);
        log.info("Published transaction.settled event for {}", transaction.getTransactionId());
    }
    
    @Override
    public void publishTransactionReversed(Transaction transaction) {
        TransactionEventDto event = TransactionEventDto.fromDomain(transaction);
        kafkaTemplate.send(TOPIC_TRANSACTION_REVERSED, 
                          transaction.getTransactionId().getValue(), 
                          event);
        log.info("Published transaction.reversed event for {}", transaction.getTransactionId());
    }
}
