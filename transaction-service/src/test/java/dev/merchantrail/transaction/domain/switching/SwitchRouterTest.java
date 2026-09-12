package dev.merchantrail.transaction.domain.switching;

import dev.merchantrail.shared.CardBrand;
import dev.merchantrail.shared.CardPan;
import dev.merchantrail.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SwitchRouter BIN Routing & Interchange Fee Tests")
class SwitchRouterTest {

    private SwitchRouter switchRouter;

    @BeforeEach
    void setUp() {
        switchRouter = new SwitchRouter();
    }

    @Test
    @DisplayName("Should route Mastercard BIN 51xxxx to Citibank with interchange fee calculation")
    void shouldRouteMastercardCiti() {
        // Valid Luhn Mastercard with BIN 510000
        CardPan cardPan = CardPan.of("5100000000000008");
        Money amount = Money.of(new BigDecimal("100.00"), "USD");

        SwitchRouter.RouteDecision decision = switchRouter.route(cardPan, amount);

        assertThat(decision.issuerId()).isEqualTo("ISSUER_CITI");
        assertThat(decision.issuerName()).isEqualTo("Citibank NA");
        assertThat(decision.cardBrand()).isEqualTo(CardBrand.MASTERCARD);
        assertThat(decision.bin()).isEqualTo("510000");
        assertThat(decision.supportsStip()).isTrue();

        // 100.00 * 0.0145 + 0.10 = 1.45 + 0.10 = 1.55
        assertThat(decision.interchangeFee().getAmount()).isEqualByComparingTo("1.5500");
        assertThat(decision.switchFee().getAmount()).isEqualByComparingTo("0.045");
    }

    @Test
    @DisplayName("Should route Mastercard BIN 52xxxx to Chase with correct fees")
    void shouldRouteMastercardChase() {
        CardPan cardPan = CardPan.ofUnchecked("5200001234567890");
        Money amount = Money.of(new BigDecimal("200.00"), "USD");

        SwitchRouter.RouteDecision decision = switchRouter.route(cardPan, amount);

        assertThat(decision.issuerId()).isEqualTo("ISSUER_CHASE");
        assertThat(decision.issuerName()).isEqualTo("JPMorgan Chase");
        assertThat(decision.cardBrand()).isEqualTo(CardBrand.MASTERCARD);
        // 200.00 * 0.0150 + 0.10 = 3.00 + 0.10 = 3.10
        assertThat(decision.interchangeFee().getAmount()).isEqualByComparingTo("3.1000");
    }

    @Test
    @DisplayName("Should route Visa BIN 4xxxxx to Visa Network")
    void shouldRouteVisaNetwork() {
        CardPan cardPan = CardPan.of("4000000000000002");
        Money amount = Money.of(new BigDecimal("50.00"), "USD");

        SwitchRouter.RouteDecision decision = switchRouter.route(cardPan, amount);

        assertThat(decision.issuerId()).isEqualTo("ISSUER_VISA_NET");
        assertThat(decision.cardBrand()).isEqualTo(CardBrand.VISA);
        assertThat(decision.bin()).isEqualTo("400000");
    }
}
