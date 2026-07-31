package com.example.hotelsmartbookingbackend.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RateLimiterServiceImpl rateLimiterService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==========================================
    // 1. isRateLimited Test Cases (UTCID01 - UTCID05)
    // ==========================================

    @Test
    @DisplayName("UTCID01 - Return false when key does not exist in Redis (value == null)")
    void should_returnFalse_when_isRateLimitedKeyDoesNotExistInRedis() {
        when(valueOperations.get("login:attempts:192.168.1.1")).thenReturn(null);

        boolean result = rateLimiterService.isRateLimited("192.168.1.1");

        assertFalse(result);
    }

    @Test
    @DisplayName("UTCID02 - Return false when attempt count in Redis is less than 5 (e.g. 3)")
    void should_returnFalse_when_isRateLimitedAttemptsCountIsLessThan5() {
        when(valueOperations.get("login:attempts:192.168.1.1")).thenReturn("3");

        boolean result = rateLimiterService.isRateLimited("192.168.1.1");

        assertFalse(result);
    }

    @Test
    @DisplayName("UTCID03 - Return true when attempt count in Redis is exactly 5")
    void should_returnTrue_when_isRateLimitedAttemptsCountIsExactly5() {
        when(valueOperations.get("login:attempts:192.168.1.1")).thenReturn("5");

        boolean result = rateLimiterService.isRateLimited("192.168.1.1");

        assertTrue(result);
    }

    @Test
    @DisplayName("UTCID04 - Return true when attempt count in Redis is greater than 5 (e.g. 8)")
    void should_returnTrue_when_isRateLimitedAttemptsCountIsGreaterThan5() {
        when(valueOperations.get("login:attempts:192.168.1.1")).thenReturn("8");

        boolean result = rateLimiterService.isRateLimited("192.168.1.1");

        assertTrue(result);
    }

    @Test
    @DisplayName("UTCID05 - Throw NumberFormatException when attempt count in Redis is non-numeric")
    void should_throwNumberFormatException_when_isRateLimitedValueInRedisIsNonNumeric() {
        when(valueOperations.get("login:attempts:192.168.1.1")).thenReturn("invalid_number");

        assertThrows(NumberFormatException.class,
                () -> rateLimiterService.isRateLimited("192.168.1.1"));
    }

    // ==========================================
    // 2. incrementAttempt Test Cases (UTCID06 - UTCID07, UTCID09)
    // ==========================================

    @Test
    @DisplayName("UTCID06 - Increment counter and set 1-minute TTL when count == 1 (first attempt)")
    void should_incrementAttemptAndSetExpire_when_countIsOne() {
        when(valueOperations.increment("login:attempts:192.168.1.1")).thenReturn(1L);

        rateLimiterService.incrementAttempt("192.168.1.1");

        verify(valueOperations).increment("login:attempts:192.168.1.1");
        verify(redisTemplate).expire("login:attempts:192.168.1.1", Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("UTCID07 - Increment counter and skip expire when count > 1 (subsequent attempts)")
    void should_incrementAttemptAndSkipExpire_when_countIsGreaterThanOne() {
        when(valueOperations.increment("login:attempts:192.168.1.1")).thenReturn(2L);

        rateLimiterService.incrementAttempt("192.168.1.1");

        verify(valueOperations).increment("login:attempts:192.168.1.1");
        verify(redisTemplate, never()).expire(any(), any());
    }

    @Test
    @DisplayName("UTCID09 - Increment counter and skip expire when increment returns null")
    void should_incrementAttemptAndSkipExpire_when_countIsNull() {
        when(valueOperations.increment("login:attempts:192.168.1.1")).thenReturn(null);

        rateLimiterService.incrementAttempt("192.168.1.1");

        verify(valueOperations).increment("login:attempts:192.168.1.1");
        verify(redisTemplate, never()).expire(any(), any());
    }

    // ==========================================
    // 3. resetAttempt Test Cases (UTCID08)
    // ==========================================

    @Test
    @DisplayName("UTCID08 - Successful resetAttempt deleting counter key from Redis")
    void should_resetAttemptSuccessfully_when_deletingRedisKey() {
        rateLimiterService.resetAttempt("192.168.1.1");

        verify(redisTemplate).delete("login:attempts:192.168.1.1");
    }
}
