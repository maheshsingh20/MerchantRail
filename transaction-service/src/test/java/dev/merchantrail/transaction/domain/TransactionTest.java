package dev.merchantrail.transaction.domain;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Transaction Domain Entity")
class TransactionTest {
    
    @Nested
    @DisplayName("Creation")
    class Creation {
        
        @Test
        @DisplayName("should create transaction with valid inputs")
        void shouldCreateTransactionWithValidInputs() {
            MerchantId merchantId = MerchantId.of("MERCH001");
            Money amount = Money.of(new BigDecimal("100.00"), "USD");
            IdempotencyKey key = IdempotencyKey.generate();
            
            Transaction transaction = Transaction.create(merchantId, amount, key);
            
            assertThat(transaction.getMerchantId()).isEqualTo(merchantId);
            assertThat(transaction.getAmount()).isEqualTo(amount);
            assertThat(transaction.getIdempotencyKey()).isEqualTo(key);
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(transaction.getTransactionId()).isNotNull();
            assertThat(transaction.getCreatedAt()).isNotNull();
            assertThat(transaction.getUpdatedAt()).isNotNull();
        }
        
        @Test
        @DisplayName("should reject negative amount")
        void shouldRejectNegativeAmount() {
            MerchantId merchantId = MerchantId.of("MERCH001");
            Money amount = Money.of(new BigDecimal("-100.00"), "USD");
            IdempotencyKey key = IdempotencyKey.generate();
            
            assertThatThrownBy(() -> Transaction.create(merchantId, amount, key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
        }
        
        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            MerchantId merchantId = MerchantId.of("MERCH001");
            Money amount = Money.of(BigDecimal.ZERO, "USD");
            IdempotencyKey key = IdempotencyKey.generate();
            
            assertThatThrownBy(() -> Transaction.create(merchantId, amount, key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be positive");
        }
        
        @Test
        @DisplayName("should reject amount exceeding maximum")
        void shouldRejectAmountExceedingMaximum() {
            MerchantId merchantId = MerchantId.of("MERCH001");
            Money amount = Money.of(new BigDecimal("1000001"), "USD");
            IdempotencyKey key = IdempotencyKey.generate();
            
            assertThatThrownBy(() -> Transaction.create(merchantId, amount, key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds maximum");
        }
        
        @Test
        @DisplayName("should reject amount below minimum")
        void shouldRejectAmountBelowMinimum() {
            MerchantId merchantId = MerchantId.of("MERCH001");
            Money amount = Money.of(new BigDecimal("0.001"), "USD");
            IdempotencyKey key = IdempotencyKey.generate();
            
            assertThatThrownBy(() -> Transaction.create(merchantId, amount, key))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("below minimum");
        }
    }
    
    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {
        
        @Test
        @DisplayName("should transition from PENDING to APPROVED")
        void shouldTransitionFromPendingToApproved() {
            Transaction transaction = createPendingTransaction();
            
            transaction.approve();
            
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.APPROVED);
        }
        
        @Test
        @DisplayName("should transition from PENDING to REJECTED")
        void shouldTransitionFromPendingToRejected() {
            Transaction transaction = createPendingTransaction();
            
            transaction.reject("Fraud detected");
            
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REJECTED);
            assertThat(transaction.getStatusReason()).isEqualTo("Fraud detected");
        }
        
        @Test
        @DisplayName("should transition from APPROVED to SETTLED")
        void shouldTransitionFromApprovedToSettled() {
            Transaction transaction = createApprovedTransaction();
            
            transaction.settle();
            
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.SETTLED);
        }
        
        @Test
        @DisplayName("should transition from PENDING to REVERSED")
        void shouldTransitionFromPendingToReversed() {
            Transaction transaction = createPendingTransaction();
            
            transaction.reverse("Bank timeout");
            
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REVERSED);
            assertThat(transaction.getStatusReason()).isEqualTo("Bank timeout");
        }
        
        @Test
        @DisplayName("should transition from APPROVED to REVERSED")
        void shouldTransitionFromApprovedToReversed() {
            Transaction transaction = createApprovedTransaction();
            
            transaction.reverse("System error");
            
            assertThat(transaction.getStatus()).isEqualTo(TransactionStatus.REVERSED);
        }
        
        @Test
        @DisplayName("should not allow approve from non-PENDING status")
        void shouldNotAllowApproveFromNonPendingStatus() {
            Transaction transaction = createApprovedTransaction();
            
            assertThatThrownBy(transaction::approve)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot approve");
        }
        
        @Test
        @DisplayName("should not allow reject from non-PENDING status")
        void shouldNotAllowRejectFromNonPendingStatus() {
            Transaction transaction = createApprovedTransaction();
            
            assertThatThrownBy(() -> transaction.reject("reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reject");
        }
        
        @Test
        @DisplayName("should not allow settle from non-APPROVED status")
        void shouldNotAllowSettleFromNonApprovedStatus() {
            Transaction transaction = createPendingTransaction();
            
            assertThatThrownBy(transaction::settle)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot settle");
        }
        
        @Test
        @DisplayName("should not allow reverse from REJECTED status")
        void shouldNotAllowReverseFromRejectedStatus() {
            Transaction transaction = createPendingTransaction();
            transaction.reject("Fraud");
            
            assertThatThrownBy(() -> transaction.reverse("reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reverse");
        }
        
        @Test
        @DisplayName("should not allow reverse from REVERSED status")
        void shouldNotAllowReverseFromReversedStatus() {
            Transaction transaction = createPendingTransaction();
            transaction.reverse("First reversal");
            
            assertThatThrownBy(() -> transaction.reverse("Second reversal"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot reverse");
        }
    }
    
    @Nested
    @DisplayName("Status Checks")
    class StatusChecks {
        
        @Test
        @DisplayName("isPending should return true for PENDING status")
        void isPendingShouldReturnTrueForPendingStatus() {
            Transaction transaction = createPendingTransaction();
            assertThat(transaction.isPending()).isTrue();
        }
        
        @Test
        @DisplayName("isApproved should return true for APPROVED status")
        void isApprovedShouldReturnTrueForApprovedStatus() {
            Transaction transaction = createApprovedTransaction();
            assertThat(transaction.isApproved()).isTrue();
        }
        
        @Test
        @DisplayName("isSettled should return true for SETTLED status")
        void isSettledShouldReturnTrueForSettledStatus() {
            Transaction transaction = createSettledTransaction();
            assertThat(transaction.isSettled()).isTrue();
        }
        
        @Test
        @DisplayName("isRejected should return true for REJECTED status")
        void isRejectedShouldReturnTrueForRejectedStatus() {
            Transaction transaction = createPendingTransaction();
            transaction.reject("Fraud");
            assertThat(transaction.isRejected()).isTrue();
        }
        
        @Test
        @DisplayName("isReversed should return true for REVERSED status")
        void isReversedShouldReturnTrueForReversedStatus() {
            Transaction transaction = createPendingTransaction();
            transaction.reverse("Timeout");
            assertThat(transaction.isReversed()).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Equality")
    class Equality {
        
        @Test
        @DisplayName("should be equal when transaction IDs match")
        void shouldBeEqualWhenTransactionIdsMatch() {
            Transaction t1 = createPendingTransaction();
            Transaction t2 = Transaction.reconstitute(
                t1.getTransactionId(),
                t1.getMerchantId(),
                t1.getAmount(),
                t1.getIdempotencyKey(),
                TransactionStatus.APPROVED,
                null,
                t1.getCreatedAt(),
                t1.getUpdatedAt()
            );
            
            assertThat(t1).isEqualTo(t2);
        }
        
        @Test
        @DisplayName("should not be equal when transaction IDs differ")
        void shouldNotBeEqualWhenTransactionIdsDiffer() {
            Transaction t1 = createPendingTransaction();
            Transaction t2 = createPendingTransaction();
            
            assertThat(t1).isNotEqualTo(t2);
        }
    }
    
    // Helper methods
    private Transaction createPendingTransaction() {
        return Transaction.create(
            MerchantId.of("MERCH001"),
            Money.of(new BigDecimal("100.00"), "USD"),
            IdempotencyKey.generate()
        );
    }
    
    private Transaction createApprovedTransaction() {
        Transaction transaction = createPendingTransaction();
        transaction.approve();
        return transaction;
    }
    
    private Transaction createSettledTransaction() {
        Transaction transaction = createApprovedTransaction();
        transaction.settle();
        return transaction;
    }
}
