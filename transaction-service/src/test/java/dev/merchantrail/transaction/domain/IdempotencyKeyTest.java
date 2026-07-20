package dev.merchantrail.transaction.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("IdempotencyKey Value Object")
class IdempotencyKeyTest {
    
    @Test
    @DisplayName("should create idempotency key with valid value")
    void shouldCreateIdempotencyKeyWithValidValue() {
        IdempotencyKey key = IdempotencyKey.of("valid-key-123");
        assertThat(key.getValue()).isEqualTo("valid-key-123");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "simple",
        "with-dashes",
        "with_underscores",
        "MixedCase123",
        "a",
        "very-long-key-that-is-still-valid-because-its-under-255-characters"
    })
    @DisplayName("should accept various valid formats")
    void shouldAcceptVariousValidFormats(String validKey) {
        IdempotencyKey key = IdempotencyKey.of(validKey);
        assertThat(key.getValue()).isEqualTo(validKey);
    }
    
    @Test
    @DisplayName("should generate unique idempotency keys")
    void shouldGenerateUniqueIdempotencyKeys() {
        IdempotencyKey key1 = IdempotencyKey.generate();
        IdempotencyKey key2 = IdempotencyKey.generate();
        
        assertThat(key1).isNotEqualTo(key2);
    }
    
    @Test
    @DisplayName("should throw exception for null value")
    void shouldThrowExceptionForNullValue() {
        assertThatThrownBy(() -> IdempotencyKey.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Idempotency key cannot be null");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "",
        "has spaces",
        "has@special",
        "has#chars",
        "has!punctuation"
    })
    @DisplayName("should reject invalid formats")
    void shouldRejectInvalidFormats(String invalidKey) {
        assertThatThrownBy(() -> IdempotencyKey.of(invalidKey))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be 1-255 alphanumeric");
    }
    
    @Test
    @DisplayName("should be equal when values are the same")
    void shouldBeEqualWhenValuesAreTheSame() {
        IdempotencyKey key1 = IdempotencyKey.of("same-key");
        IdempotencyKey key2 = IdempotencyKey.of("same-key");
        
        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }
    
    @Test
    @DisplayName("toString should return the value")
    void toStringShouldReturnTheValue() {
        IdempotencyKey key = IdempotencyKey.of("test-key");
        assertThat(key.toString()).isEqualTo("test-key");
    }
}
