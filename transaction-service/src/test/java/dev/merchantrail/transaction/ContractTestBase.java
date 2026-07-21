package dev.merchantrail.transaction;

import dev.merchantrail.transaction.adapter.in.web.TransactionController;
import dev.merchantrail.transaction.application.port.in.SubmitTransactionUseCase;
import dev.merchantrail.transaction.application.port.in.GetTransactionUseCase;
import dev.merchantrail.transaction.application.port.in.ListTransactionsUseCase;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Base class for Spring Cloud Contract tests.
 * Provides common setup and mocking for contract verification.
 */
@SpringJUnitConfig
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class ContractTestBase {

    @Mock
    private SubmitTransactionUseCase submitTransactionUseCase;

    @Mock
    private GetTransactionUseCase getTransactionUseCase;

    @Mock
    private ListTransactionsUseCase listTransactionsUseCase;

    @BeforeEach
    public void setup() {
        TransactionController controller = new TransactionController(
                submitTransactionUseCase,
                getTransactionUseCase,
                listTransactionsUseCase
        );
        
        RestAssuredMockMvc.standaloneSetup(controller);
        
        // Mock submit transaction
        when(submitTransactionUseCase.submitTransaction(any())).thenAnswer(invocation -> {
            Transaction tx = new Transaction();
            tx.setTransactionId("TX_" + UUID.randomUUID().toString());
            tx.setMerchantId("MERCHANT_12345");
            tx.setAmount(new BigDecimal("99.99"));
            tx.setCurrency("USD");
            tx.setStatus(TransactionStatus.PENDING);
            tx.setCreatedAt(Instant.now());
            return tx;
        });
        
        // Mock get transaction
        when(getTransactionUseCase.getTransaction(anyString())).thenAnswer(invocation -> {
            String txId = invocation.getArgument(0);
            Transaction tx = new Transaction();
            tx.setTransactionId(txId);
            tx.setMerchantId("MERCHANT_12345");
            tx.setAmount(new BigDecimal("99.99"));
            tx.setCurrency("USD");
            tx.setStatus(TransactionStatus.APPROVED);
            tx.setCardLast4("1111");
            tx.setCardholderName("John Doe");
            tx.setCreatedAt(Instant.parse("2024-01-01T12:00:00Z"));
            tx.setUpdatedAt(Instant.parse("2024-01-01T12:01:00Z"));
            return Optional.of(tx);
        });
        
        // Mock list transactions
        when(listTransactionsUseCase.listTransactions(any(), any())).thenAnswer(invocation -> {
            Transaction tx1 = new Transaction();
            tx1.setTransactionId("TX_001");
            tx1.setMerchantId("MERCHANT_12345");
            tx1.setAmount(new BigDecimal("99.99"));
            tx1.setCurrency("USD");
            tx1.setStatus(TransactionStatus.APPROVED);
            tx1.setCreatedAt(Instant.parse("2024-01-01T12:00:00Z"));
            
            Transaction tx2 = new Transaction();
            tx2.setTransactionId("TX_002");
            tx2.setMerchantId("MERCHANT_12345");
            tx2.setAmount(new BigDecimal("149.99"));
            tx2.setCurrency("USD");
            tx2.setStatus(TransactionStatus.PENDING);
            tx2.setCreatedAt(Instant.parse("2024-01-01T13:00:00Z"));
            
            return new PageImpl<>(Arrays.asList(tx1, tx2), PageRequest.of(0, 10), 2);
        });
    }
}
