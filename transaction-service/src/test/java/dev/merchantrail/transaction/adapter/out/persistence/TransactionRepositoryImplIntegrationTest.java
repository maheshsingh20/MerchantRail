package dev.merchantrail.transaction.adapter.out.persistence;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.domain.IdempotencyKey;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TransactionRepositoryImpl.class)
@DisplayName("TransactionRepositoryImpl Integration Test")
class TransactionRepositoryImplIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private TransactionRepositoryImpl transactionRepository;
    
    @Test
    @DisplayName("should save and retrieve transaction")
    void shouldSaveAndRetrieveTransaction() {
        // Given
        Transaction transaction = Transaction.create(
            MerchantId.of("MERCH001"),
            Money.of(new BigDecimal("100.00"), "USD"),
            IdempotencyKey.of("save-test-key")
        );
        
        // When
        Transaction saved = transactionRepository.save(transaction);
        Optional<Transaction> retrieved = transactionRepository.findById(saved.getTransactionId());
        
        // Then
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTransactionId()).isEqualTo(saved.getTransactionId());
        assertThat(retrieved.get().getMerchantId()).isEqualTo(transaction.getMerchantId());
        assertThat(retrieved.get().getAmount()).isEqualTo(transaction.getAmount());
        assertThat(retrieved.get().getStatus()).isEqualTo(TransactionStatus.PENDING);
    }
    
    @Test
    @DisplayName("should update existing transaction")
    void shouldUpdateExistingTransaction() {
        // Given
        Transaction transaction = Transaction.create(
            MerchantId.of("MERCH001"),
            Money.of(new BigDecimal("100.00"), "USD"),
            IdempotencyKey.of("update-test-key")
        );
        Transaction saved = transactionRepository.save(transaction);
        
        // When
        saved.approve();
        Transaction updated = transactionRepository.save(saved);
        
        // Then
        Optional<Transaction> retrieved = transactionRepository.findById(updated.getTransactionId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getStatus()).isEqualTo(TransactionStatus.APPROVED);
    }
    
    @Test
    @DisplayName("should find all transactions by merchant ID")
    void shouldFindAllTransactionsByMerchantId() {
        // Given
        MerchantId merchantId = MerchantId.of("MERCH002");
        
        Transaction t1 = Transaction.create(
            merchantId,
            Money.of(new BigDecimal("100.00"), "USD"),
            IdempotencyKey.of("merchant-test-key-1")
        );
        Transaction t2 = Transaction.create(
            merchantId,
            Money.of(new BigDecimal("200.00"), "USD"),
            IdempotencyKey.of("merchant-test-key-2")
        );
        Transaction t3 = Transaction.create(
            MerchantId.of("MERCH003"),
            Money.of(new BigDecimal("300.00"), "USD"),
            IdempotencyKey.of("merchant-test-key-3")
        );
        
        transactionRepository.save(t1);
        transactionRepository.save(t2);
        transactionRepository.save(t3);
        
        // When
        List<Transaction> merchantTransactions = transactionRepository.findByMerchantId(merchantId);
        
        // Then
        assertThat(merchantTransactions).hasSize(2);
        assertThat(merchantTransactions)
            .extracting(Transaction::getMerchantId)
            .containsOnly(merchantId);
    }
    
    @Test
    @DisplayName("should return empty when transaction not found")
    void shouldReturnEmptyWhenTransactionNotFound() {
        // When
        Optional<Transaction> result = transactionRepository.findById(TransactionId.generate());
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    @DisplayName("should return empty list when no transactions for merchant")
    void shouldReturnEmptyListWhenNoTransactionsForMerchant() {
        // When
        List<Transaction> result = transactionRepository.findByMerchantId(MerchantId.of("NONEXIST"));
        
        // Then
        assertThat(result).isEmpty();
    }
}
