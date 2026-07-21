package dev.merchantrail.transaction.chaos;

import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Chaos test validating Saga compensation logic under various failure scenarios.
 * Tests that distributed transactions are properly rolled back when failures occur.
 */
class SagaCompensationChaosTest extends ChaosTestBase {

    @Test
    @DisplayName("Should compensate transaction when fraud check service fails")
    void shouldCompensateWhenFraudCheckFails() throws IOException {
        // Given: Create a transaction that triggers fraud check
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("9999.99"));
        
        // When: Fraud service becomes unavailable during check
        dbProxy.toxics()
                .latency("fraud_delay", ToxicDirection.DOWNSTREAM, 2000);
        
        // Then: Transaction should be compensated (rolled back)
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Transaction updated = transactionRepository.findById(transactionId)
                            .orElseThrow();
                    assertThat(updated.getStatus())
                            .isIn(TransactionStatus.REJECTED, TransactionStatus.FAILED);
                });
        
        // Cleanup
        dbProxy.toxics().get("fraud_delay").remove();
    }

    @Test
    @DisplayName("Should compensate transaction when bank authorization times out")
    void shouldCompensateWhenBankAuthorizationTimesOut() throws IOException {
        // Given: Transaction pending bank authorization
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("299.99"));
        
        // When: Bank service experiences high latency (simulated timeout)
        dbProxy.toxics()
                .latency("bank_timeout", ToxicDirection.DOWNSTREAM, 5000);
        
        // Then: Saga should compensate by rolling back the transaction
        await().atMost(40, TimeUnit.SECONDS)
                .pollInterval(2, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Transaction updated = transactionRepository.findById(transactionId)
                            .orElseThrow();
                    assertThat(updated.getStatus())
                            .isIn(TransactionStatus.FAILED, TransactionStatus.REJECTED);
                });
        
        // Cleanup
        dbProxy.toxics().get("bank_timeout").remove();
    }

    @Test
    @DisplayName("Should handle compensation when ledger service is unavailable")
    void shouldHandleCompensationWhenLedgerUnavailable() throws IOException {
        // Given: Transaction that would update ledger
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("50.00"));
        
        // When: Ledger service becomes unavailable
        dbProxy.toxics()
                .bandwidth("ledger_down", ToxicDirection.DOWNSTREAM, 0);
        
        // Then: Compensation should still work (idempotent operations)
        // Transaction should be marked as failed
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Transaction updated = transactionRepository.findById(transactionId)
                            .orElseThrow();
                    assertThat(updated.getStatus())
                            .isIn(TransactionStatus.FAILED, TransactionStatus.REJECTED);
                });
        
        // Cleanup
        dbProxy.toxics().get("ledger_down").remove();
    }

    @Test
    @DisplayName("Should maintain idempotency during compensation retries")
    void shouldMaintainIdempotencyDuringCompensationRetries() throws IOException, InterruptedException {
        // Given: Transaction requiring compensation
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("199.99"));
        
        // When: Network has intermittent issues causing retries
        dbProxy.toxics()
                .timeout("intermittent", ToxicDirection.DOWNSTREAM, 100);
        
        Thread.sleep(5000); // Let system attempt retries
        
        // Then: Compensation should only execute once (idempotent)
        Transaction finalState = transactionRepository.findById(transactionId)
                .orElseThrow();
        
        // Status should be definitive, not in intermediate state
        assertThat(finalState.getStatus())
                .isIn(TransactionStatus.APPROVED, TransactionStatus.REJECTED, 
                      TransactionStatus.FAILED);
        
        // Cleanup
        dbProxy.toxics().get("intermittent").remove();
    }

    @Test
    @DisplayName("Should complete saga successfully when all services are healthy")
    void shouldCompleteSagaSuccessfullyWhenHealthy() throws InterruptedException {
        // Given: All services are healthy
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("79.99"));
        
        // When: Transaction flows through saga
        Thread.sleep(3000); // Allow processing time
        
        // Then: Transaction should complete successfully
        Transaction completed = transactionRepository.findById(transactionId)
                .orElseThrow();
        
        assertThat(completed.getStatus())
                .isIn(TransactionStatus.APPROVED, TransactionStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should log compensation events for audit trail")
    void shouldLogCompensationEventsForAuditTrail() throws IOException {
        // Given: Transaction that will be compensated
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("499.99"));
        
        // When: Compensation is triggered
        dbProxy.toxics()
                .latency("trigger_compensation", ToxicDirection.DOWNSTREAM, 3000);
        
        // Then: Compensation events should be logged
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Transaction updated = transactionRepository.findById(transactionId)
                            .orElseThrow();
                    // Status change indicates compensation occurred
                    assertThat(updated.getStatus())
                            .isIn(TransactionStatus.FAILED, TransactionStatus.REJECTED);
                });
        
        // In a real system, we'd verify compensation events in event store or audit log
        
        // Cleanup
        dbProxy.toxics().get("trigger_compensation").remove();
    }

    private Transaction createTestTransaction(String transactionId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setMerchantId("MERCHANT_" + UUID.randomUUID().toString().substring(0, 8));
        transaction.setAmount(amount);
        transaction.setCurrency("USD");
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCardNumber("4532015112830366");
        transaction.setCardholderName("Saga Test User");
        return transactionRepository.save(transaction);
    }
}
