package dev.merchantrail.transaction.adapter.out.redis;

import dev.merchantrail.shared.TransactionId;
import dev.merchantrail.transaction.application.port.out.IdempotencyService;
import dev.merchantrail.transaction.domain.IdempotencyKey;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-based implementation of idempotency service.
 * Keys expire after 24 hours.
 */
@Component
public class RedisIdempotencyService implements IdempotencyService {
    
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofHours(24);
    
    private final RedisTemplate<String, String> redisTemplate;
    
    public RedisIdempotencyService(@org.springframework.beans.factory.annotation.Qualifier("redisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    @Override
    public Optional<TransactionId> getTransactionId(IdempotencyKey key) {
        String redisKey = KEY_PREFIX + key.getValue();
        String value = redisTemplate.opsForValue().get(redisKey);
        
        if (value == null) {
            return Optional.empty();
        }
        
        return Optional.of(TransactionId.of(value));
    }
    
    @Override
    public void store(IdempotencyKey key, TransactionId transactionId) {
        String redisKey = KEY_PREFIX + key.getValue();
        redisTemplate.opsForValue().set(redisKey, transactionId.getValue(), TTL);
    }
}
