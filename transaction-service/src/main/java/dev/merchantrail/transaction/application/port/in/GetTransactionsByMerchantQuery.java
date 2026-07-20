package dev.merchantrail.transaction.application.port.in;

import dev.merchantrail.shared.MerchantId;

/**
 * Query to retrieve all transactions for a merchant.
 */
public record GetTransactionsByMerchantQuery(MerchantId merchantId) {
    public GetTransactionsByMerchantQuery {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
    }
}
