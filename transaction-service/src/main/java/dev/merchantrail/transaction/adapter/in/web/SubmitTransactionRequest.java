package dev.merchantrail.transaction.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.merchantrail.shared.CardPan;
import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.transaction.application.port.in.SubmitTransactionCommand;
import dev.merchantrail.transaction.domain.IdempotencyKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for submitting a transaction into the switching platform.
 */
public class SubmitTransactionRequest {
    
    @NotBlank(message = "Merchant ID is required")
    @JsonProperty("merchantId")
    private String merchantId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @JsonProperty("amount")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @JsonProperty("currency")
    private String currency;
    
    @NotBlank(message = "Idempotency key is required")
    @JsonProperty("idempotencyKey")
    private String idempotencyKey;

    @JsonProperty("cardNumber")
    private String cardNumber;
    
    public SubmitTransactionRequest() {}
    
    public SubmitTransactionCommand toCommand() {
        CardPan pan = (cardNumber != null && !cardNumber.isBlank()) ? CardPan.ofUnchecked(cardNumber) : null;
        return new SubmitTransactionCommand(
            MerchantId.of(merchantId),
            Money.of(amount, currency),
            IdempotencyKey.of(idempotencyKey),
            pan
        );
    }
    
    // Getters and setters
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
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
}
