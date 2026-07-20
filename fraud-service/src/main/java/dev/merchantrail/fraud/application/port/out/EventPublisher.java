package dev.merchantrail.fraud.application.port.out;

import dev.merchantrail.fraud.domain.FraudCheckResult;

public interface EventPublisher {
    void publishFraudCheckPassed(FraudCheckResult result);
    void publishFraudCheckFailed(FraudCheckResult result);
    void publishFraudCheckManualReview(FraudCheckResult result);
}
