package dev.merchantrail.transaction.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository interface.
 */
@Repository
interface SpringDataTransactionRepository extends JpaRepository<TransactionJpaEntity, String> {
    
    List<TransactionJpaEntity> findByMerchantId(String merchantId);
    
    Optional<TransactionJpaEntity> findByIdempotencyKey(String idempotencyKey);
}
