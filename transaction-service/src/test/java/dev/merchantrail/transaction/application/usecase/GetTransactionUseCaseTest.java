package dev.merchantrail.transaction.application.usecase;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.application.port.in.GetTransactionQuery;
import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import dev.merchantrail.transaction.domain.IdempotencyKey;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTransactionUseCase")
class GetTransactionUseCaseTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    private GetTransactionUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new GetTransactionUseCase(transactionRepository);
    }
    
    @Test
    @DisplayName("should return transaction when it exists")
    void shouldReturnTransactionWhenItExists() {
        // Given
        TransactionId txnId = TransactionId.generate();
        Transaction transaction = Transaction.reconstitute(
            txnId,
            MerchantId.of("MERCH001"),
            Money.of(new BigDecimal("100.00"), "USD"),
            IdempotencyKey.of("test-key"),
            TransactionStatus.PENDING,
            null,
            Instant.now(),
            Instant.now()
        );
        
        GetTransactionQuery query = new GetTransactionQuery(txnId);
        when(transactionRepository.findById(txnId)).thenReturn(Optional.of(transaction));
        
        // When
        Transaction result = useCase.execute(query);
        
        // Then
        assertThat(result).isEqualTo(transaction);
        verify(transactionRepository).findById(txnId);
    }
    
    @Test
    @DisplayName("should throw exception when transaction not found")
    void shouldThrowExceptionWhenTransactionNotFound() {
        // Given
        TransactionId txnId = TransactionId.generate();
        GetTransactionQuery query = new GetTransactionQuery(txnId);
        
        when(transactionRepository.findById(txnId)).thenReturn(Optional.empty());
        
        // When / Then
        assertThatThrownBy(() -> useCase.execute(query))
            .isInstanceOf(GetTransactionUseCase.TransactionNotFoundException.class)
            .hasMessageContaining("Transaction not found");
    }
}
