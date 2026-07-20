package dev.merchantrail.fraud.application.usecase;

import dev.merchantrail.fraud.application.port.in.CheckFraudCommand;
import dev.merchantrail.fraud.application.port.out.EventPublisher;
import dev.merchantrail.fraud.application.port.out.FraudCheckRepository;
import dev.merchantrail.fraud.application.port.out.MerchantHistoryService;
import dev.merchantrail.fraud.application.port.out.TransactionHistoryService;
import dev.merchantrail.fraud.domain.FraudCheckResult;
import dev.merchantrail.fraud.domain.FraudRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;

/**
 * Use case for evaluating transaction fraud risk.
 */
public class CheckFraudUseCase {
    
    private static final Logger log = LoggerFactory.getLogger(CheckFraudUseCase.class);
    
    private final FraudRule fraudRule;
    private final MerchantHistoryService merchantHistoryService;
    private final TransactionHistoryService transactionHistoryService;
    private final FraudCheckRepository fraudCheckRepository;
    private final EventPublisher eventPublisher;
    
    public CheckFraudUseCase(FraudRule fraudRule,
                            MerchantHistoryService merchantHistoryService,
                            TransactionHistoryService transactionHistoryService,
                            FraudCheckRepository fraudCheckRepository,
                            EventPublisher eventPublisher) {
        this.fraudRule = fraudRule;
        this.merchantHistoryService = merchantHistoryService;
        this.transactionHistoryService = transactionHistoryService;
        this.fraudCheckRepository = fraudCheckRepository;
        this.eventPublisher = eventPublisher;
    }
    
    public FraudCheckResult execute(CheckFraudCommand command) {
        log.info("Checking fraud for transaction {}", command.transactionId());
        
        // Calculate individual risk scores
        int amountRisk = fraudRule.calculateAmountRisk(command.amount());
        
        boolean isNewMerchant = merchantHistoryService.isNewMerchant(command.merchantId());
        int merchantRisk = fraudRule.calculateMerchantHistoryRisk(isNewMerchant);
        
        Instant oneMinuteAgo = Instant.now().minus(Duration.ofMinutes(1));
        int recentTransactions = transactionHistoryService.countRecentTransactions(
            command.merchantId(), 
            oneMinuteAgo
        );
        int velocityRisk = fraudRule.calculateVelocityRisk(recentTransactions);
        
        // Calculate total risk
        int totalRisk = amountRisk + merchantRisk + velocityRisk;
        FraudCheckResult.Decision decision = fraudRule.determineDecision(totalRisk);
        
        // Create result
        FraudCheckResult result;
        String reason = buildReason(amountRisk, merchantRisk, velocityRisk);
        
        if (decision == FraudCheckResult.Decision.APPROVED) {
            result = FraudCheckResult.approved(command.transactionId(), totalRisk);
        } else if (decision == FraudCheckResult.Decision.REJECTED) {
            result = FraudCheckResult.rejected(command.transactionId(), totalRisk, reason);
        } else {
            result = FraudCheckResult.manualReview(command.transactionId(), totalRisk, reason);
        }
        
        // Persist fraud check
        fraudCheckRepository.save(result);
        
        // Publish event
        if (result.isApproved()) {
            eventPublisher.publishFraudCheckPassed(result);
        } else if (result.isRejected()) {
            eventPublisher.publishFraudCheckFailed(result);
        } else {
            eventPublisher.publishFraudCheckManualReview(result);
        }
        
        log.info("Fraud check complete: transaction={}, decision={}, score={}",
            command.transactionId(), decision, totalRisk);
        
        return result;
    }
    
    private String buildReason(int amountRisk, int merchantRisk, int velocityRisk) {
        StringBuilder sb = new StringBuilder();
        if (amountRisk > 0) {
            sb.append("High amount (score: ").append(amountRisk).append("). ");
        }
        if (merchantRisk > 0) {
            sb.append("New merchant (score: ").append(merchantRisk).append("). ");
        }
        if (velocityRisk > 0) {
            sb.append("High velocity (score: ").append(velocityRisk).append("). ");
        }
        return sb.toString().trim();
    }
}
