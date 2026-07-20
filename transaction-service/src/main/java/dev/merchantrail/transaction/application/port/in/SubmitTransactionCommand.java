package dev.merchantrail.transaction.application.port.in;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.transaction.domain.IdempotencyKey;

/**
 * Command to submit a new transaction.
 */
public record SubmitTransactionCommand(
    MerchantId merchantId,
    Money amount,
    IdempotencyKey idempotencyKey
) {
    public SubmitTransactionCommand {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency key cannot be null");
        }
    }
}
