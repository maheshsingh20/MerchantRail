package dev.merchantrail.transaction.adapter.out.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.merchantrail.transaction.adapter.out.persistence.OutboxEventJpaEntity;
import dev.merchantrail.transaction.application.port.out.EventPublisher;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.infrastructure.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Event publisher using outbox pattern for reliable delivery.
 * Writes events to outbox table instead of directly to Kafka.
 */
@Component
@Primary
public class OutboxKafkaEventPublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(OutboxKafkaEventPublisher.class);
    
    private static final String AGGREGATE_TYPE = "Transaction";
    private static final String TOPIC_TRANSACTION_INITIATED = "transaction.initiated";
    private static final String TOPIC_TRANSACTION_APPROVED = "transaction.approved";
    private static final String TOPIC_TRANSACTION_SETTLED = "transaction.settled";
    private static final String TOPIC_TRANSACTION_REVERSED = "transaction.reversed";
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    public OutboxKafkaEventPublisher(OutboxEventRepository outboxEventRepository,
                                    ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void publishTransactionInitiated(Transaction transaction) {
        publishToOutbox(transaction, TOPIC_TRANSACTION_INITIATED);
    }
    
    @Override
    public void publishTransactionApproved(Transaction transaction) {
        publishToOutbox(transaction, TOPIC_TRANSACTION_APPROVED);
    }
    
    @Override
    public void publishTransactionSettled(Transaction transaction) {
        publishToOutbox(transaction, TOPIC_TRANSACTION_SETTLED);
    }
    
    @Override
    public void publishTransactionReversed(Transaction transaction) {
        publishToOutbox(transaction, TOPIC_TRANSACTION_REVERSED);
    }
    
    private void publishToOutbox(Transaction transaction, String eventType) {
        try {
            TransactionEventDto eventDto = TransactionEventDto.fromDomain(transaction);
            String payload = objectMapper.writeValueAsString(eventDto);
            
            OutboxEventJpaEntity outboxEvent = new OutboxEventJpaEntity(
                transaction.getTransactionId().getValue(),
                AGGREGATE_TYPE,
                eventType,
                payload
            );
            
            outboxEventRepository.save(outboxEvent);
            
            log.info("Wrote {} event to outbox for transaction {}",
                eventType, transaction.getTransactionId());
                
        } catch (Exception e) {
            log.error("Failed to write event to outbox", e);
            throw new RuntimeException("Failed to write event to outbox", e);
        }
    }
}
