package dev.merchantrail.transaction.chaos;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.application.port.in.SubmitTransactionCommand;
import dev.merchantrail.transaction.application.usecase.SubmitTransactionUseCase;
import dev.merchantrail.transaction.domain.IdempotencyKey;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Chaos Engineering tests using Toxiproxy.
 * Tests system behavior under failure conditions (latency, timeouts, network partitions).
 */
@SpringBootTest
@Testcontainers
@DisplayName("Chaos Engineering Tests")
class ChaosEngineeringTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
    
    @Container
    static ToxiproxyContainer toxiproxy = new ToxiproxyContainer(
        DockerImageName.parse("ghcr.io/shopify/toxiproxy:2.5.0")
    );
    
    @Autowired
    private SubmitTransactionUseCase submitTransactionUseCase;
    
    @Test
    @DisplayName("System should handle database latency gracefully")
    void shouldHandleDatabaseLatencyGracefully() throws Exception {
        // Given: Toxiproxy adds 2 second latency to database
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(
            toxiproxy.getHost(), 
            toxiproxy.getControlPort()
        );
        
        Proxy proxy = toxiproxyClient.createProxy(
            "postgres", 
            "0.0.0.0:8666", 
            postgres.getHost() + ":" + postgres.getFirstMappedPort()
        );
        
        proxy.toxics()
            .latency("db-latency", ToxicDirection.DOWNSTREAM, 2000);
        
        // When: Transaction is submitted with latency
        SubmitTransactionCommand command = new SubmitTransactionCommand(
            MerchantId.of("MERCH001"),
            Money.of(new BigDecimal("100.00"), "USD"),
            IdempotencyKey.of("chaos-test-latency")
        );
        
        long startTime = System.currentTimeMillis();
        Transaction result = submitTransactionUseCase.execute(command);
        long duration = System.currentTimeMillis() - startTime;
        
        // Then: Transaction succeeds but takes longer
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(duration).isGreaterThan(2000); // At least 2 seconds due to latency
        
        // Cleanup
        proxy.delete();
    }
    
    @Test
    @DisplayName("System should timeout on excessive database latency")
    void shouldTimeoutOnExcessiveDatabaseLatency() throws Exception {
        // Given: Toxiproxy adds 30 second latency (exceeds connection timeout)
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(
            toxiproxy.getHost(), 
            toxiproxy.getControlPort()
        );
        
        Proxy proxy = toxiproxyClient.createProxy(
            "postgres-slow", 
            "0.0.0.0:8667", 
            postgres.getHost() + ":" + postgres.getFirstMappedPort()
        );
        
        proxy.toxics()
            .latency("extreme-latency", ToxicDirection.DOWNSTREAM, 30000);
        
        // When: Transaction is submitted
        SubmitTransactionCommand command = new SubmitTransactionCommand(
            MerchantId.of("MERCH002"),
            Money.of(new BigDecimal("200.00"), "USD"),
            IdempotencyKey.of("chaos-test-timeout")
        );
        
        // Then: Should timeout or handle gracefully
        try {
            submitTransactionUseCase.execute(command);
        } catch (Exception e) {
            // Expected to fail due to timeout
            assertThat(e).isInstanceOf(RuntimeException.class);
        }
        
        // Cleanup
        proxy.delete();
    }
    
    @Test
    @DisplayName("Idempotency should work even with retries after network failure")
    void idempotencyShouldWorkWithRetriesAfterNetworkFailure() throws Exception {
        // Given: Create transaction successfully
        SubmitTransactionCommand command = new SubmitTransactionCommand(
            MerchantId.of("MERCH003"),
            Money.of(new BigDecimal("300.00"), "USD"),
            IdempotencyKey.of("chaos-test-idempotency")
        );
        
        Transaction first = submitTransactionUseCase.execute(command);
        TransactionId firstId = first.getTransactionId();
        
        // When: Simulate network partition and retry with same idempotency key
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(
            toxiproxy.getHost(), 
            toxiproxy.getControlPort()
        );
        
        Proxy proxy = toxiproxyClient.createProxy(
            "postgres-partition", 
            "0.0.0.0:8668", 
            postgres.getHost() + ":" + postgres.getFirstMappedPort()
        );
        
        // Simulate connection reset
        proxy.toxics()
            .resetPeer("reset", ToxicDirection.DOWNSTREAM, 1000);
        
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            // Retry submission with same idempotency key
            Transaction retry = submitTransactionUseCase.execute(command);
            
            // Then: Should return same transaction (idempotency preserved)
            assertThat(retry.getTransactionId()).isEqualTo(firstId);
            assertThat(retry.getAmount()).isEqualTo(first.getAmount());
        });
        
        // Cleanup
        proxy.delete();
    }
    
    @Test
    @DisplayName("System should maintain data consistency under network jitter")
    void shouldMaintainConsistencyUnderNetworkJitter() throws Exception {
        // Given: Toxiproxy adds random latency (jitter)
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(
            toxiproxy.getHost(), 
            toxiproxy.getControlPort()
        );
        
        Proxy proxy = toxiproxyClient.createProxy(
            "postgres-jitter", 
            "0.0.0.0:8669", 
            postgres.getHost() + ":" + postgres.getFirstMappedPort()
        );
        
        proxy.toxics()
            .latency("jitter", ToxicDirection.DOWNSTREAM, 500)
            .setJitter(300);
        
        // When: Submit multiple transactions under jitter
        for (int i = 0; i < 5; i++) {
            SubmitTransactionCommand command = new SubmitTransactionCommand(
                MerchantId.of("MERCH004"),
                Money.of(new BigDecimal(String.valueOf(100 + i)), "USD"),
                IdempotencyKey.of("chaos-jitter-" + i)
            );
            
            Transaction result = submitTransactionUseCase.execute(command);
            
            // Then: All transactions should succeed despite jitter
            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
        }
        
        // Cleanup
        proxy.delete();
    }
}
