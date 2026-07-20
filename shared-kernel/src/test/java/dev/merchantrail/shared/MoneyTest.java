package dev.merchantrail.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Money Value Object")
class MoneyTest {
    
    @Nested
    @DisplayName("Creation")
    class Creation {
        
        @Test
        @DisplayName("should create money with valid amount and currency")
        void shouldCreateMoneyWithValidAmountAndCurrency() {
            Money money = Money.of(new BigDecimal("100.50"), "USD");
            
            assertThat(money.getAmount()).isEqualByComparingTo("100.50");
            assertThat(money.getCurrencyCode()).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("should round amount to currency's decimal places")
        void shouldRoundAmountToCurrencyDecimalPlaces() {
            // USD has 2 decimal places
            Money usd = Money.of(new BigDecimal("100.556"), "USD");
            assertThat(usd.getAmount()).isEqualByComparingTo("100.56");
            
            // JPY has 0 decimal places
            Money jpy = Money.of(new BigDecimal("1000.7"), "JPY");
            assertThat(jpy.getAmount()).isEqualByComparingTo("1001");
        }
        
        @Test
        @DisplayName("should create money from minor units")
        void shouldCreateMoneyFromMinorUnits() {
            // 1050 cents = $10.50
            Money money = Money.ofMinor(1050, "USD");
            
            assertThat(money.getAmount()).isEqualByComparingTo("10.50");
            assertThat(money.getCurrencyCode()).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("should throw exception for null amount")
        void shouldThrowExceptionForNullAmount() {
            assertThatThrownBy(() -> Money.of(null, "USD"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Amount cannot be null");
        }
        
        @Test
        @DisplayName("should throw exception for null currency code")
        void shouldThrowExceptionForNullCurrencyCode() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("100"), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Currency code cannot be null");
        }
        
        @Test
        @DisplayName("should throw exception for invalid currency code")
        void shouldThrowExceptionForInvalidCurrencyCode() {
            assertThatThrownBy(() -> Money.of(new BigDecimal("100"), "INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid currency code");
        }
    }
    
    @Nested
    @DisplayName("Arithmetic Operations")
    class ArithmeticOperations {
        
        @Test
        @DisplayName("should add two money amounts with same currency")
        void shouldAddTwoMoneyAmountsWithSameCurrency() {
            Money m1 = Money.of(new BigDecimal("100.50"), "USD");
            Money m2 = Money.of(new BigDecimal("50.25"), "USD");
            
            Money result = m1.add(m2);
            
            assertThat(result.getAmount()).isEqualByComparingTo("150.75");
            assertThat(result.getCurrencyCode()).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("should subtract two money amounts with same currency")
        void shouldSubtractTwoMoneyAmountsWithSameCurrency() {
            Money m1 = Money.of(new BigDecimal("100.50"), "USD");
            Money m2 = Money.of(new BigDecimal("50.25"), "USD");
            
            Money result = m1.subtract(m2);
            
            assertThat(result.getAmount()).isEqualByComparingTo("50.25");
            assertThat(result.getCurrencyCode()).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("should multiply money by scalar")
        void shouldMultiplyMoneyByScalar() {
            Money money = Money.of(new BigDecimal("10.50"), "USD");
            
            Money result = money.multiply(new BigDecimal("3"));
            
            assertThat(result.getAmount()).isEqualByComparingTo("31.50");
            assertThat(result.getCurrencyCode()).isEqualTo("USD");
        }
        
        @Test
        @DisplayName("should throw exception when adding different currencies")
        void shouldThrowExceptionWhenAddingDifferentCurrencies() {
            Money usd = Money.of(new BigDecimal("100"), "USD");
            Money eur = Money.of(new BigDecimal("100"), "EUR");
            
            assertThatThrownBy(() -> usd.add(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot operate on different currencies");
        }
        
        @Test
        @DisplayName("should throw exception when subtracting different currencies")
        void shouldThrowExceptionWhenSubtractingDifferentCurrencies() {
            Money usd = Money.of(new BigDecimal("100"), "USD");
            Money eur = Money.of(new BigDecimal("100"), "EUR");
            
            assertThatThrownBy(() -> usd.subtract(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot operate on different currencies");
        }
    }
    
    @Nested
    @DisplayName("Comparison")
    class Comparison {
        
        @Test
        @DisplayName("should identify positive amount")
        void shouldIdentifyPositiveAmount() {
            Money money = Money.of(new BigDecimal("100"), "USD");
            assertThat(money.isPositive()).isTrue();
            assertThat(money.isNegative()).isFalse();
            assertThat(money.isZero()).isFalse();
        }
        
        @Test
        @DisplayName("should identify negative amount")
        void shouldIdentifyNegativeAmount() {
            Money money = Money.of(new BigDecimal("-100"), "USD");
            assertThat(money.isPositive()).isFalse();
            assertThat(money.isNegative()).isTrue();
            assertThat(money.isZero()).isFalse();
        }
        
        @Test
        @DisplayName("should identify zero amount")
        void shouldIdentifyZeroAmount() {
            Money money = Money.of(BigDecimal.ZERO, "USD");
            assertThat(money.isPositive()).isFalse();
            assertThat(money.isNegative()).isFalse();
            assertThat(money.isZero()).isTrue();
        }
        
        @ParameterizedTest
        @CsvSource({
            "100, 50, 1",    // greater
            "50, 50, 0",     // equal
            "25, 50, -1"     // less
        })
        @DisplayName("should compare money amounts correctly")
        void shouldCompareMoneyAmountsCorrectly(String amount1, String amount2, int expectedSign) {
            Money m1 = Money.of(new BigDecimal(amount1), "USD");
            Money m2 = Money.of(new BigDecimal(amount2), "USD");
            
            int result = m1.compareTo(m2);
            
            if (expectedSign > 0) {
                assertThat(result).isPositive();
            } else if (expectedSign < 0) {
                assertThat(result).isNegative();
            } else {
                assertThat(result).isZero();
            }
        }
        
        @Test
        @DisplayName("should throw exception when comparing different currencies")
        void shouldThrowExceptionWhenComparingDifferentCurrencies() {
            Money usd = Money.of(new BigDecimal("100"), "USD");
            Money eur = Money.of(new BigDecimal("100"), "EUR");
            
            assertThatThrownBy(() -> usd.compareTo(eur))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot operate on different currencies");
        }
    }
    
    @Nested
    @DisplayName("Minor Units Conversion")
    class MinorUnitsConversion {
        
        @Test
        @DisplayName("should convert USD to minor units (cents)")
        void shouldConvertUsdToMinorUnits() {
            Money money = Money.of(new BigDecimal("10.50"), "USD");
            assertThat(money.getMinorUnits()).isEqualTo(1050L);
        }
        
        @Test
        @DisplayName("should convert JPY to minor units (no decimal places)")
        void shouldConvertJpyToMinorUnits() {
            Money money = Money.of(new BigDecimal("1000"), "JPY");
            assertThat(money.getMinorUnits()).isEqualTo(1000L);
        }
        
        @Test
        @DisplayName("should round-trip convert minor units")
        void shouldRoundTripConvertMinorUnits() {
            Money original = Money.ofMinor(12345, "USD");
            long minorUnits = original.getMinorUnits();
            Money reconstructed = Money.ofMinor(minorUnits, "USD");
            
            assertThat(reconstructed).isEqualTo(original);
        }
    }
    
    @Nested
    @DisplayName("Immutability")
    class Immutability {
        
        @Test
        @DisplayName("should return new instance on arithmetic operations")
        void shouldReturnNewInstanceOnArithmeticOperations() {
            Money original = Money.of(new BigDecimal("100"), "USD");
            Money added = original.add(Money.of(new BigDecimal("50"), "USD"));
            
            assertThat(added).isNotSameAs(original);
            assertThat(original.getAmount()).isEqualByComparingTo("100");
            assertThat(added.getAmount()).isEqualByComparingTo("150");
        }
    }
    
    @Nested
    @DisplayName("Equality and HashCode")
    class EqualityAndHashCode {
        
        @Test
        @DisplayName("should be equal when amount and currency are the same")
        void shouldBeEqualWhenAmountAndCurrencyAreTheSame() {
            Money m1 = Money.of(new BigDecimal("100.50"), "USD");
            Money m2 = Money.of(new BigDecimal("100.50"), "USD");
            
            assertThat(m1).isEqualTo(m2);
            assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
        }
        
        @Test
        @DisplayName("should not be equal when amounts differ")
        void shouldNotBeEqualWhenAmountsDiffer() {
            Money m1 = Money.of(new BigDecimal("100.50"), "USD");
            Money m2 = Money.of(new BigDecimal("100.51"), "USD");
            
            assertThat(m1).isNotEqualTo(m2);
        }
        
        @Test
        @DisplayName("should not be equal when currencies differ")
        void shouldNotBeEqualWhenCurrenciesDiffer() {
            Money m1 = Money.of(new BigDecimal("100"), "USD");
            Money m2 = Money.of(new BigDecimal("100"), "EUR");
            
            assertThat(m1).isNotEqualTo(m2);
        }
        
        @Test
        @DisplayName("should handle trailing zeros in equality")
        void shouldHandleTrailingZerosInEquality() {
            Money m1 = Money.of(new BigDecimal("100.50"), "USD");
            Money m2 = Money.of(new BigDecimal("100.500"), "USD");
            
            assertThat(m1).isEqualTo(m2);
        }
    }
    
    @Nested
    @DisplayName("String Representation")
    class StringRepresentation {
        
        @Test
        @DisplayName("should format toString correctly")
        void shouldFormatToStringCorrectly() {
            Money money = Money.of(new BigDecimal("100.50"), "USD");
            assertThat(money.toString()).isEqualTo("USD 100.50");
        }
    }
}
