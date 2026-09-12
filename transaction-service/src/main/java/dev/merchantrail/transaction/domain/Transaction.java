package dev.merchantrail.transaction.domain;

import dev.merchantrail.shared.CardBrand;
import dev.merchantrail.shared.CardPan;
import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing a payment transaction within the switching network.
 * Follows state machine pattern with valid state transitions and card switching metadata.
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

    // Switching & Routing Attributes
    private String maskedPan;
    private String cardBin;
    private CardBrand cardBrand;
    private String routedIssuerId;
    private boolean isStip;
    private String authCode;
    private Money interchangeFee;
    private Money switchFee;
    
    // Private constructor - use factory methods
    private Transaction(TransactionId transactionId, MerchantId merchantId, Money amount, 
                       IdempotencyKey idempotencyKey, TransactionStatus status, 
                       String statusReason, Instant createdAt, Instant updatedAt,
                       String maskedPan, String cardBin, CardBrand cardBrand,
                       String routedIssuerId, boolean isStip, String authCode,
                       Money interchangeFee, Money switchFee) {
        this.transactionId = transactionId;
        this.merchantId = merchantId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
        this.status = status;
        this.statusReason = statusReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.maskedPan = maskedPan;
        this.cardBin = cardBin;
        this.cardBrand = cardBrand;
        this.routedIssuerId = routedIssuerId;
        this.isStip = isStip;
        this.authCode = authCode;
        this.interchangeFee = interchangeFee;
        this.switchFee = switchFee;
    }
    
    /**
     * Creates a new transaction in PENDING status without card switching details.
     */
    public static Transaction create(MerchantId merchantId, Money amount, IdempotencyKey idempotencyKey) {
        return create(merchantId, amount, idempotencyKey, null);
    }

    /**
     * Creates a new transaction with card PAN and BIN routing details.
     */
    public static Transaction create(MerchantId merchantId, Money amount, IdempotencyKey idempotencyKey, CardPan cardPan) {
        validateAmount(amount);
        
        Instant now = Instant.now();
        String masked = cardPan != null ? cardPan.getMasked() : null;
        String bin = cardPan != null ? cardPan.getBin() : null;
        CardBrand brand = cardPan != null ? cardPan.getBrand() : CardBrand.UNKNOWN;

        return new Transaction(
            TransactionId.generate(),
            merchantId,
            amount,
            idempotencyKey,
            TransactionStatus.PENDING,
            null,
            now,
            now,
            masked,
            bin,
            brand,
            null,
            false,
            null,
            null,
            null
        );
    }
    
    /**
     * Reconstitutes a transaction from persistence (legacy).
     */
    public static Transaction reconstitute(TransactionId transactionId, MerchantId merchantId, 
                                          Money amount, IdempotencyKey idempotencyKey,
                                          TransactionStatus status, String statusReason,
                                          Instant createdAt, Instant updatedAt) {
        return reconstitute(transactionId, merchantId, amount, idempotencyKey, status, statusReason,
            createdAt, updatedAt, null, null, CardBrand.UNKNOWN, null, false, null, null, null);
    }

    /**
     * Reconstitutes a transaction from persistence with switching metadata.
     */
    public static Transaction reconstitute(TransactionId transactionId, MerchantId merchantId, 
                                          Money amount, IdempotencyKey idempotencyKey,
                                          TransactionStatus status, String statusReason,
                                          Instant createdAt, Instant updatedAt,
                                          String maskedPan, String cardBin, CardBrand cardBrand,
                                          String routedIssuerId, boolean isStip, String authCode,
                                          Money interchangeFee, Money switchFee) {
        return new Transaction(transactionId, merchantId, amount, idempotencyKey, 
                             status, statusReason, createdAt, updatedAt,
                             maskedPan, cardBin, cardBrand, routedIssuerId, isStip,
                             authCode, interchangeFee, switchFee);
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
     * Approves the transaction with switching resolution (issuer auth or STIP).
     */
    public void approveWithSwitching(String authCode, String routedIssuerId, boolean isStip, 
                                     Money interchangeFee, Money switchFee) {
        approve();
        this.authCode = authCode;
        this.routedIssuerId = routedIssuerId;
        this.isStip = isStip;
        this.interchangeFee = interchangeFee;
        this.switchFee = switchFee;
        if (isStip) {
            this.statusReason = "Approved via Stand-In Processing (STIP) - Issuer SLA timeout";
        }
    }
    
    /**
     * Rejects the transaction (failed fraud check, decline, or validation).
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

    public String getMaskedPan() {
        return maskedPan;
    }

    public String getCardBin() {
        return cardBin;
    }

    public CardBrand getCardBrand() {
        return cardBrand;
    }

    public String getRoutedIssuerId() {
        return routedIssuerId;
    }

    public boolean isStip() {
        return isStip;
    }

    public String getAuthCode() {
        return authCode;
    }

    public Money getInterchangeFee() {
        return interchangeFee;
    }

    public Money getSwitchFee() {
        return switchFee;
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
