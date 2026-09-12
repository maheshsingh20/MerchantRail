package dev.merchantrail.transaction.domain.switching;

import dev.merchantrail.shared.CardBrand;
import dev.merchantrail.shared.CardPan;
import dev.merchantrail.shared.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Enterprise Switching Router responsible for BIN range lookups,
 * issuer endpoint resolution, and interchange/network fee calculation.
 * Models high-throughput network interface processor switching logic.
 */
public class SwitchRouter {

    public record RouteDecision(
        String issuerId,
        String issuerName,
        CardBrand cardBrand,
        String bin,
        Money interchangeFee,
        Money switchFee,
        boolean supportsStip
    ) {}

    // Simulated BIN routing table (Acquirer -> Switch -> Issuer Bank)
    private static final Map<String, IssuerRouteRule> BIN_ROUTING_TABLE = new LinkedHashMap<>();

    private record IssuerRouteRule(
        String prefix,
        String issuerId,
        String issuerName,
        CardBrand brand,
        BigDecimal interchangeRate, // e.g. 0.015 (1.5%)
        BigDecimal fixedInterchange, // e.g. $0.10
        BigDecimal networkSwitchFee, // e.g. $0.05
        boolean allowsStip
    ) {}

    static {
        // Mastercard BIN ranges: 510000 - 559999
        BIN_ROUTING_TABLE.put("51", new IssuerRouteRule(
            "51", "ISSUER_CITI", "Citibank NA", CardBrand.MASTERCARD,
            new BigDecimal("0.0145"), new BigDecimal("0.10"), new BigDecimal("0.045"), true
        ));
        BIN_ROUTING_TABLE.put("52", new IssuerRouteRule(
            "52", "ISSUER_CHASE", "JPMorgan Chase", CardBrand.MASTERCARD,
            new BigDecimal("0.0150"), new BigDecimal("0.10"), new BigDecimal("0.050"), true
        ));
        BIN_ROUTING_TABLE.put("53", new IssuerRouteRule(
            "53", "ISSUER_BARCLAYS", "Barclays Bank", CardBrand.MASTERCARD,
            new BigDecimal("0.0140"), new BigDecimal("0.08"), new BigDecimal("0.040"), true
        ));
        BIN_ROUTING_TABLE.put("54", new IssuerRouteRule(
            "54", "ISSUER_HSBC", "HSBC Global Payments", CardBrand.MASTERCARD,
            new BigDecimal("0.0155"), new BigDecimal("0.12"), new BigDecimal("0.050"), true
        ));
        BIN_ROUTING_TABLE.put("55", new IssuerRouteRule(
            "55", "ISSUER_HDFC", "HDFC Bank", CardBrand.MASTERCARD,
            new BigDecimal("0.0135"), new BigDecimal("0.05"), new BigDecimal("0.035"), true
        ));

        // Visa BIN range: 4x
        BIN_ROUTING_TABLE.put("4", new IssuerRouteRule(
            "4", "ISSUER_VISA_NET", "Visa Dual Message Network", CardBrand.VISA,
            new BigDecimal("0.0160"), new BigDecimal("0.10"), new BigDecimal("0.055"), true
        ));
    }

    /**
     * Resolves the switching decision for a transaction based on card PAN.
     */
    public RouteDecision route(CardPan cardPan, Money amount) {
        Objects.requireNonNull(cardPan, "Card PAN cannot be null");
        Objects.requireNonNull(amount, "Amount cannot be null");

        String bin = cardPan.getBin();
        IssuerRouteRule matchedRule = null;

        // Longest prefix match
        for (Map.Entry<String, IssuerRouteRule> entry : BIN_ROUTING_TABLE.entrySet()) {
            if (bin.startsWith(entry.getKey())) {
                matchedRule = entry.getValue();
                break;
            }
        }

        if (matchedRule == null) {
            // Default fallback route
            matchedRule = new IssuerRouteRule(
                bin.substring(0, 2), "ISSUER_DEFAULT", "Default Network Route",
                cardPan.getBrand(), new BigDecimal("0.0150"), new BigDecimal("0.10"),
                new BigDecimal("0.05"), true
            );
        }

        // Calculate interchange fee: (amount * rate) + fixed
        BigDecimal variableInterchange = amount.getAmount()
            .multiply(matchedRule.interchangeRate)
            .setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalInterchange = variableInterchange.add(matchedRule.fixedInterchange);
        Money interchangeFee = Money.of(totalInterchange, amount.getCurrencyCode());

        // Calculate switch processing fee
        Money switchFee = Money.of(matchedRule.networkSwitchFee, amount.getCurrencyCode());

        return new RouteDecision(
            matchedRule.issuerId,
            matchedRule.issuerName,
            matchedRule.brand,
            bin,
            interchangeFee,
            switchFee,
            matchedRule.allowsStip
        );
    }
}
