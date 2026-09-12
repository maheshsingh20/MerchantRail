package dev.merchantrail.shared;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CardPan Value Object Tests")
class CardPanTest {

    // Standard valid Luhn Mastercard
    private static final String VALID_MC = "5100000000000008";
    // Standard valid Luhn Visa
    private static final String VALID_VISA = "4000000000000002";

    @Test
    @DisplayName("Should extract BIN, brand, and mask correctly for Mastercard")
    void shouldExtractMastercardDetails() {
        CardPan cardPan = CardPan.of(VALID_MC);

        assertThat(cardPan.getBin()).isEqualTo("510000");
        assertThat(cardPan.getBrand()).isEqualTo(CardBrand.MASTERCARD);
        assertThat(cardPan.getLastFour()).isEqualTo("0008");
        assertThat(cardPan.getMasked()).isEqualTo("510000******0008");
        assertThat(cardPan.toString()).isEqualTo("510000******0008");
    }

    @Test
    @DisplayName("Should extract BIN and brand for Visa")
    void shouldExtractVisaDetails() {
        CardPan cardPan = CardPan.of(VALID_VISA);

        assertThat(cardPan.getBin()).isEqualTo("400000");
        assertThat(cardPan.getBrand()).isEqualTo(CardBrand.VISA);
        assertThat(cardPan.getLastFour()).isEqualTo("0002");
    }

    @Test
    @DisplayName("Should strip whitespace and hyphens")
    void shouldStripFormatting() {
        CardPan cardPan = CardPan.of("5100-0000-0000-0008");
        assertThat(cardPan.getBin()).isEqualTo("510000");
        assertThat(cardPan.getLastFour()).isEqualTo("0008");
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "abcd-efgh", "5100000000000000"}) // Last fails Luhn
    @DisplayName("Should reject invalid or checksum-failed PANs")
    void shouldRejectInvalidPans(String invalidPan) {
        assertThatThrownBy(() -> CardPan.of(invalidPan))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should support ofUnchecked for mock testing")
    void shouldSupportUnchecked() {
        CardPan cardPan = CardPan.ofUnchecked("5200001234567890");
        assertThat(cardPan.getBin()).isEqualTo("520000");
        assertThat(cardPan.getBrand()).isEqualTo(CardBrand.MASTERCARD);
    }
}
