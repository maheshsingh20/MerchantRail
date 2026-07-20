package dev.merchantrail.shared;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Value object representing a unique merchant identifier.
 * Format: 8 alphanumeric characters (e.g., "MERCH001", "M12AB789")
 * Immutable and validates format on construction.
 */
public final class MerchantId {
    
    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Z0-9]{8}$");
    private static final String PREFIX = "M";
    
    private final String value;
    
    private MerchantId(String value) {
        this.value = value;
    }
    
    /**
     * Creates a MerchantId from a string value.
     *
     * @param value the merchant ID string (must be 8 alphanumeric characters)
     * @return MerchantId instance
     * @throws IllegalArgumentException if format is invalid
     * @throws NullPointerException if value is null
     */
    public static MerchantId of(String value) {
        Objects.requireNonNull(value, "Merchant ID cannot be null");
        
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Merchant ID must be 8 alphanumeric characters, got: " + value
            );
        }
        
        return new MerchantId(value);
    }
    
    /**
     * Generates a new unique MerchantId with M prefix.
     *
     * @return new MerchantId instance
     */
    public static MerchantId generate() {
        // Generate a UUID and convert to alphanumeric, take first 7 chars, add prefix
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String suffix = uuid.substring(0, 7).toUpperCase();
        String value = PREFIX + suffix;
        return new MerchantId(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MerchantId that = (MerchantId) o;
        return value.equals(that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
