package dev.merchantrail.fraud.adapter.out.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.merchantrail.fraud.domain.FraudCheckResult;

public class FraudEventDto {
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("riskScore")
    private int riskScore;
    
    @JsonProperty("decision")
    private String decision;
    
    @JsonProperty("reason")
    private String reason;
    
    public FraudEventDto() {}
    
    public static FraudEventDto fromDomain(FraudCheckResult result) {
        FraudEventDto dto = new FraudEventDto();
        dto.transactionId = result.getTransactionId().getValue();
        dto.riskScore = result.getRiskScore();
        dto.decision = result.getDecision().name();
        dto.reason = result.getReason();
        return dto;
    }
    
    // Getters and setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
