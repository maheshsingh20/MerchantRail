package dev.merchantrail.transaction.application.usecase;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.application.port.in.SubmitTransactionCommand;
import dev.merchantrail.transaction.application.port.out.EventPublisher;
import dev.merchantrail.transaction.application.port.out.IdempotencyService;
import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import dev.merchantrail.transaction.domain.IdempotencyKey;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubmitTransactionUseCase")
class SubmitTransactionUseCaseTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private IdempotencyService idempotencyService;
    
    @Mock
    private EventPublisher eventPublisher;
    
    private SubmitTransactionUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new SubmitTransactionUseCase(transactionRepository, idempotencyService, eventPublisher);
    }
    
    @Test
    @DisplayName("should create new transaction when idempotency key does not exist")
    void shouldCreateNewTransactionWhenIdempotencyKeyDoesNotExist() {
        // Given
        MerchantId merchantId = MerchantId.of("MERCH001");
        Money amount = Money.of(new BigDecimal("100.00"), "USD");
        IdempotencyKey key = IdempotencyKey.of("unique-key-123");
        SubmitTransactionCommand command = new SubmitTransactionCommand(merchantId, amount, key);
        
        when(idempotencyService.getTransactionId(key)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        
        // When
        Transaction result = useCase.execute(command);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMerchantId()).isEqualTo(merchantId);
        assertThat(result.getAmount()).isEqualTo(amount);
        assertThat(result.getIdempotencyKey()).isEqualTo(key);
        assertThat(result.getStatus()).isEqualTo(TransactionStatus.PENDING);
        
        verify(transactionRepository).save(any(Transaction.class));
        verify(idempotencyService).store(eq(key), any(TransactionId.class));
        verify(eventPublisher).publishTransactionInitiated(any(Transaction.class));
    }
    
    @Test
    @DisplayName("should return existing transaction when idempotency key already exists")
    void shouldReturnExistingTransactionWhenIdempotencyKeyAlreadyExists() {
        // Given
        MerchantId merchantId = MerchantId.of("MERCH001");
        Money amount = Money.of(new BigDecimal("100.00"), "USD");
        IdempotencyKey key = IdempotencyKey.of("existing-key-456");
        TransactionId existingTxnId = TransactionId.generate();
        
        Transaction existingTransaction = Transaction.reconstitute(
            existingTxnId,
            merchantId,
            amount,
            key,
            TransactionStatus.PENDING,
            null,
            java.time.Instant.now(),
            java.time.Instant.now()
        );
        
        SubmitTransactionCommand command = new SubmitTransactionCommand(merchantId, amount, key);
        
        when(idempotencyService.getTransactionId(key)).thenReturn(Optional.of(existingTxnId));
        when(transactionRepository.findById(existingTxnId)).thenReturn(Optional.of(existingTransaction));
        
        // When
        Transaction result = useCase.execute(command);
        
        // Then
        assertThat(result).isEqualTo(existingTransaction);
        
        verify(idempotencyService).getTransactionId(key);
        verify(transactionRepository).findById(existingTxnId);
        verify(transactionRepository, never()).save(any());
        verify(eventPublisher, never()).publishTransactionInitiated(any());
    }
    
    @Test
    @DisplayName("should throw exception when idempotency key exists but transaction not found")
    void shouldThrowExceptionWhenIdempotencyKeyExistsButTransactionNotFound() {
        // Given
        MerchantId merchantId = MerchantId.of("MERCH001");
        Money amount = Money.of(new BigDecimal("100.00"), "USD");
        IdempotencyKey key = IdempotencyKey.of("orphaned-key-789");
        TransactionId orphanedTxnId = TransactionId.generate();
        
        SubmitTransactionCommand command = new SubmitTransactionCommand(merchantId, amount, key);
        
        when(idempotencyService.getTransactionId(key)).thenReturn(Optional.of(orphanedTxnId));
        when(transactionRepository.findById(orphanedTxnId)).thenReturn(Optional.empty());
        
        // When / Then
        assertThatThrownBy(() -> useCase.execute(command))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Idempotency key exists but transaction not found");
    }
    
    @Test
    @DisplayName("should publish transaction initiated event with correct transaction")
    void shouldPublishTransactionInitiatedEventWithCorrectTransaction() {
        // Given
        MerchantId merchantId = MerchantId.of("MERCH001");
        Money amount = Money.of(new BigDecimal("250.50"), "USD");
        IdempotencyKey key = IdempotencyKey.of("event-test-key");
        SubmitTransactionCommand command = new SubmitTransactionCommand(merchantId, amount, key);
        
        when(idempotencyService.getTransactionId(key)).thenReturn(Optional.empty());
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));
        
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        
        // When
        useCase.execute(command);
        
        // Then
        verify(eventPublisher).publishTransactionInitiated(transactionCaptor.capture());
        Transaction publishedTransaction = transactionCaptor.getValue();
        
        assertThat(publishedTransaction.getMerchantId()).isEqualTo(merchantId);
        assertThat(publishedTransaction.getAmount()).isEqualTo(amount);
        assertThat(publishedTransaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
    }
}
