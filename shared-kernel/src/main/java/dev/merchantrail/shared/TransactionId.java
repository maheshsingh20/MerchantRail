package dev.merchantrail.shared;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Value object representing a unique transaction identifier.
 * Format: 16 alphanumeric characters (e.g., "TXN1234567890ABC")
 * Immutable and validates format on construction.
 */
public final class TransactionId {
    
    private static final Pattern VALID_PATTERN = Pattern.compile("^[A-Z0-9]{16}$");
    private static final String PREFIX = "TXN";
    
    private final String value;
    
    private TransactionId(String value) {
        this.value = value;
    }
    
    /**
     * Creates a TransactionId from a string value.
     *
     * @param value the transaction ID string (must be 16 alphanumeric characters)
     * @return TransactionId instance
     * @throws IllegalArgumentException if format is invalid
     * @throws NullPointerException if value is null
     */
    public static TransactionId of(String value) {
        Objects.requireNonNull(value, "Transaction ID cannot be null");
        
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Transaction ID must be 16 alphanumeric characters, got: " + value
            );
        }
        
        return new TransactionId(value);
    }
    
    /**
     * Generates a new unique TransactionId with TXN prefix.
     *
     * @return new TransactionId instance
     */
    public static TransactionId generate() {
        // Generate a UUID and convert to base36 (alphanumeric), take first 13 chars, add prefix
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String suffix = uuid.substring(0, 13).toUpperCase();
        String value = PREFIX + suffix;
        return new TransactionId(value);
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TransactionId that = (TransactionId) o;
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
