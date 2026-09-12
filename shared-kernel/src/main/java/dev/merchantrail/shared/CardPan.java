package dev.merchantrail.shared;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value object representing a Primary Account Number (PAN) in card payments.
 * Enforces Luhn checksum validation, PCI-DSS compliant masking, and BIN extraction.
 */
public final class CardPan implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern DIGITS_ONLY = Pattern.compile("^[0-9]{13,19}$");

    private final String pan;
    private final String bin;
    private final CardBrand brand;

    private CardPan(String sanitizedPan) {
        this.pan = sanitizedPan;
        this.bin = sanitizedPan.length() >= 6 ? sanitizedPan.substring(0, 6) : sanitizedPan;
        this.brand = resolveBrand(sanitizedPan);
    }

    /**
     * Creates and validates a CardPan from raw card number string.
     *
     * @param rawPan raw card number, may contain spaces or hyphens
     * @return validated CardPan instance
     * @throws IllegalArgumentException if PAN is invalid or fails Luhn check
     */
    public static CardPan of(String rawPan) {
        Objects.requireNonNull(rawPan, "Card PAN cannot be null");
        String sanitized = rawPan.replaceAll("[\\s-]", "");

        if (!DIGITS_ONLY.matcher(sanitized).matches()) {
            throw new IllegalArgumentException("Card PAN must contain 13 to 19 digits");
        }

        if (!validateLuhn(sanitized)) {
            throw new IllegalArgumentException("Card PAN failed Luhn checksum validation");
        }

        return new CardPan(sanitized);
    }

    /**
     * Creates a CardPan without strict Luhn validation (useful for test/mocked card numbers).
     */
    public static CardPan ofUnchecked(String rawPan) {
        Objects.requireNonNull(rawPan, "Card PAN cannot be null");
        String sanitized = rawPan.replaceAll("[\\s-]", "");
        if (sanitized.length() < 6) {
            throw new IllegalArgumentException("Card PAN must be at least 6 digits");
        }
        return new CardPan(sanitized);
    }

    /**
     * Returns the 6-digit Bank Identification Number (BIN) used for network switching & routing.
     */
    public String getBin() {
        return bin;
    }

    /**
     * Returns the 8-digit extended BIN (ISO/IEC 7812-1:2017 standard).
     */
    public String getExtendedBin() {
        return pan.length() >= 8 ? pan.substring(0, 8) : bin;
    }

    /**
     * Returns the detected card scheme brand (Mastercard, Visa, Amex, etc.).
     */
    public CardBrand getBrand() {
        return brand;
    }

    /**
     * Returns last 4 digits of the card number.
     */
    public String getLastFour() {
        return pan.substring(pan.length() - 4);
    }

    /**
     * Returns PCI-DSS compliant masked representation: e.g. 510000******1234
     */
    public String getMasked() {
        if (pan.length() <= 10) {
            return "******" + getLastFour();
        }
        int maskLength = pan.length() - 10;
        return pan.substring(0, 6) + "*".repeat(maskLength) + getLastFour();
    }

    /**
     * Validates Luhn formula.
     */
    public static boolean validateLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private static CardBrand resolveBrand(String pan) {
        if (pan.startsWith("4")) {
            return CardBrand.VISA;
        }
        // Mastercard: 51-55 or 2221-2720
        if (pan.length() >= 2) {
            int prefix2 = Integer.parseInt(pan.substring(0, 2));
            if (prefix2 >= 51 && prefix2 <= 55) {
                return CardBrand.MASTERCARD;
            }
        }
        if (pan.length() >= 4) {
            int prefix4 = Integer.parseInt(pan.substring(0, 4));
            if (prefix4 >= 2221 && prefix4 <= 2720) {
                return CardBrand.MASTERCARD;
            }
        }
        if (pan.startsWith("34") || pan.startsWith("37")) {
            return CardBrand.AMEX;
        }
        if (pan.startsWith("6011") || pan.startsWith("65")) {
            return CardBrand.DISCOVER;
        }
        return CardBrand.UNKNOWN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardPan cardPan = (CardPan) o;
        return Objects.equals(pan, cardPan.pan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pan);
    }

    @Override
    public String toString() {
        return getMasked();
    }
}
