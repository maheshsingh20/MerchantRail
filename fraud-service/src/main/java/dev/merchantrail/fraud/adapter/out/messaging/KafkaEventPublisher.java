package dev.merchantrail.fraud.adapter.out.messaging;

import dev.merchantrail.fraud.application.port.out.EventPublisher;
import dev.merchantrail.fraud.domain.FraudCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventPublisher implements EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);
    
    private final KafkaTemplate<String, FraudEventDto> kafkaTemplate;
    
    public KafkaEventPublisher(KafkaTemplate<String, FraudEventDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @Override
    public void publishFraudCheckPassed(FraudCheckResult result) {
        FraudEventDto event = FraudEventDto.fromDomain(result);
        kafkaTemplate.send("fraud.passed", result.getTransactionId().getValue(), event);
        log.info("Published fraud.passed for transaction {}", result.getTransactionId());
    }
    
    @Override
    public void publishFraudCheckFailed(FraudCheckResult result) {
        FraudEventDto event = FraudEventDto.fromDomain(result);
        kafkaTemplate.send("fraud.failed", result.getTransactionId().getValue(), event);
        log.info("Published fraud.failed for transaction {}", result.getTransactionId());
    }
    
    @Override
    public void publishFraudCheckManualReview(FraudCheckResult result) {
        FraudEventDto event = FraudEventDto.fromDomain(result);
        kafkaTemplate.send("fraud.manual_review", result.getTransactionId().getValue(), event);
        log.info("Published fraud.manual_review for transaction {}", result.getTransactionId());
    }
}
