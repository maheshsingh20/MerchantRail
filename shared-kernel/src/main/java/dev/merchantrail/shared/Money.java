package dev.merchantrail.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing a monetary amount with currency.
 * Immutable and validates currency codes according to ISO 4217.
 */
public final class Money {
    
    private final BigDecimal amount;
    private final Currency currency;
    
    private Money(BigDecimal amount, Currency currency) {
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        this.currency = currency;
    }
    
    /**
     * Creates a Money instance with the specified amount and currency code.
     *
     * @param amount the monetary amount (will be rounded to currency's decimal places)
     * @param currencyCode ISO 4217 currency code (e.g., "USD", "EUR")
     * @return Money instance
     * @throws IllegalArgumentException if currency code is invalid
     * @throws NullPointerException if amount or currencyCode is null
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currencyCode, "Currency code cannot be null");
        
        try {
            Currency currency = Currency.getInstance(currencyCode);
            return new Money(amount, currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currencyCode, e);
        }
    }
    
    /**
     * Creates a Money instance from a long amount (in minor units) and currency code.
     * For example, 1050 cents becomes $10.50 USD.
     *
     * @param minorUnits amount in the currency's minor units (e.g., cents for USD)
     * @param currencyCode ISO 4217 currency code
     * @return Money instance
     */
    public static Money ofMinor(long minorUnits, String currencyCode) {
        Currency currency = Currency.getInstance(currencyCode);
        int fractionDigits = currency.getDefaultFractionDigits();
        BigDecimal divisor = BigDecimal.TEN.pow(fractionDigits);
        BigDecimal amount = BigDecimal.valueOf(minorUnits).divide(divisor, fractionDigits, RoundingMode.HALF_EVEN);
        return new Money(amount, currency);
    }
    
    /**
     * Adds another Money amount to this one.
     *
     * @param other the money to add
     * @return new Money instance with sum
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * Subtracts another Money amount from this one.
     *
     * @param other the money to subtract
     * @return new Money instance with difference
     * @throws IllegalArgumentException if currencies don't match
     */
    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }
    
    /**
     * Multiplies this Money by a scalar value.
     *
     * @param multiplier the value to multiply by
     * @return new Money instance with product
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currency);
    }
    
    /**
     * Checks if this amount is positive (greater than zero).
     *
     * @return true if amount is positive
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Checks if this amount is negative (less than zero).
     *
     * @return true if amount is negative
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }
    
    /**
     * Checks if this amount is zero.
     *
     * @return true if amount is zero
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Compares this Money to another for ordering.
     *
     * @param other the money to compare to
     * @return negative if less, zero if equal, positive if greater
     * @throws IllegalArgumentException if currencies don't match
     */
    public int compareTo(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }
    
    /**
     * Returns the amount in the currency's minor units.
     * For example, $10.50 USD returns 1050.
     *
     * @return amount in minor units
     */
    public long getMinorUnits() {
        int fractionDigits = currency.getDefaultFractionDigits();
        BigDecimal multiplier = BigDecimal.TEN.pow(fractionDigits);
        return amount.multiply(multiplier).longValue();
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public Currency getCurrency() {
        return currency;
    }
    
    public String getCurrencyCode() {
        return currency.getCurrencyCode();
    }
    
    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot operate on different currencies: %s and %s",
                    this.currency.getCurrencyCode(),
                    other.currency.getCurrencyCode())
            );
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && currency.equals(money.currency);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }
    
    @Override
    public String toString() {
        return String.format("%s %s", currency.getCurrencyCode(), amount.toPlainString());
    }
}
