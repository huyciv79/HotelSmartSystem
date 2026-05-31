package com.example.hotelsmartbookingbackend.service.impl;

import com.example.hotelsmartbookingbackend.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterServiceImpl implements RateLimiterService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String LOGIN_ATTEMPT_PREFIX = "login:attempts:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    @Override
    public boolean isRateLimited(String key) {
        String redisKey = LOGIN_ATTEMPT_PREFIX + key;
        Object value = redisTemplate.opsForValue().get(redisKey);

        if (value == null) {
            return false;
        }

        int attempts = Integer.parseInt(value.toString());
        return attempts >= MAX_ATTEMPTS;
    }

    @Override
    public void incrementAttempt(String key) {
        String redisKey = LOGIN_ATTEMPT_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);

        // Đặt TTL chỉ khi counter vừa được tạo mới (lần đầu tiên)
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, WINDOW_DURATION);
        }
    }

    @Override
    public void resetAttempt(String key) {
        redisTemplate.delete(LOGIN_ATTEMPT_PREFIX + key);
    }
}
