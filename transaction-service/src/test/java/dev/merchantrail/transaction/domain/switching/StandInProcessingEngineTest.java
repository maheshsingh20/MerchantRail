package dev.merchantrail.transaction.domain.switching;

import dev.merchantrail.shared.CardPan;
import dev.merchantrail.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StandInProcessingEngine (STIP) Tests")
class StandInProcessingEngineTest {

    private StandInProcessingEngine stipEngine;

    @BeforeEach
    void setUp() {
        stipEngine = new StandInProcessingEngine();
    }

    @Test
    @DisplayName("Should approve transaction under offline threshold with STIP auth code")
    void shouldApproveEligibleStipTransaction() {
        CardPan cardPan = CardPan.ofUnchecked("5100001234567890");
        Money amount = Money.of(new BigDecimal("150.00"), "USD");

        StandInProcessingEngine.StipDecision decision = stipEngine.evaluate(cardPan, amount, 15);

        assertThat(decision.approved()).isTrue();
        assertThat(decision.responseCode()).isEqualTo("00");
        assertThat(decision.authCode()).startsWith("ST");
        assertThat(decision.reason()).contains("Approved by Switch Stand-In Processing");
    }

    @Test
    @DisplayName("Should decline transaction exceeding STIP max offline limit ($500.00)")
    void shouldDeclineOverLimitStipTransaction() {
        CardPan cardPan = CardPan.ofUnchecked("5100001234567890");
        Money amount = Money.of(new BigDecimal("750.00"), "USD");

        StandInProcessingEngine.StipDecision decision = stipEngine.evaluate(cardPan, amount, 10);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.responseCode()).isEqualTo("61"); // Exceeds withdrawal limit
        assertThat(decision.reason()).contains("exceeds max offline limit");
    }

    @Test
    @DisplayName("Should decline transaction with high fraud risk score in STIP mode")
    void shouldDeclineHighRiskStipTransaction() {
        CardPan cardPan = CardPan.ofUnchecked("5100001234567890");
        Money amount = Money.of(new BigDecimal("80.00"), "USD");

        StandInProcessingEngine.StipDecision decision = stipEngine.evaluate(cardPan, amount, 85);

        assertThat(decision.approved()).isFalse();
        assertThat(decision.responseCode()).isEqualTo("05"); // Do not honor
        assertThat(decision.reason()).contains("Risk score exceeds offline threshold");
    }

    @Test
    @DisplayName("Should enforce offline velocity limits per card in STIP mode")
    void shouldEnforceVelocityLimits() {
        CardPan cardPan = CardPan.ofUnchecked("5100001234567890");
        Money amount = Money.of(new BigDecimal("25.00"), "USD");

        // Submit 5 approved STIP transactions
        for (int i = 0; i < 5; i++) {
            StandInProcessingEngine.StipDecision decision = stipEngine.evaluate(cardPan, amount, 10);
            assertThat(decision.approved()).isTrue();
        }

        // 6th transaction must be declined due to velocity limit
        StandInProcessingEngine.StipDecision declined = stipEngine.evaluate(cardPan, amount, 10);
        assertThat(declined.approved()).isFalse();
        assertThat(declined.responseCode()).isEqualTo("65");
        assertThat(declined.reason()).contains("velocity limit exceeded");
    }
}
