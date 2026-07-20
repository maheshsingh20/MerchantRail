package dev.merchantrail.transaction.domain;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a payment transaction.
 * Follows state machine pattern with valid state transitions.
 */
public class Transaction {
    
    private final TransactionId transactionId;
    private final MerchantId merchantId;
    private final Money amount;
    private final IdempotencyKey idempotencyKey;
    private TransactionStatus status;
    private String statusReason;
    private final Instant createdAt;
    private Instant updatedAt;
    
    // Private constructor - use factory methods
    private Transaction(TransactionId transactionId, MerchantId merchantId, Money amount, 
                       IdempotencyKey idempotencyKey, TransactionStatus status, 
                       String statusReason, Instant createdAt, Instant updatedAt) {
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.statusReason = statusReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    /**
     * Creates a new transaction in PENDING status.
     */
    public static Transaction create(MerchantId merchantId, Money amount, IdempotencyKey idempotencyKey) {
        validateAmount(amount);
        
        Instant now = Instant.now();
        return new Transaction(
            TransactionId.generate(),
            merchantId,
            amount,
            idempotencyKey,
            TransactionStatus.PENDING,
            null,
            now,
            now
        );
    }
    
    /**
     * Reconstitutes a transaction from persistence.
     */
    public static Transaction reconstitute(TransactionId transactionId, MerchantId merchantId, 
                                          Money amount, IdempotencyKey idempotencyKey,
                                          TransactionStatus status, String statusReason,
                                          Instant createdAt, Instant updatedAt) {
        return new Transaction(transactionId, merchantId, amount, idempotencyKey, 
                             status, statusReason, createdAt, updatedAt);
    }
    
    /**
     * Approves the transaction after passing fraud checks.
     */
    public void approve() {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot approve transaction in %s status", status)
            );
        }
        this.status = TransactionStatus.APPROVED;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Rejects the transaction (failed fraud check or validation).
     */
    public void reject(String reason) {
        if (status != TransactionStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot reject transaction in %s status", status)
            );
        }
        this.status = TransactionStatus.REJECTED;
        this.statusReason = reason;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Marks transaction as settled after successful bank authorization.
     */
    public void settle() {
        if (status != TransactionStatus.APPROVED) {
            throw new IllegalStateException(
                String.format("Cannot settle transaction in %s status", status)
            );
        }
        this.status = TransactionStatus.SETTLED;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Reverses a transaction (compensating action in saga).
     */
    public void reverse(String reason) {
        if (status == TransactionStatus.REVERSED || status == TransactionStatus.REJECTED) {
            throw new IllegalStateException(
                String.format("Cannot reverse transaction in %s status", status)
            );
        }
        this.status = TransactionStatus.REVERSED;
        this.statusReason = reason;
        this.updatedAt = Instant.now();
    }
    
    /**
     * Validates amount according to business rules.
     */
    private static void validateAmount(Money amount) {
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        
        // Business rule: Maximum transaction amount is $1,000,000
        Money maxAmount = Money.of(java.math.BigDecimal.valueOf(1_000_000), amount.getCurrencyCode());
        if (amount.compareTo(maxAmount) > 0) {
            throw new IllegalArgumentException("Transaction amount exceeds maximum allowed");
        }
        
        // Business rule: Minimum transaction amount is $0.01
        Money minAmount = Money.of(java.math.BigDecimal.valueOf(0.01), amount.getCurrencyCode());
        if (amount.compareTo(minAmount) < 0) {
            throw new IllegalArgumentException("Transaction amount below minimum allowed");
        }
    }
    
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }
    
    public boolean isApproved() {
        return status == TransactionStatus.APPROVED;
    }
    
    public boolean isSettled() {
        return status == TransactionStatus.SETTLED;
    }
    
    public boolean isReversed() {
        return status == TransactionStatus.REVERSED;
    }
    
    public boolean isRejected() {
        return status == TransactionStatus.REJECTED;
    }
    
    // Getters
    public TransactionId getTransactionId() {
        return transactionId;
    }
    
    public MerchantId getMerchantId() {
        return merchantId;
    }
    
    public Money getAmount() {
        return amount;
    }
    
    public IdempotencyKey getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public TransactionStatus getStatus() {
        return status;
    }
    
    public String getStatusReason() {
        return statusReason;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return transactionId.equals(that.transactionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
}
