package dev.merchantrail.transaction.adapter.out.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Object for transaction events published to Kafka.
 */
public class TransactionEventDto {
    
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
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    // Default constructor for Jackson
    public TransactionEventDto() {}
    
    public static TransactionEventDto fromDomain(Transaction transaction) {
        TransactionEventDto dto = new TransactionEventDto();
        dto.transactionId = transaction.getTransactionId().getValue();
        dto.merchantId = transaction.getMerchantId().getValue();
        dto.amount = transaction.getAmount().getAmount();
        dto.currency = transaction.getAmount().getCurrencyCode();
        dto.status = transaction.getStatus();
        dto.statusReason = transaction.getStatusReason();
        dto.timestamp = transaction.getUpdatedAt();
        return dto;
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
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
