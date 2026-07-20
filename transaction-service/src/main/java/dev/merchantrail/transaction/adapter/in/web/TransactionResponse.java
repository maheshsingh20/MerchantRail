package dev.merchantrail.transaction.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for transaction endpoints.
 */
public class TransactionResponse {
    
    @JsonProperty("transactionId")
    private String transactionId;
    
    @JsonProperty("merchantId")
    private String merchantId;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("status")
    private TransactionStatus status;
    
    @JsonProperty("statusReason")
    private String statusReason;
    
    @JsonProperty("createdAt")
    private Instant createdAt;
    
    @JsonProperty("updatedAt")
    private Instant updatedAt;
    
    public TransactionResponse() {}
    
    public static TransactionResponse fromDomain(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.transactionId = transaction.getTransactionId().getValue();
        response.merchantId = transaction.getMerchantId().getValue();
        response.amount = transaction.getAmount().getAmount();
        response.currency = transaction.getAmount().getCurrencyCode();
        response.status = transaction.getStatus();
        response.statusReason = transaction.getStatusReason();
        response.createdAt = transaction.getCreatedAt();
        response.updatedAt = transaction.getUpdatedAt();
        return response;
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
}
