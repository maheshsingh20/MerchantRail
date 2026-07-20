package dev.merchantrail.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MerchantId Value Object")
class MerchantIdTest {
    
    @Nested
    @DisplayName("Creation from String")
    class CreationFromString {
        
        @Test
        @DisplayName("should create merchant ID with valid 8-character alphanumeric string")
        void shouldCreateMerchantIdWithValidString() {
            MerchantId merchantId = MerchantId.of("MERCH001");
            
            assertThat(merchantId.getValue()).isEqualTo("MERCH001");
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "MERCH001",
            "M1234567",
            "ABC12345",
            "00000000",
            "ZZZZZZZZ"
        })
        @DisplayName("should accept various valid formats")
        void shouldAcceptVariousValidFormats(String validId) {
            MerchantId merchantId = MerchantId.of(validId);
            assertThat(merchantId.getValue()).isEqualTo(validId);
        }
        
        @Test
        @DisplayName("should throw exception for null value")
        void shouldThrowExceptionForNullValue() {
            assertThatThrownBy(() -> MerchantId.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Merchant ID cannot be null");
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "",                 // empty
            "SHORT",            // too short (5 chars)
            "TOOLONGID",        // too long (9 chars)
            "merch001",         // lowercase
            "MERCH-01",         // contains hyphen
            "MERCH 01",         // contains space
            "MERCH@01"          // contains special character
        })
        @DisplayName("should reject invalid formats")
        void shouldRejectInvalidFormats(String invalidId) {
            assertThatThrownBy(() -> MerchantId.of(invalidId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Merchant ID must be 8 alphanumeric characters");
        }
    }
    
    @Nested
    @DisplayName("Generation")
    class Generation {
        
        @Test
        @DisplayName("should generate valid merchant ID")
        void shouldGenerateValidMerchantId() {
            MerchantId merchantId = MerchantId.generate();
            
            assertThat(merchantId.getValue())
                .hasSize(8)
                .matches("^[A-Z0-9]{8}$")
                .startsWith("M");
        }
        
        @Test
        @DisplayName("should generate unique merchant IDs")
        void shouldGenerateUniqueMerchantIds() {
            Set<String> generatedIds = new HashSet<>();
            
            for (int i = 0; i < 1000; i++) {
                MerchantId id = MerchantId.generate();
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
            MerchantId generated = MerchantId.generate();
            
            // Should not throw exception
            MerchantId recreated = MerchantId.of(generated.getValue());
            
            assertThat(recreated).isEqualTo(generated);
        }
    }
    
    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCode {
        
        @Test
        @DisplayName("should be equal when values are the same")
        void shouldBeEqualWhenValuesAreTheSame() {
            MerchantId id1 = MerchantId.of("MERCH001");
            MerchantId id2 = MerchantId.of("MERCH001");
            
            assertThat(id1).isEqualTo(id2);
            assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
        }
        
        @Test
        @DisplayName("should not be equal when values differ")
        void shouldNotBeEqualWhenValuesDiffer() {
            MerchantId id1 = MerchantId.of("MERCH001");
            MerchantId id2 = MerchantId.of("MERCH002");
            
            assertThat(id1).isNotEqualTo(id2);
        }
        
        @Test
        @DisplayName("should be equal to itself")
        void shouldBeEqualToItself() {
            MerchantId id = MerchantId.of("MERCH001");
            
            assertThat(id).isEqualTo(id);
        }
        
        @Test
        @DisplayName("should not be equal to null")
        void shouldNotBeEqualToNull() {
            MerchantId id = MerchantId.of("MERCH001");
            
            assertThat(id).isNotEqualTo(null);
        }
        
        @Test
        @DisplayName("should not be equal to different type")
        void shouldNotBeEqualToDifferentType() {
            MerchantId id = MerchantId.of("MERCH001");
            
            assertThat(id).isNotEqualTo("MERCH001");
        }
        
        @Test
        @DisplayName("should work correctly in hash-based collections")
        void shouldWorkCorrectlyInHashBasedCollections() {
            MerchantId id1 = MerchantId.of("MERCH001");
            MerchantId id2 = MerchantId.of("MERCH001");
            MerchantId id3 = MerchantId.of("MERCH002");
            
            Set<MerchantId> set = new HashSet<>();
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
            MerchantId id = MerchantId.of("MERCH001");
            
            assertThat(id.toString()).isEqualTo("MERCH001");
        }
    }
    
    @Nested
    @DisplayName("Immutability")
    class Immutability {
        
        @Test
        @DisplayName("value should not be modifiable")
        void valueShouldNotBeModifiable() {
            String original = "MERCH001";
            MerchantId id = MerchantId.of(original);
            
            // Even if we modify the original string variable, ID should be unchanged
            original = "MODIFIED";
            
            assertThat(id.getValue()).isEqualTo("MERCH001");
        }
    }
}
