package dev.merchantrail.ledger.application.port.in;

import java.math.BigDecimal;

/**
 * Command to record a transaction in the ledger
 */
public record RecordTransactionCommand(
        String transactionId,
        String merchantId,
        BigDecimal amount,
        String currency,
        String type  // DEBIT or CREDIT
) {
}
