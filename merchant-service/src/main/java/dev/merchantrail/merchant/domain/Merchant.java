package dev.merchantrail.merchant.domain;

import dev.merchantrail.shared.MerchantId;

import java.time.Instant;
import java.util.Objects;

/**
 * Merchant entity representing a business using the payment gateway.
 */
public class Merchant {
    
    private final MerchantId merchantId;
    private String businessName;
    private String apiKey;
    private MerchantStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    
    private Merchant(MerchantId merchantId, String businessName, String apiKey,
                    MerchantStatus status, Instant createdAt, Instant updatedAt) {
        this.merchantId = merchantId;
        this.businessName = businessName;
        this.apiKey = apiKey;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public static Merchant create(String businessName) {
        if (businessName == null || businessName.trim().isEmpty()) {
            throw new IllegalArgumentException("Business name is required");
        }
        
        Instant now = Instant.now();
        return new Merchant(
            MerchantId.generate(),
            businessName,
            ApiKey.generate(),
            MerchantStatus.PENDING_APPROVAL,
            now,
            now
        );
    }
    
    public static Merchant reconstitute(MerchantId merchantId, String businessName,
                                       String apiKey, MerchantStatus status,
                                       Instant createdAt, Instant updatedAt) {
        return new Merchant(merchantId, businessName, apiKey, status, createdAt, updatedAt);
    }
    
    public void approve() {
        if (status != MerchantStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Can only approve PENDING merchants");
        }
        this.status = MerchantStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }
    
    public void suspend() {
        if (status != MerchantStatus.ACTIVE) {
            throw new IllegalStateException("Can only suspend ACTIVE merchants");
        }
        this.status = MerchantStatus.SUSPENDED;
        this.updatedAt = Instant.now();
    }
    
    public void regenerateApiKey() {
        this.apiKey = ApiKey.generate();
        this.updatedAt = Instant.now();
    }
    
    public boolean isActive() {
        return status == MerchantStatus.ACTIVE;
    }
    
    // Getters
    public MerchantId getMerchantId() { return merchantId; }
    public String getBusinessName() { return businessName; }
    public String getApiKey() { return apiKey; }
    public MerchantStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Merchant merchant = (Merchant) o;
        return merchantId.equals(merchant.merchantId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(merchantId);
    }
    
    public enum MerchantStatus {
        PENDING_APPROVAL,
        ACTIVE,
        SUSPENDED
    }
}
