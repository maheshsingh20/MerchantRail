package dev.merchantrail.fraud.domain;

import dev.merchantrail.shared.TransactionId;

import java.util.Objects;

/**
 * Result of a fraud check evaluation.
 */
public class FraudCheckResult {
    
    private final TransactionId transactionId;
    private final int riskScore; // 0-100
    private final Decision decision;
    private final String reason;
    
    private FraudCheckResult(TransactionId transactionId, int riskScore, Decision decision, String reason) {
        this.transactionId = transactionId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.reason = reason;
    }
    
    public static FraudCheckResult approved(TransactionId transactionId, int riskScore) {
        return new FraudCheckResult(transactionId, riskScore, Decision.APPROVED, "Low risk");
    }
    
    public static FraudCheckResult rejected(TransactionId transactionId, int riskScore, String reason) {
        return new FraudCheckResult(transactionId, riskScore, Decision.REJECTED, reason);
    }
    
    public static FraudCheckResult manualReview(TransactionId transactionId, int riskScore, String reason) {
        return new FraudCheckResult(transactionId, riskScore, Decision.MANUAL_REVIEW, reason);
    }
    
    public boolean isApproved() {
        return decision == Decision.APPROVED;
    }
    
    public boolean isRejected() {
        return decision == Decision.REJECTED;
    }
    
    public boolean requiresManualReview() {
        return decision == Decision.MANUAL_REVIEW;
    }
    
    public TransactionId getTransactionId() {
        return transactionId;
    }
    
    public int getRiskScore() {
        return riskScore;
    }
    
    public Decision getDecision() {
        return decision;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FraudCheckResult that = (FraudCheckResult) o;
        return transactionId.equals(that.transactionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(transactionId);
    }
    
    public enum Decision {
        APPROVED,
        REJECTED,
        MANUAL_REVIEW
    }
}
