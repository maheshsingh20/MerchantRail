package dev.merchantrail.transaction.application.usecase;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.transaction.application.port.in.GetTransactionsByMerchantQuery;
import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import dev.merchantrail.transaction.domain.IdempotencyKey;
import dev.merchantrail.transaction.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTransactionsByMerchantUseCase")
class GetTransactionsByMerchantUseCaseTest {
    
    @Mock
    private TransactionRepository transactionRepository;
    
    private GetTransactionsByMerchantUseCase useCase;
    
    @BeforeEach
    void setUp() {
        useCase = new GetTransactionsByMerchantUseCase(transactionRepository);
    }
    
    @Test
    @DisplayName("should return all transactions for merchant")
    void shouldReturnAllTransactionsForMerchant() {
        // Given
        MerchantId merchantId = MerchantId.of("MERCH001");
        Transaction t1 = Transaction.create(merchantId, Money.of(new BigDecimal("100"), "USD"), IdempotencyKey.generate());
        Transaction t2 = Transaction.create(merchantId, Money.of(new BigDecimal("200"), "USD"), IdempotencyKey.generate());
        
        GetTransactionsByMerchantQuery query = new GetTransactionsByMerchantQuery(merchantId);
        when(transactionRepository.findByMerchantId(merchantId)).thenReturn(Arrays.asList(t1, t2));
        
        // When
        List<Transaction> result = useCase.execute(query);
        
        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(t1, t2);
        verify(transactionRepository).findByMerchantId(merchantId);
    }
    
    @Test
    @DisplayName("should return empty list when no transactions found")
    void shouldReturnEmptyListWhenNoTransactionsFound() {
        // Given
        MerchantId merchantId = MerchantId.of("MERCH999");
        GetTransactionsByMerchantQuery query = new GetTransactionsByMerchantQuery(merchantId);
        
        when(transactionRepository.findByMerchantId(merchantId)).thenReturn(List.of());
        
        // When
        List<Transaction> result = useCase.execute(query);
        
        // Then
        assertThat(result).isEmpty();
        verify(transactionRepository).findByMerchantId(merchantId);
    }
}
