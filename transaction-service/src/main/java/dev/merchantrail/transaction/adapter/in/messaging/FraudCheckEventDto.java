package dev.merchantrail.transaction.adapter.in.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FraudCheckEventDto {
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("riskScore")
    private int riskScore;
    
    @JsonProperty("decision")
    private String decision;
    
    @JsonProperty("reason")
    private String reason;
    
    public FraudCheckEventDto() {}
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
    
    public int getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }
    
    public String getDecision() {
        return decision;
    }
    
    public void setDecision(String decision) {
        this.decision = decision;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
