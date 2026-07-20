package dev.merchantrail.transaction.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for outbox pattern - ensures reliable event publishing.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_published", columnList = "published"),
    @Index(name = "idx_outbox_created", columnList = "created_at")
})
public class OutboxEventJpaEntity {
    
    @Id
    @Column(name = "id")
    private String id;
    
    @Column(name = "aggregate_id", length = 50, nullable = false)
    private String aggregateId;
    
    @Column(name = "aggregate_type", length = 50, nullable = false)
    private String aggregateType;
    
    @Column(name = "event_type", length = 100, nullable = false)
    private String eventType;
    
    @Column(name = "payload", columnDefinition = "TEXT", nullable = false)
    private String payload;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "published", nullable = false)
    private boolean published = false;
    
    @Column(name = "published_at")
    private Instant publishedAt;
    
    protected OutboxEventJpaEntity() {}
    
    public OutboxEventJpaEntity(String aggregateId, String aggregateType, 
                               String eventType, String payload) {
        this.id = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }
    
    public void markAsPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public String getAggregateId() {
        return aggregateId;
    }
    
    public String getAggregateType() {
        return aggregateType;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public boolean isPublished() {
        return published;
    }
    
    public Instant getPublishedAt() {
        return publishedAt;
    }
}
