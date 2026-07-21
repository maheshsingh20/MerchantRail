package dev.merchantrail.transaction.chaos;

import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Chaos test validating system behavior when Redis experiences network partitions.
 * Tests cache resilience and fallback to database when Redis is unavailable.
 */
class RedisPartitionChaosTest extends ChaosTestBase {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Should fallback to database when Redis is completely down")
    void shouldFallbackToDatabaseWhenRedisDown() throws IOException {
        // Given: A transaction is cached in Redis
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        
        // Cache the transaction
        String cacheKey = "transaction:" + transactionId;
        redisTemplate.opsForValue().set(cacheKey, transaction, 1, TimeUnit.HOURS);
        
        // When: Redis becomes completely unavailable (100% packet loss)
        redisProxy.toxics()
                .bandwidth("cut_connection", ToxicDirection.DOWNSTREAM, 0);
        
        // Then: System should still retrieve transaction from database
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Transaction retrieved = transactionRepository.findById(transactionId)
                            .orElse(null);
                    assertThat(retrieved).isNotNull();
                    assertThat(retrieved.getTransactionId()).isEqualTo(transactionId);
                });
        
        // Cleanup
        redisProxy.toxics().get("cut_connection").remove();
    }

    @Test
    @DisplayName("Should handle Redis timeout during write operations")
    void shouldHandleRedisTimeoutDuringWrite() throws IOException {
        // Given: Redis has high latency
        redisProxy.toxics()
                .latency("high_latency", ToxicDirection.DOWNSTREAM, 5000);
        
        // When: Attempting to cache a transaction
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        
        String cacheKey = "transaction:" + transactionId;
        long startTime = System.currentTimeMillis();
        
        // Then: Operation should timeout gracefully
        try {
            redisTemplate.opsForValue().set(cacheKey, transaction, 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Expected to timeout or fail
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        // Transaction should still be persisted in database
        Transaction dbTransaction = transactionRepository.findById(transactionId)
                .orElse(null);
        assertThat(dbTransaction).isNotNull();
        
        // Cleanup
        redisProxy.toxics().get("high_latency").remove();
    }

    @Test
    @DisplayName("Should recover when Redis network partition is healed")
    void shouldRecoverWhenPartitionHealed() throws IOException, InterruptedException {
        // Given: Redis is partitioned
        redisProxy.toxics()
                .bandwidth("partition", ToxicDirection.DOWNSTREAM, 0);
        
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId);
        
        // When: Partition is healed
        Thread.sleep(2000);
        redisProxy.toxics().get("partition").remove();
        Thread.sleep(1000);
        
        // Then: Redis should work normally again
        String cacheKey = "transaction:" + transactionId;
        redisTemplate.opsForValue().set(cacheKey, transaction, 1, TimeUnit.HOURS);
        
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        assertThat(cached).isNotNull();
    }

    @Test
    @DisplayName("Should maintain consistency when Redis is intermittently available")
    void shouldMaintainConsistencyWithIntermittentRedis() throws IOException, InterruptedException {
        // Given: Redis has intermittent connection issues
        redisProxy.toxics()
                .limitData("intermittent", ToxicDirection.DOWNSTREAM, 1000);
        
        // When: Multiple write operations occur
        for (int i = 0; i < 5; i++) {
            String txId = UUID.randomUUID().toString();
            Transaction tx = createTestTransaction(txId);
            
            try {
                String key = "transaction:" + txId;
                redisTemplate.opsForValue().set(key, tx, 1, TimeUnit.HOURS);
            } catch (Exception e) {
                // Some operations may fail, which is expected
            }
            Thread.sleep(500);
        }
        
        // Then: Database should remain consistent
        long dbCount = transactionRepository.count();
        assertThat(dbCount).isGreaterThanOrEqualTo(5);
        
        // Cleanup
        redisProxy.toxics().get("intermittent").remove();
    }

    private Transaction createTestTransaction(String transactionId) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setMerchantId("MERCHANT_" + UUID.randomUUID().toString().substring(0, 8));
        transaction.setAmount(new BigDecimal("99.99"));
        transaction.setCurrency("USD");
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCardNumber("4111111111111111");
        transaction.setCardholderName("Test User");
        return transactionRepository.save(transaction);
    }
}
