package dev.merchantrail.transaction.domain;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Value object representing an idempotency key for preventing duplicate transaction submissions.
 */
public final class IdempotencyKey {
    
    private static final Pattern VALID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,255}$");
    
    private final String value;
    
    private IdempotencyKey(String value) {
        this.value = value;
    }
    
    /**
     * Creates an IdempotencyKey from a client-provided value.
     */
    public static IdempotencyKey of(String value) {
        Objects.requireNonNull(value, "Idempotency key cannot be null");
        
        if (!VALID_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "Idempotency key must be 1-255 alphanumeric, dash, or underscore characters"
            );
        }
        
        return new IdempotencyKey(value);
    }
    
    /**
     * Generates a new unique idempotency key.
     */
    public static IdempotencyKey generate() {
        return new IdempotencyKey(UUID.randomUUID().toString());
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdempotencyKey that = (IdempotencyKey) o;
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
