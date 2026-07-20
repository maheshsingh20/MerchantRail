package dev.merchantrail.fraud.application.port.out;

import dev.merchantrail.fraud.domain.FraudCheckResult;

public interface FraudCheckRepository {
    void save(FraudCheckResult result);
}
