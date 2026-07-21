package dev.merchantrail.transaction.chaos;

import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Chaos tests for database latency scenarios.
 * 
 * Tests system behavior when database operations are slow but not failing.
 */
class DatabaseLatencyChaosTest extends ChaosTestBase {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldHandleModerateLatencyGracefully() throws Exception {
        // Given: 500ms database latency
        injectDatabaseLatency(500);

        // When: Submit transaction
        var request = Map.of(
            "merchantId", "MERCH001",
            "amount", 100.50,
            "currency", "USD",
            "idempotencyKey", "chaos-test-1"
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "/api/v1/transactions", 
            request, 
            Map.class
        );

        // Then: Transaction should succeed despite latency
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsKey("transactionId");
        assertThat(response.getBody().get("status")).isEqualTo("PENDING");
    }

    @Test
    void shouldMaintainIdempotencyUnderLatency() throws Exception {
        // Given: High database latency
        injectDatabaseLatency(2000);

        var request = Map.of(
            "merchantId", "MERCH002",
            "amount", 200.00,
            "currency", "USD",
            "idempotencyKey", "chaos-idempotency-test"
        );

        // When: Submit same transaction twice
        ResponseEntity<Map> response1 = restTemplate.postForEntity(
            "/api/v1/transactions", 
            request, 
            Map.class
        );

        ResponseEntity<Map> response2 = restTemplate.postForEntity(
            "/api/v1/transactions", 
            request, 
            Map.class
        );

        // Then: Both should succeed and return same transaction
        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response1.getBody().get("transactionId"))
            .isEqualTo(response2.getBody().get("transactionId"));
    }

    @Test
    void shouldHandleLatencySpikes() throws Exception {
        // Given: Variable latency (jitter)
        postgresProxy.toxics()
            .latency("variable-latency", eu.rekawek.toxiproxy.model.ToxicDirection.DOWNSTREAM, 1000)
            .setJitter(800); // 200ms to 1800ms

        // When: Submit multiple transactions
        for (int i = 0; i < 5; i++) {
            var request = Map.of(
                "merchantId", "MERCH003",
                "amount", 50.00 + i,
                "currency", "USD",
                "idempotencyKey", "spike-test-" + i
            );

            ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/transactions", 
                request, 
                Map.class
            );

            // Then: All transactions should succeed despite variable latency
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    @Test
    void shouldTimeoutOnExcessiveLatency() throws Exception {
        // Given: Extreme latency (30 seconds - exceeds timeout)
        simulateDatabaseTimeout();

        var request = Map.of(
            "merchantId", "MERCH004",
            "amount", 300.00,
            "currency", "USD",
            "idempotencyKey", "timeout-test"
        );

        // When: Submit transaction
        // Then: Should timeout gracefully (circuit breaker opens)
        await().atMost(10, TimeUnit.SECONDS)
            .untilAsserted(() -> {
                try {
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                        "/api/v1/transactions", 
                        request, 
                        Map.class
                    );
                    assertThat(response.getStatusCode())
                        .isIn(HttpStatus.REQUEST_TIMEOUT, HttpStatus.SERVICE_UNAVAILABLE);
                } catch (Exception e) {
                    // Timeout exception expected
                    assertThat(e.getMessage()).contains("timeout");
                }
            });
    }
}
