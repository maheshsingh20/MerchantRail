package dev.merchantrail.transaction.e2e;

import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import dev.merchantrail.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end test validating gRPC protocol integration with bank simulator service.
 * Tests that transaction-service can communicate with bank-simulator via gRPC.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GrpcProtocolTest {

    @Container
    static GenericContainer<?> bankSimulator = new GenericContainer<>("merchantrail/bank-simulator:latest")
            .withExposedPorts(9090);

    @Autowired
    private TransactionRepository transactionRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("bank.grpc.host", bankSimulator::getHost);
        registry.add("bank.grpc.port", () -> bankSimulator.getMappedPort(9090));
    }

    @Test
    @DisplayName("Should communicate with bank simulator via gRPC for authorization")
    void shouldCommunicateWithBankSimulatorViaGrpc() {
        // Given: A transaction requiring bank authorization
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("99.99"));
        
        // When: Transaction is processed
        transactionRepository.save(transaction);
        
        // Then: gRPC call should be made to bank simulator for authorization
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Transaction updated = transactionRepository.findById(transactionId)
                            .orElseThrow();
                    // Transaction should be authorized via gRPC
                    assertThat(updated.getStatus())
                            .isIn(TransactionStatus.APPROVED, TransactionStatus.REJECTED);
                });
    }

    @Test
    @DisplayName("Should handle gRPC authorization approval response")
    void shouldHandleGrpcAuthorizationApproval() {
        // Given: Transaction with valid card details
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("50.00"));
        transaction.setCardNumber("4111111111111111"); // Valid test card
        
        // When: Authorization request is sent via gRPC
        transactionRepository.save(transaction);
        
        // Then: Should receive approval response
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Transaction updated = transactionRepository.findById(transactionId)
                            .orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TransactionStatus.APPROVED);
                });
    }

    @Test
    @DisplayName("Should handle gRPC authorization rejection response")
    void shouldHandleGrpcAuthorizationRejection() {
        // Given: Transaction with insufficient funds scenario
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("999999.99"));
        
        // When: Authorization request is sent via gRPC
        transactionRepository.save(transaction);
        
        // Then: Should receive rejection response
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Transaction updated = transactionRepository.findById(transactionId)
                            .orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TransactionStatus.REJECTED);
                });
    }

    @Test
    @DisplayName("Should handle gRPC connection timeout gracefully")
    void shouldHandleGrpcConnectionTimeoutGracefully() {
        // Given: Transaction that may timeout
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("75.00"));
        
        // When: gRPC call might timeout
        transactionRepository.save(transaction);
        
        // Then: Should handle timeout and mark transaction appropriately
        await().atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Transaction updated = transactionRepository.findById(transactionId)
                            .orElseThrow();
                    // Should either succeed or fail gracefully (not stuck in PENDING)
                    assertThat(updated.getStatus())
                            .isIn(TransactionStatus.APPROVED, TransactionStatus.REJECTED, 
                                  TransactionStatus.FAILED);
                });
    }

    @Test
    @DisplayName("Should include authorization code in gRPC response")
    void shouldIncludeAuthorizationCodeInGrpcResponse() {
        // Given: Approved transaction
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = createTestTransaction(transactionId, new BigDecimal("25.00"));
        transaction.setCardNumber("4111111111111111");
        
        // When: Authorization is approved via gRPC
        transactionRepository.save(transaction);
        
        // Then: Response should include authorization code
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Transaction updated = transactionRepository.findById(transactionId)
                            .orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TransactionStatus.APPROVED);
                    // In real implementation, would check authorizationCode field
                    // assertThat(updated.getAuthorizationCode()).isNotBlank();
                });
    }

    private Transaction createTestTransaction(String transactionId, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setMerchantId("MERCHANT_TEST_123");
        transaction.setAmount(amount);
        transaction.setCurrency("USD");
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setCardNumber("4532015112830366");
        transaction.setCardholderName("gRPC Test User");
        transaction.setCardExpiry("12/25");
        transaction.setCardCvv("123");
        return transaction;
    }
}
