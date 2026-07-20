package dev.merchantrail.transaction.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.merchantrail.transaction.adapter.out.messaging.TransactionEventDto;
import dev.merchantrail.transaction.adapter.out.persistence.OutboxEventJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Polls outbox table and publishes events to Kafka.
 * Ensures at-least-once delivery of events.
 */
@Component
public class OutboxPoller {
    
    private static final Logger log = LoggerFactory.getLogger(OutboxPoller.class);
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, TransactionEventDto> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public OutboxPoller(OutboxEventRepository outboxEventRepository,
                       KafkaTemplate<String, TransactionEventDto> kafkaTemplate,
                       ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Scheduled(fixedDelay = 1000) // Poll every second
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventJpaEntity> pendingEvents = 
            outboxEventRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        log.debug("Publishing {} pending outbox events", pendingEvents.size());
        
        for (OutboxEventJpaEntity event : pendingEvents) {
            try {
                TransactionEventDto eventDto = objectMapper.readValue(
                    event.getPayload(), 
                    TransactionEventDto.class
                );
                
                kafkaTemplate.send(
                    event.getEventType(), 
                    event.getAggregateId(), 
                    eventDto
                ).get(); // Wait for acknowledgment
                
                event.markAsPublished();
                outboxEventRepository.save(event);
                
                log.debug("Published outbox event: id={}, type={}", 
                    event.getId(), event.getEventType());
                
            } catch (Exception e) {
                log.error("Failed to publish outbox event: id={}", event.getId(), e);
                // Event will be retried on next poll
            }
        }
    }
}
