package dev.merchantrail.transaction.adapter.out.persistence;

import dev.merchantrail.shared.MerchantId;
import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.application.port.out.TransactionRepository;
import dev.merchantrail.transaction.domain.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter implementing TransactionRepository port using Spring Data JPA.
 */
@Component
public class TransactionRepositoryImpl implements TransactionRepository {
    
    private final SpringDataTransactionRepository jpaRepository;
    
    public TransactionRepositoryImpl(SpringDataTransactionRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Transaction save(Transaction transaction) {
        TransactionJpaEntity entity = TransactionJpaEntity.fromDomain(transaction);
        TransactionJpaEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }
    
    @Override
    public Optional<Transaction> findById(TransactionId transactionId) {
        return jpaRepository.findById(transactionId.getValue())
            .map(TransactionJpaEntity::toDomain);
    }
    
    @Override
    public List<Transaction> findByMerchantId(MerchantId merchantId) {
        return jpaRepository.findByMerchantId(merchantId.getValue())
            .stream()
            .map(TransactionJpaEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<Transaction> findAll() {
        return jpaRepository.findAll()
            .stream()
            .map(TransactionJpaEntity::toDomain)
            .collect(Collectors.toList());
    }
}
