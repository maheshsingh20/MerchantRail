package dev.merchantrail.fraud.application.port.in;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.Money;
import dev.merchantrail.shared.TransactionId;

public record CheckFraudCommand(
    TransactionId transactionId,
    MerchantId merchantId,
    Money amount
) {}
