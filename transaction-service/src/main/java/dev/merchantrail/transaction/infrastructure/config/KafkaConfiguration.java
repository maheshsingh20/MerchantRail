package dev.merchantrail.transaction.infrastructure.config;

import dev.merchantrail.transaction.adapter.out.messaging.TransactionEventDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for event publishing.
 */
@Configuration
public class KafkaConfiguration {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, TransactionEventDto> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, TransactionEventDto> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    @Bean
    public NewTopic transactionInitiatedTopic() {
        return TopicBuilder.name("transaction.initiated")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic transactionApprovedTopic() {
        return TopicBuilder.name("transaction.approved")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic transactionSettledTopic() {
        return TopicBuilder.name("transaction.settled")
            .partitions(3)
            .replicas(1)
            .build();
    }
    
    @Bean
    public NewTopic transactionReversedTopic() {
        return TopicBuilder.name("transaction.reversed")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
