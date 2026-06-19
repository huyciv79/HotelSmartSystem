package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.dto.IdempotentRecord;
import com.example.hotelsmartbookingbackend.service.IdempotencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String KEY_PREFIX = "idempotency:";
    private static final Duration TTL = Duration.ofDays(1); // Keep records for 24 hours

    @Override
    public boolean startOperation(String key) {
        String redisKey = KEY_PREFIX + key;
        IdempotentRecord record = new IdempotentRecord("PROCESSING", 0, null);
        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, record, TTL);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void completeOperation(String key, int responseStatus, Object responseBody) {
        String redisKey = KEY_PREFIX + key;
        IdempotentRecord record = new IdempotentRecord("SUCCESS", responseStatus, responseBody);
        redisTemplate.opsForValue().set(redisKey, record, TTL);
    }

    @Override
    public Optional<IdempotentRecord> getRecord(String key) {
        String redisKey = KEY_PREFIX + key;
        Object record = redisTemplate.opsForValue().get(redisKey);
        if (record instanceof IdempotentRecord) {
            return Optional.of((IdempotentRecord) record);
        } else if (record != null) {
            try {
                if (record instanceof java.util.Map) {
                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) record;
                    String status = (String) map.get("status");
                    Number responseStatusNum = (Number) map.get("responseStatus");
                    int responseStatus = responseStatusNum != null ? responseStatusNum.intValue() : 0;
                    Object responseBody = map.get("responseBody");
                    return Optional.of(new IdempotentRecord(status, responseStatus, responseBody));
                }
            } catch (Exception e) {
                // Fallback failed
            }
        }
        return Optional.empty();
    }

    @Override
    public void removeKey(String key) {
        String redisKey = KEY_PREFIX + key;
        redisTemplate.delete(redisKey);
    }
}
