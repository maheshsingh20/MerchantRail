package dev.merchantrail.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("TransactionId Value Object")
class TransactionIdTest {
    
    @Nested
    @DisplayName("Creation from String")
    class CreationFromString {
        
        @Test
        @DisplayName("should create transaction ID with valid 16-character alphanumeric string")
        void shouldCreateTransactionIdWithValidString() {
            TransactionId transactionId = TransactionId.of("TXN1234567890ABC");
            
            assertThat(transactionId.getValue()).isEqualTo("TXN1234567890ABC");
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "ABCD1234EFGH5678",
            "0000000000000000",
            "ZZZZZZZZZZZZZZZZ",
            "1A2B3C4D5E6F7G8H"
        })
        @DisplayName("should accept various valid formats")
        void shouldAcceptVariousValidFormats(String validId) {
            TransactionId transactionId = TransactionId.of(validId);
            assertThat(transactionId.getValue()).isEqualTo(validId);
        }
        
        @Test
        @DisplayName("should throw exception for null value")
        void shouldThrowExceptionForNullValue() {
            assertThatThrownBy(() -> TransactionId.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Transaction ID cannot be null");
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "",                      // empty
            "SHORT",                 // too short
            "WAYTOOLONGVALUE1234",   // too long
            "TXN123456789012",       // only 15 characters
            "txn1234567890abc",      // lowercase
            "TXN-1234-567890",       // contains hyphens
            "TXN 1234567890A",       // contains space
            "TXN123456789@AB"        // contains special character
        })
        @DisplayName("should reject invalid formats")
        void shouldRejectInvalidFormats(String invalidId) {
            assertThatThrownBy(() -> TransactionId.of(invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction ID must be 16 alphanumeric characters");
        }
    }
    
    @Nested
    @DisplayName("Generation")
    class Generation {
        
        @Test
        @DisplayName("should generate valid transaction ID")
        void shouldGenerateValidTransactionId() {
            TransactionId transactionId = TransactionId.generate();
            
            assertThat(transactionId.getValue())
                .hasSize(16)
                .matches("^[A-Z0-9]{16}$")
                .startsWith("TXN");
        }
        
        @Test
        @DisplayName("should generate unique transaction IDs")
        void shouldGenerateUniqueTransactionIds() {
            Set<String> generatedIds = new HashSet<>();
            
            for (int i = 0; i < 1000; i++) {
                TransactionId id = TransactionId.generate();
                boolean wasAdded = generatedIds.add(id.getValue());
                assertThat(wasAdded)
                    .as("Generated ID should be unique: " + id.getValue())
                    .isTrue();
            }
            
            assertThat(generatedIds).hasSize(1000);
        }
        
        @Test
        @DisplayName("generated ID should be valid for of() method")
        void generatedIdShouldBeValidForOfMethod() {
            TransactionId generated = TransactionId.generate();
            
            // Should not throw exception
            TransactionId recreated = TransactionId.of(generated.getValue());
            
            assertThat(recreated).isEqualTo(generated);
        }
    }
    
    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCode {
        
        @Test
        @DisplayName("should be equal when values are the same")
        void shouldBeEqualWhenValuesAreTheSame() {
            TransactionId id1 = TransactionId.of("TXN1234567890ABC");
            TransactionId id2 = TransactionId.of("TXN1234567890ABC");
            
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }
        
        @Test
        @DisplayName("should not be equal when values differ")
        void shouldNotBeEqualWhenValuesDiffer() {
            TransactionId id1 = TransactionId.of("TXN1234567890ABC");
            TransactionId id2 = TransactionId.of("TXN1234567890XYZ");
            
            assertThat(id1).isNotEqualTo(id2);
        }
        
        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            TransactionId id = TransactionId.of("TXN1234567890ABC");
            
            assertThat(id).isEqualTo(id);
        }
        
        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            TransactionId id = TransactionId.of("TXN1234567890ABC");
            
            assertThat(id).isNotEqualTo(null);
        }
        
        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            TransactionId id = TransactionId.of("TXN1234567890ABC");
            
            assertThat(id).isNotEqualTo("TXN1234567890ABC");
        }
        
        @Test
        @DisplayName("should work correctly in hash-based collections")
        void shouldWorkCorrectlyInHashBasedCollections() {
            TransactionId id1 = TransactionId.of("TXN1234567890ABC");
            TransactionId id2 = TransactionId.of("TXN1234567890ABC");
            TransactionId id3 = TransactionId.of("TXN1234567890XYZ");
            
            Set<TransactionId> set = new HashSet<>();
            set.add(id1);
            set.add(id2); // Should not add (same as id1)
            set.add(id3);
            
            assertThat(set).hasSize(2);
            assertThat(set).contains(id1, id3);
        }
    }
    
    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {
        
        @Test
        @DisplayName("toString should return the value")
        void toStringShouldReturnTheValue() {
            TransactionId id = TransactionId.of("TXN1234567890ABC");
            
            assertThat(id.toString()).isEqualTo("TXN1234567890ABC");
        }
    }
    
    @Nested
    @DisplayName("Immutability")
    class Immutability {
        
        @Test
        @DisplayName("value should not be modifiable")
        void valueShouldNotBeModifiable() {
            String original = "TXN1234567890ABC";
            TransactionId id = TransactionId.of(original);
            
            // Even if we modify the original string variable, ID should be unchanged
            original = "MODIFIED00000000";
            
            assertThat(id.getValue()).isEqualTo("TXN1234567890ABC");
        }
    }
}
