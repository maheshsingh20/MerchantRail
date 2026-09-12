package dev.merchantrail.transaction.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.merchantrail.transaction.domain.Transaction;
import dev.merchantrail.transaction.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for transaction endpoints with switching and network routing metadata.
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

    // Switching Attributes
    @JsonProperty("cardPan")
    private String cardPan;

    @JsonProperty("cardBin")
    private String cardBin;

    @JsonProperty("cardBrand")
    private String cardBrand;

    @JsonProperty("routedIssuerId")
    private String routedIssuerId;

    @JsonProperty("isStip")
    private boolean isStip;

    @JsonProperty("authCode")
    private String authCode;

    @JsonProperty("interchangeFee")
    private BigDecimal interchangeFee;

    @JsonProperty("switchFee")
    private BigDecimal switchFee;
    
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

        response.cardPan = transaction.getMaskedPan();
        response.cardBin = transaction.getCardBin();
        response.cardBrand = transaction.getCardBrand() != null ? transaction.getCardBrand().name() : null;
        response.routedIssuerId = transaction.getRoutedIssuerId();
        response.isStip = transaction.isStip();
        response.authCode = transaction.getAuthCode();
        response.interchangeFee = transaction.getInterchangeFee() != null ? transaction.getInterchangeFee().getAmount() : null;
        response.switchFee = transaction.getSwitchFee() != null ? transaction.getSwitchFee().getAmount() : null;

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

    public String getCardBrand() {
        return cardBrand;
    }

    public void setCardBrand(String cardBrand) {
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
