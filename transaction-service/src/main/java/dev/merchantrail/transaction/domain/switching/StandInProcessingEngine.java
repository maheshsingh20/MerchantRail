package dev.merchantrail.transaction.domain.switching;

import dev.merchantrail.shared.CardPan;
import dev.merchantrail.shared.Money;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stand-In Processing (STIP) Engine.
 * Enables the payment switch to make autonomous authorization decisions on behalf of issuers
 * when issuer links are down, unreachable, or exceed latency SLAs.
 */
public class StandInProcessingEngine {

    // Maximum transaction amount allowed under STIP parameters ($500.00)
    private static final BigDecimal MAX_STIP_AMOUNT = new BigDecimal("500.00");
    // Maximum offline velocity count per card PAN in memory
    private static final int MAX_VELOCITY_COUNT = 5;

    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, Integer> cardVelocityTracker = new ConcurrentHashMap<>();

    public record StipDecision(
        boolean approved,
        String responseCode,
        String authCode,
        String reason
    ) {}

    /**
     * Evaluates whether a transaction can be approved under STIP parameters.
     *
     * @param cardPan card details
     * @param amount transaction amount
     * @param fraudScore computed fraud risk score (0-100)
     * @return StipDecision
     */
    public StipDecision evaluate(CardPan cardPan, Money amount, int fraudScore) {
        Objects.requireNonNull(cardPan, "Card PAN cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");

        // Rule 1: High risk transactions rejected in STIP mode
        if (fraudScore > 65) {
            return new StipDecision(
                false,
                "05",
                "",
                "Declined by STIP: Risk score exceeds offline threshold (" + fraudScore + ")"
            );
        }

        // Rule 2: Amount threshold for offline stand-in processing
        if (amount.getAmount().compareTo(MAX_STIP_AMOUNT) > 0) {
            return new StipDecision(
                false,
                "61",
                "",
                "Declined by STIP: Amount " + amount + " exceeds max offline limit of " + MAX_STIP_AMOUNT
            );
        }

        // Rule 3: Card velocity check in STIP mode
        int currentVelocity = cardVelocityTracker.merge(cardPan.getBin() + cardPan.getLastFour(), 1, Integer::sum);
        if (currentVelocity > MAX_VELOCITY_COUNT) {
            return new StipDecision(
                false,
                "65",
                "",
                "Declined by STIP: Card offline velocity limit exceeded (" + currentVelocity + ")"
            );
        }

        // Passed all STIP rules -> Generate 6-char STIP Authorization Code
        int randomSuffix = 1000 + RANDOM.nextInt(9000);
        String authCode = "ST" + randomSuffix;

        return new StipDecision(
            true,
            "00",
            authCode,
            "Approved by Switch Stand-In Processing (STIP)"
        );
    }

    /**
     * Resets velocity tracker for testing purposes.
     */
    public void resetVelocity() {
        cardVelocityTracker.clear();
    }
}
