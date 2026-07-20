package dev.merchantrail.fraud.domain;

import dev.merchantrail.shared.Money;

import java.math.BigDecimal;

/**
 * Fraud detection rules.
 */
public class FraudRule {
    
    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("10000");
    private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("50000");
    private static final int VELOCITY_THRESHOLD_SECONDS = 60;
    
    /**
     * Calculates risk score based on transaction amount.
     * Returns 0-50 based on amount.
     */
    public int calculateAmountRisk(Money amount) {
        BigDecimal amountValue = amount.getAmount();
        
        if (amountValue.compareTo(VERY_HIGH_AMOUNT_THRESHOLD) >= 0) {
            return 50; // Maximum amount risk
        } else if (amountValue.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            return 30; // High amount risk
        } else if (amountValue.compareTo(new BigDecimal("1000")) >= 0) {
            return 10; // Medium amount risk
        }
        
        return 0; // Low amount risk
    }
    
    /**
     * Checks if merchant is new (high risk).
     * Returns 0 or 25.
     */
    public int calculateMerchantHistoryRisk(boolean isNewMerchant) {
        return isNewMerchant ? 25 : 0;
    }
    
    /**
     * Checks transaction velocity (multiple transactions in short time).
     * Returns 0 or 40.
     */
    public int calculateVelocityRisk(int transactionsInLastMinute) {
        if (transactionsInLastMinute >= 5) {
            return 40; // Very high velocity
        } else if (transactionsInLastMinute >= 3) {
            return 25; // High velocity
        } else if (transactionsInLastMinute >= 2) {
            return 10; // Medium velocity
        }
        return 0; // Normal velocity
    }
    
    /**
     * Determines decision based on total risk score.
     */
    public FraudCheckResult.Decision determineDecision(int totalRiskScore) {
        if (totalRiskScore >= 70) {
            return FraudCheckResult.Decision.REJECTED;
        } else if (totalRiskScore >= 50) {
            return FraudCheckResult.Decision.MANUAL_REVIEW;
        } else {
            return FraudCheckResult.Decision.APPROVED;
        }
    }
}
