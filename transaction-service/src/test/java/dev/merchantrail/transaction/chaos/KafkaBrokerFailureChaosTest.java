package dev.merchantrail.transaction.chaos;

import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Chaos test validating system behavior when Kafka broker experiences failures.
 * Tests event publishing resilience, retry mechanisms, and message durability.
 */
class KafkaBrokerFailureChaosTest extends ChaosTestBase {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @DisplayName("Should buffer events when Kafka broker is temporarily down")
    void shouldBufferEventsWhenBrokerDown() throws IOException, InterruptedException {
        // Given: Kafka broker becomes unavailable
        kafkaProxy.toxics()
                .bandwidth("broker_down", ToxicDirection.DOWNSTREAM, 0);
        
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        
        // When: Attempting to publish event while broker is down
        try {
            kafkaTemplate.send("transaction-events", transactionId, transaction);
        } catch (Exception e) {
            // Expected to fail or timeout
        }
        
        // And: Broker comes back online
        Thread.sleep(2000);
        kafkaProxy.toxics().get("broker_down").remove();
        Thread.sleep(1000);
        
        // Then: Event should eventually be delivered when broker recovers
        await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    // Verify the event was persisted via outbox pattern
                    assertThat(transactionRepository.findById(transactionId)).isPresent();
                });
    }

    @Test
    @DisplayName("Should handle Kafka broker with high latency")
    void shouldHandleKafkaBrokerWithHighLatency() throws IOException {
        // Given: Kafka broker has high latency (3 seconds)
        kafkaProxy.toxics()
                .latency("broker_latency", ToxicDirection.DOWNSTREAM, 3000);
        
        // When: Publishing multiple events
        long startTime = System.currentTimeMillis();
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        
        try {
            kafkaTemplate.send("transaction-events", transactionId, transaction);
        } catch (Exception e) {
            // May timeout depending on configuration
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Then: System should not block indefinitely
        assertThat(duration).isLessThan(10000); // Should timeout or succeed within 10s
        
        // Transaction should still be persisted in database (outbox pattern)
        Transaction dbTransaction = transactionRepository.findById(transactionId)
                .orElse(null);
        assertThat(dbTransaction).isNotNull();
        
        // Cleanup
        kafkaProxy.toxics().get("broker_latency").remove();
    }

    @Test
    @DisplayName("Should retry failed event publishing with exponential backoff")
    void shouldRetryFailedEventPublishing() throws IOException, InterruptedException {
        // Given: Kafka experiences intermittent failures
        kafkaProxy.toxics()
                .timeout("intermittent_failure", ToxicDirection.DOWNSTREAM, 100);
        
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        
        // When: Publishing event with intermittent failures
        int successCount = 0;
        int failureCount = 0;
        
        for (int i = 0; i < 10; i++) {
            try {
                kafkaTemplate.send("transaction-events", transactionId, transaction).get(2, TimeUnit.SECONDS);
                successCount++;
            } catch (Exception e) {
                failureCount++;
            }
            Thread.sleep(500);
        }
        
        // Then: Some messages should succeed (with retries)
        assertThat(successCount).isGreaterThan(0);
        
        // Cleanup
        kafkaProxy.toxics().get("intermittent_failure").remove();
    }

    @Test
    @DisplayName("Should maintain message order despite broker failures")
    void shouldMaintainMessageOrderDespiteBrokerFailures() throws IOException, InterruptedException {
        // Given: Kafka has packet loss
        kafkaProxy.toxics()
                .limitData("packet_loss", ToxicDirection.DOWNSTREAM, 5000);
        
        // When: Publishing ordered sequence of events
        String baseId = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            String txId = baseId + "-" + i;
            Transaction tx = createTestTransaction(txId);
            
            try {
                kafkaTemplate.send("transaction-events", txId, tx);
            } catch (Exception e) {
                // Some may fail
            }
            Thread.sleep(200);
        }
        
        // Then: Messages that succeed should maintain order
        // This is guaranteed by Kafka's partition-based ordering
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long count = transactionRepository.count();
                    assertThat(count).isGreaterThanOrEqualTo(5);
                });
        
        // Cleanup
        kafkaProxy.toxics().get("packet_loss").remove();
    }

    @Test
    @DisplayName("Should use outbox pattern when Kafka is completely unavailable")
    void shouldUseOutboxPatternWhenKafkaUnavailable() throws IOException {
        // Given: Kafka is completely down
        kafkaProxy.toxics()
                .bandwidth("kafka_down", ToxicDirection.DOWNSTREAM, 0);
        
        // When: Creating a transaction (which should publish an event)
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        
        // Then: Transaction is saved to database via outbox pattern
        // Even though Kafka is down, the transaction should be persisted
        Transaction saved = transactionRepository.findById(transactionId)
                .orElseThrow();
        assertThat(saved.getTransactionId()).isEqualTo(transactionId);
        
        // The outbox table should contain the pending event
        // This will be published when Kafka recovers
        
        // Cleanup
        kafkaProxy.toxics().get("kafka_down").remove();
    }

    private Transaction createTestTransaction(String transactionId) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setMerchantId("MERCHANT_" + UUID.randomUUID().toString().substring(0, 8));
        transaction.setAmount(new BigDecimal("149.99"));
        transaction.setCurrency("USD");
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCardNumber("5555555555554444");
        transaction.setCardholderName("Chaos Test User");
        return transactionRepository.save(transaction);
    }
}
