package dev.merchantrail.fraud.application.port.out;

import dev.merchantrail.shared.MerchantId;

import java.time.Instant;

public interface TransactionHistoryService {
    int countRecentTransactions(MerchantId merchantId, Instant since);
}
