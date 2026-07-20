package dev.merchantrail.transaction.infrastructure.outbox;

import dev.merchantrail.transaction.adapter.out.persistence.OutboxEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventJpaEntity, String> {
    
    List<OutboxEventJpaEntity> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
