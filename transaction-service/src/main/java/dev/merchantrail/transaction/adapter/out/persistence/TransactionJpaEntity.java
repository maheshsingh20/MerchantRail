package dev.merchantrail.transaction.adapter.out.persistence;

import dev.merchantrail.shared.CardBrand;
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
 * JPA entity for transaction persistence with network switching metadata.
 */
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_card_bin", columnList = "card_bin"),
    @Index(name = "idx_is_stip", columnList = "is_stip")
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

    // Switching & Network Columns
    @Column(name = "card_pan", length = 32)
    private String cardPan;

    @Column(name = "card_bin", length = 8)
    private String cardBin;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_brand", length = 20)
    private CardBrand cardBrand;

    @Column(name = "routed_issuer_id", length = 50)
    private String routedIssuerId;

    @Column(name = "is_stip", nullable = false)
    private boolean isStip = false;

    @Column(name = "auth_code", length = 20)
    private String authCode;

    @Column(name = "interchange_fee", precision = 19, scale = 4)
    private BigDecimal interchangeFee;

    @Column(name = "switch_fee", precision = 19, scale = 4)
    private BigDecimal switchFee;
    
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
        
        entity.cardPan = transaction.getMaskedPan();
        entity.cardBin = transaction.getCardBin();
        entity.cardBrand = transaction.getCardBrand();
        entity.routedIssuerId = transaction.getRoutedIssuerId();
        entity.isStip = transaction.isStip();
        entity.authCode = transaction.getAuthCode();
        entity.interchangeFee = transaction.getInterchangeFee() != null ? transaction.getInterchangeFee().getAmount() : null;
        entity.switchFee = transaction.getSwitchFee() != null ? transaction.getSwitchFee().getAmount() : null;

        return entity;
    }
    
    /**
     * Converts JPA entity to domain entity.
     */
    public Transaction toDomain() {
        Money interchange = interchangeFee != null ? Money.of(interchangeFee, currency) : null;
        Money switchMoney = switchFee != null ? Money.of(switchFee, currency) : null;

        return Transaction.reconstitute(
            TransactionId.of(transactionId),
            MerchantId.of(merchantId),
            Money.of(amount, currency),
            IdempotencyKey.of(idempotencyKey),
            status,
            statusReason,
            createdAt,
            updatedAt,
            cardPan,
            cardBin,
            cardBrand,
            routedIssuerId,
            isStip,
            authCode,
            interchange,
            switchMoney
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

    public String getCardPan() {
        return cardPan;
    }

    public void setCardPan(String cardPan) {
        this.cardPan = cardPan;
    }

    public String getCardBin() {
        return cardBin;
    }

    public void setCardBin(String cardBin) {
        this.cardBin = cardBin;
    }

    public CardBrand getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(CardBrand cardBrand) {
        this.cardBrand = cardBrand;
    }

    public String getRoutedIssuerId() {
        return routedIssuerId;
    }

    public void setRoutedIssuerId(String routedIssuerId) {
        this.routedIssuerId = routedIssuerId;
    }

    public boolean isStip() {
        return isStip;
    }

    public void setStip(boolean stip) {
        isStip = stip;
    }

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public BigDecimal getInterchangeFee() {
        return interchangeFee;
    }

    public void setInterchangeFee(BigDecimal interchangeFee) {
        this.interchangeFee = interchangeFee;
    }

    public BigDecimal getSwitchFee() {
        return switchFee;
    }

    public void setSwitchFee(BigDecimal switchFee) {
        this.switchFee = switchFee;
    }
}
