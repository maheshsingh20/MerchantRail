package dev.merchantrail.fraud.application.port.out;

import dev.merchantrail.shared.MerchantId;

public interface MerchantHistoryService {
    boolean isNewMerchant(MerchantId merchantId);
}
