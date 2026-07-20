package dev.merchantrail.transaction.infrastructure.config;

import dev.merchantrail.transaction.application.port.out.EventPublisher;
import dev.merchantrail.transaction.application.port.out.IdempotencyService;
import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import dev.merchantrail.transaction.application.usecase.GetTransactionUseCase;
import dev.merchantrail.transaction.application.usecase.GetTransactionsByMerchantUseCase;
import dev.merchantrail.transaction.application.usecase.SubmitTransactionUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for wiring use cases with their dependencies.
 * This is where hexagonal architecture ports and adapters are connected.
 */
@Configuration
public class UseCaseConfiguration {
    
    @Bean
    public SubmitTransactionUseCase submitTransactionUseCase(
            TransactionRepository transactionRepository,
            IdempotencyService idempotencyService,
            EventPublisher eventPublisher) {
        return new SubmitTransactionUseCase(transactionRepository, idempotencyService, eventPublisher);
    }
    
    @Bean
    public GetTransactionUseCase getTransactionUseCase(TransactionRepository transactionRepository) {
        return new GetTransactionUseCase(transactionRepository);
    }
    
    @Bean
    public GetTransactionsByMerchantUseCase getTransactionsByMerchantUseCase(
            TransactionRepository transactionRepository) {
        return new GetTransactionsByMerchantUseCase(transactionRepository);
    }
}
