package dev.merchantrail.fraud.domain

import dev.merchantrail.shared.Money
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Spock specification for fraud rule engine.
 * Demonstrates Groovy/Spock for data-driven testing.
 */
class FraudRuleSpec extends Specification {
    
    FraudRule fraudRule = new FraudRule()
    
    @Unroll
    def "should calculate amount risk: #amount #currency = #expectedRisk"() {
        given: "a transaction amount"
        Money money = Money.of(amount, currency)
        
        when: "risk score is calculated"
        int riskScore = fraudRule.calculateAmountRisk(money)
        
        then: "risk matches expected value"
        riskScore == expectedRisk
        
        where: "various transaction amounts"
        amount   | currency || expectedRisk
        100      | "USD"    || 0   // Low amount
        500      | "USD"    || 0   // Low amount
        1000     | "USD"    || 10  // Medium amount
        5000     | "USD"    || 10  // Medium amount
        10000    | "USD"    || 30  // High amount
        25000    | "USD"    || 30  // High amount
        50000    | "USD"    || 50  // Very high amount
        100000   | "USD"    || 50  // Very high amount
    }
    
    @Unroll
    def "should calculate merchant history risk: isNew=#isNewMerchant => risk=#expectedRisk"() {
        when: "merchant history is evaluated"
        int riskScore = fraudRule.calculateMerchantHistoryRisk(isNewMerchant)
        
        then: "risk matches expected value"
        riskScore == expectedRisk
        
        where:
        isNewMerchant || expectedRisk
        true          || 25   // New merchant is risky
        false         || 0    // Established merchant is safe
    }
    
    @Unroll
    def "should calculate velocity risk: #transactions transactions = #expectedRisk risk"() {
        when: "velocity is calculated"
        int riskScore = fraudRule.calculateVelocityRisk(transactions)
        
        then: "risk matches expected value"
        riskScore == expectedRisk
        
        where: "various transaction velocities"
        transactions || expectedRisk
        0            || 0    // No velocity
        1            || 0    // Normal
        2            || 10   // Medium velocity
        3            || 25   // High velocity
        4            || 25   // High velocity
        5            || 40   // Very high velocity
        10           || 40   // Very high velocity
    }
    
    @Unroll
    def "should determine decision based on total risk: score=#totalRisk => #expectedDecision"() {
        when: "decision is determined"
        def decision = fraudRule.determineDecision(totalRisk)
        
        then: "decision matches expected value"
        decision == expectedDecision
        
        where: "various risk scores"
        totalRisk || expectedDecision
        0         || FraudCheckResult.Decision.APPROVED
        10        || FraudCheckResult.Decision.APPROVED
        25        || FraudCheckResult.Decision.APPROVED
        49        || FraudCheckResult.Decision.APPROVED
        50        || FraudCheckResult.Decision.MANUAL_REVIEW
        60        || FraudCheckResult.Decision.MANUAL_REVIEW
        69        || FraudCheckResult.Decision.MANUAL_REVIEW
        70        || FraudCheckResult.Decision.REJECTED
        80        || FraudCheckResult.Decision.REJECTED
        100       || FraudCheckResult.Decision.REJECTED
    }
    
    def "should combine multiple risk factors correctly"() {
        given: "a high-risk scenario"
        Money highAmount = Money.of(60000, "USD")
        boolean newMerchant = true
        int highVelocity = 6
        
        when: "all risk factors are calculated"
        int amountRisk = fraudRule.calculateAmountRisk(highAmount)
        int merchantRisk = fraudRule.calculateMerchantHistoryRisk(newMerchant)
        int velocityRisk = fraudRule.calculateVelocityRisk(highVelocity)
        int totalRisk = amountRisk + merchantRisk + velocityRisk
        def decision = fraudRule.determineDecision(totalRisk)
        
        then: "total risk is sum of all factors"
        amountRisk == 50
        merchantRisk == 25
        velocityRisk == 40
        totalRisk == 115
        
        and: "transaction should be rejected"
        decision == FraudCheckResult.Decision.REJECTED
    }
    
    def "should approve low-risk transaction"() {
        given: "a low-risk scenario"
        Money lowAmount = Money.of(100, "USD")
        boolean establishedMerchant = false
        int normalVelocity = 1
        
        when: "all risk factors are calculated"
        int amountRisk = fraudRule.calculateAmountRisk(lowAmount)
        int merchantRisk = fraudRule.calculateMerchantHistoryRisk(establishedMerchant)
        int velocityRisk = fraudRule.calculateVelocityRisk(normalVelocity)
        int totalRisk = amountRisk + merchantRisk + velocityRisk
        def decision = fraudRule.determineDecision(totalRisk)
        
        then: "all risk factors are low"
        amountRisk == 0
        merchantRisk == 0
        velocityRisk == 0
        totalRisk == 0
        
        and: "transaction should be approved"
        decision == FraudCheckResult.Decision.APPROVED
    }
    
    def "should flag for manual review on borderline risk"() {
        given: "a borderline risk scenario"
        Money mediumAmount = Money.of(12000, "USD")
        boolean newMerchant = true
        int mediumVelocity = 2
        
        when: "all risk factors are calculated"
        int amountRisk = fraudRule.calculateAmountRisk(mediumAmount)
        int merchantRisk = fraudRule.calculateMerchantHistoryRisk(newMerchant)
        int velocityRisk = fraudRule.calculateVelocityRisk(mediumVelocity)
        int totalRisk = amountRisk + merchantRisk + velocityRisk
        def decision = fraudRule.determineDecision(totalRisk)
        
        then: "risk score is in manual review range"
        totalRisk >= 50
        totalRisk < 70
        
        and: "transaction should require manual review"
        decision == FraudCheckResult.Decision.MANUAL_REVIEW
    }
}
