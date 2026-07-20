package dev.merchantrail.transaction.adapter.out.persistence;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.domain.IdempotencyKey;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity for transaction persistence.
 * Separate from domain entity to maintain clean architecture.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class TransactionJpaEntity {
    
    @Id
    @Column(name = "transaction_id", length = 16, nullable = false)
    private String transactionId;
    
    @Column(name = "merchant_id", length = 8, nullable = false)
    private String merchantId;
    
    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;
    
    @Column(name = "idempotency_key", length = 255, nullable = false, unique = true)
    private String idempotencyKey;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TransactionStatus status;
    
    @Column(name = "status_reason", length = 500)
    private String statusReason;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @Version
    @Column(name = "version")
    private Long version;
    
    // Default constructor for JPA
    protected TransactionJpaEntity() {}
    
    /**
     * Converts domain entity to JPA entity.
     */
    public static TransactionJpaEntity fromDomain(Transaction transaction) {
        TransactionJpaEntity entity = new TransactionJpaEntity();
        entity.transactionId = transaction.getTransactionId().getValue();
        entity.merchantId = transaction.getMerchantId().getValue();
        entity.amount = transaction.getAmount().getAmount();
        entity.currency = transaction.getAmount().getCurrencyCode();
        entity.idempotencyKey = transaction.getIdempotencyKey().getValue();
        entity.status = transaction.getStatus();
        entity.statusReason = transaction.getStatusReason();
        entity.createdAt = transaction.getCreatedAt();
        entity.updatedAt = transaction.getUpdatedAt();
        return entity;
    }
    
    /**
     * Converts JPA entity to domain entity.
     */
    public Transaction toDomain() {
        return Transaction.reconstitute(
            TransactionId.of(transactionId),
            MerchantId.of(merchantId),
            Money.of(amount, currency),
            IdempotencyKey.of(idempotencyKey),
            status,
            statusReason,
            createdAt,
            updatedAt
        );
    }
    
    // Getters and setters
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public String getMerchantId() {
        return merchantId;
    }
    
    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public TransactionStatus getStatus() {
        return status;
    }
    
    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
    
    public String getStatusReason() {
        return statusReason;
    }
    
    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    public void setVersion(Long version) {
        this.version = version;
    }
}
